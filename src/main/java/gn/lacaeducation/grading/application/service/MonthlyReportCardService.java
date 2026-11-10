package gn.lacaeducation.grading.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.grading.TeacherNameProvider;
import gn.lacaeducation.grading.application.port.out.AssessmentRepository;
import gn.lacaeducation.grading.application.port.out.ClassSubjectQueryPort;
import gn.lacaeducation.grading.application.port.out.ClassSubjectQueryPort.ClassSubjectInfo;
import gn.lacaeducation.grading.application.port.out.GradeRepository;
import gn.lacaeducation.grading.application.port.out.MonthlyReportCardPdfGenerator;
import gn.lacaeducation.grading.application.port.out.MonthlyReportCardPdfGenerator.Line;
import gn.lacaeducation.grading.application.port.out.MonthlyReportCardPdfGenerator.MonthlyReportCardDocument;
import gn.lacaeducation.grading.application.port.out.MonthlyReportCardRepository;
import gn.lacaeducation.grading.domain.*;
import gn.lacaeducation.students.StudentDataProvider;
import gn.lacaeducation.students.StudentPublicInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Bulletin de contrôle mensuel (§censeur) : diagnostic de niveau en cours d'année, saisi dans la
 * période semestrielle/trimestrielle en cours (pas une période séparée — voir
 * {@link AssessmentType#MONTHLY}) mais généré et stocké dans un sous-domaine totalement indépendant
 * de {@link ReportCard}/{@code GradeCalculationService} : jamais la même table, jamais agrégé dans
 * le bulletin annuel. Réutilise le même moteur de calcul par matière
 * ({@link SubjectSemesterAverage#computeMonthly}) et la même formule de moyenne générale
 * (Σ moyennes pondérées / Σ coefficients) que le bulletin officiel.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class MonthlyReportCardService {

    private static final int SCALE = 2;

    private final AssessmentRepository assessmentRepository;
    private final GradeRepository gradeRepository;
    private final MonthlyReportCardRepository monthlyReportCardRepository;
    private final ClassSubjectQueryPort classSubjectQueryPort;
    private final AcademicsDataProvider academicsDataProvider;
    private final TeacherNameProvider teacherNameProvider;
    private final StudentDataProvider studentDataProvider;
    private final MonthlyReportCardPdfGenerator pdfGenerator;

    private static final String[] MONTHS_FR = {"Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
            "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"};

    /** Génère (ou régénère) le bulletin mensuel de tous les élèves ayant au moins une note MONTHLY
     * sur ce mois pour cette classe — même convention que {@code GradeCalculationService} (les
     * élèves concernés sont déduits des notes existantes, pas d'un appel séparé à la liste des
     * inscrits). */
    public List<MonthlyReportCard> generateForClass(Long classId, YearMonth month) {
        List<Assessment> monthly = monthlyAssessments(classId, month);
        List<Long> studentIds = monthly.stream()
                .flatMap(a -> gradeRepository.findByAssessmentId(a.getId()).stream())
                .map(Grade::getStudentId)
                .distinct()
                .toList();
        List<MonthlyReportCard> cards = new ArrayList<>();
        for (Long studentId : studentIds) {
            cards.add(generateForStudent(studentId, classId, month, monthly));
        }
        updateRanks(cards);
        return cards;
    }

    public MonthlyReportCard generateForStudent(Long studentId, Long classId, YearMonth month) {
        MonthlyReportCard saved = generateForStudent(studentId, classId, month, monthlyAssessments(classId, month));
        // Même convention que GradeCalculationService.generateForStudent : un bulletin individuel
        // recalcule le rang pour toute la classe, pour rester cohérent avec les autres élèves.
        updateRanks(findByClass(classId, month));
        return monthlyReportCardRepository.findById(saved.getId()).orElse(saved);
    }

    private MonthlyReportCard generateForStudent(Long studentId, Long classId, YearMonth month, List<Assessment> monthly) {
        List<ClassSubjectInfo> subjectInfos = classSubjectQueryPort.findByClassId(classId);
        Map<Long, BigDecimal> subjectCoeffMap = subjectInfos.stream()
                .collect(Collectors.toMap(ClassSubjectInfo::subjectId, ClassSubjectInfo::coefficient, (a, b) -> a));
        Map<Long, Long> subjectTeacherMap = subjectInfos.stream()
                .filter(i -> i.teacherId() != null)
                .collect(Collectors.toMap(ClassSubjectInfo::subjectId, ClassSubjectInfo::teacherId, (a, b) -> a));

        CycleGroup cycleGroup = resolveCycleGroup(classId);
        Map<Long, List<Assessment>> bySubject = monthly.stream().collect(Collectors.groupingBy(Assessment::getSubjectId));

        List<MonthlyReportCardLine> lines = new ArrayList<>();
        BigDecimal sumWeighted = BigDecimal.ZERO;
        BigDecimal sumCoeffs = BigDecimal.ZERO;

        for (Map.Entry<Long, List<Assessment>> entry : bySubject.entrySet()) {
            Long subjectId = entry.getKey();
            List<BigDecimal> values = entry.getValue().stream()
                    .flatMap(a -> gradeRepository.findByAssessmentId(a.getId()).stream())
                    .filter(g -> studentId.equals(g.getStudentId()) && g.getValue() != null)
                    .map(Grade::getValue)
                    .toList();
            if (values.isEmpty()) continue;

            BigDecimal coef = cycleGroup == CycleGroup.PRIMAIRE ? BigDecimal.ONE : subjectCoeffMap.getOrDefault(subjectId, BigDecimal.ONE);
            BigDecimal avg = SubjectSemesterAverage.computeMonthly(cycleGroup, coef, values);
            if (avg == null) continue;

            Long teacherId = subjectTeacherMap.get(subjectId);
            String teacherLabel = teacherId != null ? teacherNameProvider.teacherName(teacherId).orElse(null) : null;

            lines.add(MonthlyReportCardLine.builder()
                    .subjectId(subjectId).subjectAverage(avg).coefficient(coef).teacherName(teacherLabel).build());

            sumWeighted = sumWeighted.add(avg);
            sumCoeffs = sumCoeffs.add(coef);
        }

        // Même formule que le bulletin officiel (GradeCalculationService) : la moyenne générale
        // divise par la SOMME DES COEFFICIENTS, jamais par le nombre de matières — les deux ne
        // coïncident qu'en Primaire, où le coefficient est forcé à 1 pour chaque matière.
        BigDecimal overall = sumCoeffs.signum() > 0
                ? sumWeighted.divide(sumCoeffs, SCALE, RoundingMode.HALF_UP)
                : null;

        MonthlyReportCard card = MonthlyReportCard.builder()
                .tenantId(TenantContext.get())
                .studentId(studentId).classId(classId).yearMonth(month)
                .overallAverage(overall).autoAppreciation(autoAppreciation(overall))
                .generatedAt(LocalDateTime.now())
                .build();

        return persistWithLines(card, lines);
    }

    /** Appréciation automatique (§censeur, "pour que son parent puisse savoir si il doit faire des
     * révisions") — réutilise le même barème que le bulletin officiel ({@link GradingScale#honorFor}),
     * jamais un nouveau seuil inventé ici. */
    private static String autoAppreciation(BigDecimal overallAverage) {
        if (overallAverage == null) return null;
        return switch (GradingScale.defaults().honorFor(overallAverage)) {
            case "Très Bien" -> "Excellent travail ce mois-ci — continuez ainsi.";
            case "Bien" -> "Bon travail, résultats satisfaisants.";
            case "Assez Bien" -> "Résultats corrects ; des efforts supplémentaires sont encouragés.";
            case "Passable" -> "Résultats justes ; des révisions sont recommandées.";
            default -> "Résultats insuffisants ; des révisions sont nécessaires.";
        };
    }

    /** Classement du mois, parmi les élèves de la classe ayant un bulletin mensuel ce mois-ci —
     * même règle d'ex æquo que le bulletin officiel, voir {@link RankingUtil}. */
    private void updateRanks(List<MonthlyReportCard> cards) {
        if (cards.isEmpty()) return;
        int classSize = cards.size();
        for (RankingUtil.Ranked<MonthlyReportCard> entry : RankingUtil.rank(cards, MonthlyReportCard::getOverallAverage)) {
            MonthlyReportCard card = entry.item();
            card.setRank(entry.rank());
            card.setClassSize(classSize);
            monthlyReportCardRepository.save(card);
        }
    }

    /** Remplace le bulletin mensuel existant pour ce (élève, classe, mois) s'il y en a un, en
     * réutilisant sa ligne (UPDATE) plutôt que supprimer puis réinsérer — sinon l'ordre de flush par
     * défaut d'Hibernate (INSERT avant DELETE) viole la contrainte unique (student_id, class_id,
     * year_month) : bug réel rencontré en test, la 1ère régénération d'un mois déjà généré échouait
     * systématiquement en 409. L'appréciation manuelle du censeur ({@code comment}) est reportée sur
     * la ligne réutilisée : une régénération (ex. après correction d'une note) ne doit jamais
     * effacer ce qu'il a écrit. */
    private MonthlyReportCard persistWithLines(MonthlyReportCard card, List<MonthlyReportCardLine> lines) {
        monthlyReportCardRepository.findByStudentIdAndClassIdAndYearMonth(card.getStudentId(), card.getClassId(), card.getYearMonth())
                .ifPresent(existing -> {
                    card.setId(existing.getId());
                    card.setComment(existing.getComment());
                    monthlyReportCardRepository.deleteLinesByMonthlyReportCardId(existing.getId());
                });
        MonthlyReportCard saved = monthlyReportCardRepository.save(card);
        for (MonthlyReportCardLine line : lines) {
            line.setMonthlyReportCardId(saved.getId());
            monthlyReportCardRepository.saveLine(line);
        }
        return saved;
    }

    private List<Assessment> monthlyAssessments(Long classId, YearMonth month) {
        return assessmentRepository.findByClassId(classId).stream()
                .filter(a -> a.getType() == AssessmentType.MONTHLY && a.getDate() != null
                        && YearMonth.from(a.getDate()).equals(month))
                .toList();
    }

    private CycleGroup resolveCycleGroup(Long classId) {
        if (classId == null) return CycleGroup.SECONDAIRE;
        return academicsDataProvider.findClass(classId)
                .map(c -> CycleGroup.fromSchoolCycle(c.schoolCycle()))
                .orElse(CycleGroup.SECONDAIRE);
    }

    @Transactional(readOnly = true)
    public List<MonthlyReportCard> findByClass(Long classId, YearMonth month) {
        return monthlyReportCardRepository.findByClassIdAndYearMonth(classId, month);
    }

    @Transactional(readOnly = true)
    public Optional<MonthlyReportCard> findById(Long id) {
        return monthlyReportCardRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<MonthlyReportCardLine> findLines(Long id) {
        return monthlyReportCardRepository.findLines(id);
    }

    /** Appréciation manuelle du censeur — jamais écrasée par une régénération ultérieure
     * ({@link #persistWithLines}). */
    public MonthlyReportCard setComment(Long id, String comment) {
        MonthlyReportCard card = monthlyReportCardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MonthlyReportCard", id));
        card.setComment(comment);
        return monthlyReportCardRepository.save(card);
    }

    @Transactional(readOnly = true)
    public byte[] pdfBytes(Long id) {
        MonthlyReportCard card = monthlyReportCardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MonthlyReportCard", id));
        List<MonthlyReportCardLine> lines = monthlyReportCardRepository.findLines(id);

        StudentPublicInfo student = studentDataProvider.findStudent(card.getStudentId()).orElse(null);
        String studentName = student == null ? "Élève #" + card.getStudentId()
                : ((student.firstName() == null ? "" : student.firstName()) + " "
                + (student.lastName() == null ? "" : student.lastName())).trim();
        String className = academicsDataProvider.findClass(card.getClassId()).map(c -> c.label()).orElse(null);

        Map<Long, String> subjectLabels = academicsDataProvider.subjectLabels(
                lines.stream().map(MonthlyReportCardLine::getSubjectId).collect(Collectors.toSet()));

        List<Line> pdfLines = lines.stream()
                .map(l -> new Line(subjectLabels.getOrDefault(l.getSubjectId(), "Matière #" + l.getSubjectId()),
                        l.getSubjectAverage(), l.getCoefficient(), l.getTeacherName()))
                .toList();

        MonthlyReportCardDocument doc = new MonthlyReportCardDocument(
                card.getTenantId(), studentName, student != null ? student.studentNumber() : null,
                className, monthLabel(card.getYearMonth()), card.getOverallAverage(),
                card.getRank(), card.getClassSize(), pdfLines,
                card.getAutoAppreciation(), card.getComment());

        return pdfGenerator.generate(doc);
    }

    private static String monthLabel(YearMonth ym) {
        if (ym == null) return null;
        return MONTHS_FR[ym.getMonthValue() - 1] + " " + ym.getYear();
    }
}
