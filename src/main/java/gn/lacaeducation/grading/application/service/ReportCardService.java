package gn.lacaeducation.grading.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.grading.application.port.out.ReportCardPdfGenerator.ReportCardContext;
import gn.lacaeducation.students.StudentDataProvider;
import gn.lacaeducation.students.StudentPublicInfo;
import gn.lacaeducation.grading.application.event.ReportCardPublishedEvent;
import gn.lacaeducation.grading.application.port.in.ReportCardUseCase;
import gn.lacaeducation.grading.application.port.out.DocumentStoragePort;
import gn.lacaeducation.grading.application.port.out.ReportCardPdfGenerator;
import gn.lacaeducation.grading.application.port.out.ReportCardRepository;
import gn.lacaeducation.grading.domain.ReportCard;
import gn.lacaeducation.grading.domain.ReportCardLine;
import gn.lacaeducation.grading.domain.ReportCardStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class ReportCardService implements ReportCardUseCase {

    private final ReportCardRepository repo;
    private final GradeCalculationService calculationService;
    private final ApplicationEventPublisher eventPublisher;
    private final ReportCardPdfGenerator pdfGenerator;
    private final DocumentStoragePort documentStorage;
    private final StudentDataProvider studentProvider;
    private final AcademicsDataProvider academicsProvider;
    private final gn.lacaeducation.grading.application.port.out.ClassSubjectQueryPort classSubjectQueryPort;
    private final gn.lacaeducation.grading.application.port.out.HonorRollPdfGenerator honorRollGenerator;
    private final gn.lacaeducation.grading.application.port.out.ClassCentralizerPdfGenerator centralizerGenerator;
    private final gn.lacaeducation.grading.application.port.in.AnnualReportCardUseCase annualReportCardUseCase;

    @Override
    public ReportCard generate(GenerateReportCardCommand c) {
        return calculationService.generateForStudent(c.studentId(), c.classId(), c.periodId());
    }

    @Override
    public List<ReportCard> generateBulk(GenerateBulkCommand c) {
        return calculationService.generateForClass(c.classId(), c.periodId());
    }

    @Override
    public ReportCard publish(Long id) {
        ReportCard rc = load(id);
        rc.setStatus(ReportCardStatus.PUBLISHED);
        ReportCard saved = repo.save(rc);
        eventPublisher.publishEvent(
                new ReportCardPublishedEvent(null, rc.getStudentId(), rc.getPeriodId(), rc.getId()));
        return saved;
    }

    @Override
    public ReportCard setDistinction(Long id, gn.lacaeducation.grading.domain.Distinction distinction) {
        ReportCard rc = load(id);
        rc.setDistinction(distinction != null ? distinction : gn.lacaeducation.grading.domain.Distinction.NONE);
        return repo.save(rc);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReportCard> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportCard> findByClass(Long classId, Long periodId) {
        return repo.findByClassIdAndPeriodId(classId, periodId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReportCard> findByStudent(Long studentId, Long periodId) {
        return repo.findByStudentIdAndPeriodId(studentId, periodId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportCard> findByStudentId(Long studentId) {
        return repo.findByStudentId(studentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportCardLine> findLines(Long id) {
        return repo.findLines(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> getPdf(Long id) {
        return Optional.ofNullable(load(id).getPdfFile());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] pdfBytes(Long id) {
        ReportCard rc = load(id);
        List<ReportCardLine> lines = repo.findLines(id);
        return pdfGenerator.generate(rc, lines, buildContext(rc, lines));
    }

    @Async
    @Override
    public void generatePdfAsync(Long id) {
        // Asynchronous PDF generation: render the report card, store the bytes, then persist the
        // resulting location so subsequent reads can serve it (idempotent — re-rendering overwrites).
        ReportCard rc = load(id);
        List<ReportCardLine> lines = repo.findLines(id);
        byte[] pdf = pdfGenerator.generate(rc, lines, buildContext(rc, lines));
        String location = documentStorage.store("report-card-" + id + ".pdf", pdf);
        rc.setPdfFile(location);
        repo.save(rc);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] honorRollPdf(Long periodId) {
        Long tenantId = gn.lacaeducation.common.TenantContext.get();
        // Toutes les classes de la période (pas une seule) : le palmarès classe les meilleurs élèves
        // DE L'ÉTABLISSEMENT, pas d'une classe isolée (§retour fondateur). Le rang stocké sur chaque
        // bulletin (ReportCard.rank) est un rang PAR CLASSE — inutilisable ici ; on reclasse tout le
        // monde par moyenne générale décroissante pour obtenir une position école entière.
        List<ReportCard> cards = repo.findByPeriodId(periodId).stream()
                .filter(rc -> rc.getOverallAverage() != null)
                .sorted(java.util.Comparator.comparing(ReportCard::getOverallAverage, java.util.Comparator.reverseOrder()))
                .toList();
        // Classement standard ("1, 1, 3, 4, 4…") : deux moyennes strictement égales partagent le même
        // rang, jamais "1, 2" (fidèle à la maquette de référence, "EX ÆQUO"). Deux passes : d'abord les
        // rangs (égalité = même rang que le précédent), puis repérage des rangs partagés par 2+ élèves.
        List<Integer> ranks = new java.util.ArrayList<>();
        BigDecimal prevAverage = null;
        int prevRank = 0;
        for (int i = 0; i < cards.size(); i++) {
            BigDecimal average = cards.get(i).getOverallAverage();
            int rank = (prevAverage != null && prevAverage.compareTo(average) == 0) ? prevRank : i + 1;
            ranks.add(rank);
            prevAverage = average;
            prevRank = rank;
        }
        java.util.Map<Integer, Long> rankCounts = ranks.stream()
                .collect(java.util.stream.Collectors.groupingBy(r -> r, java.util.stream.Collectors.counting()));

        var rows = new java.util.ArrayList<gn.lacaeducation.grading.application.port.out.HonorRollPdfGenerator.Row>();
        for (int i = 0; i < cards.size(); i++) {
            ReportCard rc = cards.get(i);
            int rank = ranks.get(i);
            boolean exAequo = rankCounts.get(rank) > 1;
            StudentPublicInfo s = studentProvider.findStudent(rc.getStudentId()).orElse(null);
            String className = academicsProvider.findClass(rc.getClassId())
                    .map(c -> c.levelLabel() != null && !c.levelLabel().isBlank() ? c.label() + " (" + c.levelLabel() + ")" : c.label())
                    .orElse(null);
            rows.add(new gn.lacaeducation.grading.application.port.out.HonorRollPdfGenerator.Row(
                    rank, exAequo, studentName(rc.getStudentId()),
                    s != null ? s.firstName() : null, s != null ? s.lastName() : null,
                    rc.getOverallAverage(), rc.getHonor(), s != null ? s.photo() : null, className));
        }
        String periodLabel = academicsProvider.findPeriod(periodId).map(p -> p.label()).orElse(null);
        String schoolYear = tenantId == null ? null : academicsProvider.currentAcademicYearLabel(tenantId).orElse(null);
        return honorRollGenerator.generate(new gn.lacaeducation.grading.application.port.out.HonorRollPdfGenerator.HonorRoll(
                tenantId, periodLabel, schoolYear, rows));
    }

    /**
     * Centralisateur de classe (§retour fondateur, maquette {@code features/Centralisateur-bon-design.pdf}) :
     * une ligne par élève, moyennes générales mensuelles (toutes matières confondues) par semestre, puis
     * bilan annuel — s'appuie sur {@link AnnualReportCardUseCase#findByClass} pour le rang/mention/
     * décision/moyennes semestrielles (jamais recalculés différemment, pour rester identiques au
     * bulletin annuel) ; seuls les détails mensuels/composition "toutes matières" sont calculés ici, en
     * combinant {@code GradeCalculationService#monthlyBreakdown} (par matière) pondéré par coefficient —
     * même principe que la moyenne générale du bulletin, appliqué mois par mois.
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] classCentralizerPdf(Long classId, Long academicYearId) {
        List<gn.lacaeducation.grading.domain.AnnualReportCard> annualCards =
                annualReportCardUseCase.findByClass(null, classId, academicYearId);
        Long tenantId = annualCards.stream()
                .map(c -> studentProvider.findStudent(c.getStudentId()).map(StudentPublicInfo::tenantId).orElse(null))
                .filter(java.util.Objects::nonNull).findFirst().orElse(null);

        java.util.List<gn.lacaeducation.grading.application.port.out.ClassCentralizerPdfGenerator.StudentRow> rows = new java.util.ArrayList<>();
        java.util.SortedSet<java.time.YearMonth> s1MonthSet = new java.util.TreeSet<>();
        java.util.SortedSet<java.time.YearMonth> s2MonthSet = new java.util.TreeSet<>();
        BigDecimal s1Sum = BigDecimal.ZERO; int s1Count = 0;
        BigDecimal s2Sum = BigDecimal.ZERO; int s2Count = 0;
        int admitted = 0;

        for (gn.lacaeducation.grading.domain.AnnualReportCard card : annualCards.stream()
                .sorted(java.util.Comparator.comparing((gn.lacaeducation.grading.domain.AnnualReportCard c) ->
                        c.getRank() == null ? Integer.MAX_VALUE : c.getRank())).toList()) {
            StudentPublicInfo student = studentProvider.findStudent(card.getStudentId()).orElse(null);
            String fullName = student == null ? "Élève #" + card.getStudentId()
                    : (safeUpper(student.lastName()) + " " + safe(student.firstName())).trim();

            GeneralBreakdown s1 = card.getS1PeriodId() == null ? GeneralBreakdown.EMPTY
                    : generalBreakdown(card.getStudentId(), classId, card.getS1PeriodId());
            GeneralBreakdown s2 = card.getS2PeriodId() == null ? GeneralBreakdown.EMPTY
                    : generalBreakdown(card.getStudentId(), classId, card.getS2PeriodId());
            s1MonthSet.addAll(s1.monthly().keySet());
            s2MonthSet.addAll(s2.monthly().keySet());

            if (card.getS1Average() != null) { s1Sum = s1Sum.add(card.getS1Average()); s1Count++; }
            if (card.getS2Average() != null) { s2Sum = s2Sum.add(card.getS2Average()); s2Count++; }
            if ("ADMIS".equals(card.getDecision())) admitted++;

            rows.add(new gn.lacaeducation.grading.application.port.out.ClassCentralizerPdfGenerator.StudentRow(
                    fullName, student != null ? student.studentNumber() : null,
                    s1.monthly(), s1.compoAverage(), card.getS1Average(),
                    s2.monthly(), s2.compoAverage(), card.getS2Average(),
                    card.getOverallAverage(), card.getRank(), card.getHonor(), card.getDecision()));
        }

        String className = academicsProvider.findClass(classId)
                .map(c -> c.levelLabel() != null && !c.levelLabel().isBlank() ? c.label() + " (" + c.levelLabel() + ")" : c.label())
                .orElse(null);
        String schoolYear = tenantId == null ? null : academicsProvider.currentAcademicYearLabel(tenantId).orElse(null);
        String topStudent = rows.stream().filter(r -> Integer.valueOf(1).equals(r.rank()))
                .map(gn.lacaeducation.grading.application.port.out.ClassCentralizerPdfGenerator.StudentRow::studentName)
                .findFirst().orElse(null);
        BigDecimal s1ClassAvg = s1Count == 0 ? null : s1Sum.divide(BigDecimal.valueOf(s1Count), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal s2ClassAvg = s2Count == 0 ? null : s2Sum.divide(BigDecimal.valueOf(s2Count), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal annualClassAvg = (s1ClassAvg != null && s2ClassAvg != null)
                ? s1ClassAvg.add(s2ClassAvg).divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP)
                : (s1ClassAvg != null ? s1ClassAvg : s2ClassAvg);

        return centralizerGenerator.generate(new gn.lacaeducation.grading.application.port.out.ClassCentralizerPdfGenerator.Centralizer(
                tenantId, className, schoolYear, s1MonthSet.stream().toList(), s2MonthSet.stream().toList(),
                rows, s1ClassAvg, s2ClassAvg, annualClassAvg, admitted, topStudent));
    }

    private record GeneralBreakdown(java.util.SortedMap<java.time.YearMonth, BigDecimal> monthly, BigDecimal compoAverage) {
        static final GeneralBreakdown EMPTY = new GeneralBreakdown(new java.util.TreeMap<>(), null);
    }

    /** Combine le détail par matière ({@code monthlyBreakdown}, déjà "coefcié" par matière) en une
     * moyenne GÉNÉRALE (toutes matières) par mois et en composition générale — même formule que la
     * moyenne générale du bulletin ({@code Σ coef × valeur / Σ coef}), appliquée mois par mois plutôt
     * qu'une seule fois sur la période entière. */
    private GeneralBreakdown generalBreakdown(Long studentId, Long classId, Long periodId) {
        List<gn.lacaeducation.grading.domain.SubjectMonthlyBreakdown> perSubject =
                calculationService.monthlyBreakdown(studentId, classId, periodId);
        if (perSubject.isEmpty()) return GeneralBreakdown.EMPTY;

        java.util.SortedSet<java.time.YearMonth> months = new java.util.TreeSet<>();
        for (var s : perSubject) months.addAll(s.monthlyCours().keySet());

        java.util.SortedMap<java.time.YearMonth, BigDecimal> monthly = new java.util.TreeMap<>();
        for (java.time.YearMonth ym : months) {
            BigDecimal weighted = BigDecimal.ZERO;
            BigDecimal coefSum = BigDecimal.ZERO;
            for (var s : perSubject) {
                BigDecimal v = s.monthlyCours().get(ym);
                if (v == null) continue;
                BigDecimal coef = s.coefficient() != null ? s.coefficient() : BigDecimal.ONE;
                weighted = weighted.add(v.multiply(coef));
                coefSum = coefSum.add(coef);
            }
            if (coefSum.compareTo(BigDecimal.ZERO) > 0) {
                monthly.put(ym, weighted.divide(coefSum, 2, java.math.RoundingMode.HALF_UP));
            }
        }

        BigDecimal compoWeighted = BigDecimal.ZERO;
        BigDecimal compoCoefSum = BigDecimal.ZERO;
        for (var s : perSubject) {
            if (s.compoAverage() == null) continue;
            BigDecimal coef = s.coefficient() != null ? s.coefficient() : BigDecimal.ONE;
            compoWeighted = compoWeighted.add(s.compoAverage().multiply(coef));
            compoCoefSum = compoCoefSum.add(coef);
        }
        BigDecimal compoAverage = compoCoefSum.compareTo(BigDecimal.ZERO) > 0
                ? compoWeighted.divide(compoCoefSum, 2, java.math.RoundingMode.HALF_UP) : null;

        return new GeneralBreakdown(monthly, compoAverage);
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private static String safeUpper(String s) { return s == null ? "" : s.toUpperCase(java.util.Locale.FRENCH); }

    private String studentName(Long studentId) {
        if (studentId == null) return "—";
        return studentProvider.findStudent(studentId)
                .map(s -> ((s.firstName() == null ? "" : s.firstName()) + " "
                        + (s.lastName() == null ? "" : s.lastName())).trim())
                .filter(n -> !n.isBlank())
                .orElse("Élève #" + studentId);
    }

    /** Resolves human-readable names from the other modules so the PDF never shows raw IDs. */
    private ReportCardContext buildContext(ReportCard rc, List<ReportCardLine> lines) {
        StudentPublicInfo student = studentProvider.findStudent(rc.getStudentId()).orElse(null);
        Long tenantId = student != null ? student.tenantId() : null;
        String studentName = student == null ? null
                : ((student.firstName() != null ? student.firstName() : "") + " "
                   + (student.lastName() != null ? student.lastName() : "")).trim();
        gn.lacaeducation.academics.ClassPublicInfo classInfo = academicsProvider.findClass(rc.getClassId()).orElse(null);
        String className = classInfo != null ? classInfo.label() : null;
        String cycleLabel = classInfo != null ? classInfo.schoolCycle() : null;
        var period = academicsProvider.findPeriod(rc.getPeriodId()).orElse(null);
        String periodLabel = period != null ? period.label() : null;
        String schoolYear = tenantId == null ? null : academicsProvider.currentAcademicYearLabel(tenantId).orElse(null);
        var subjectIds = lines.stream().map(ReportCardLine::getSubjectId)
                .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        var monthlyBreakdown = calculationService.monthlyBreakdown(rc.getStudentId(), rc.getClassId(), rc.getPeriodId());

        // Semestre précédent (§retour fondateur) : sur le bulletin du 2ème semestre, on affiche aussi la
        // moyenne déjà connue du 1er — recherche parmi les "vraies" périodes semestrielles (celles reliées
        // à leur composition via compositionPeriodId, même convention que le bulletin annuel) de la même
        // année scolaire, celle qui précède immédiatement periodId.
        String previousSemesterLabel = null;
        java.math.BigDecimal previousSemesterAverage = null;
        String previousSemesterHonor = null;
        if (period != null && period.academicYearId() != null) {
            List<gn.lacaeducation.academics.PeriodPublicInfo> semesters = resolveSemesterPeriods(period.academicYearId());
            int idx = -1;
            for (int i = 0; i < semesters.size(); i++) {
                if (semesters.get(i).id().equals(rc.getPeriodId())) { idx = i; break; }
            }
            if (idx > 0) {
                var prev = semesters.get(idx - 1);
                previousSemesterLabel = prev.label();
                var prevCard = repo.findByStudentIdAndPeriodId(rc.getStudentId(), prev.id());
                previousSemesterAverage = prevCard.map(ReportCard::getOverallAverage).orElse(null);
                previousSemesterHonor = prevCard.map(ReportCard::getHonor).orElse(null);
            }
        }

        return new ReportCardContext(tenantId, studentName,
                student != null ? student.studentNumber() : null,
                className, cycleLabel, periodLabel, schoolYear, academicsProvider.subjectLabels(subjectIds), monthlyBreakdown,
                previousSemesterLabel, previousSemesterAverage, previousSemesterHonor,
                student != null ? student.photo() : null);
    }

    /** Les "vraies" périodes semestrielles d'une année (reliées à leur composition via
     * compositionPeriodId), dans l'ordre — même convention que {@code AnnualReportCardService}. Repli
     * sur toutes les périodes SEMESTRE si aucune n'a ce lien (écoles sans ce découpage). */
    private List<gn.lacaeducation.academics.PeriodPublicInfo> resolveSemesterPeriods(Long academicYearId) {
        List<gn.lacaeducation.academics.PeriodPublicInfo> all = academicsProvider.findPeriodsByAcademicYear(academicYearId).stream()
                .filter(p -> "SEMESTRE".equals(p.type()))
                .toList();
        List<gn.lacaeducation.academics.PeriodPublicInfo> withComposition = all.stream()
                .filter(p -> p.compositionPeriodId() != null)
                .toList();
        return withComposition.isEmpty() ? all : withComposition;
    }

    @Override
    public void delete(Long id) {
        // Les lignes référencent le bulletin sans cascade DB (V8__grading.sql) : les supprimer d'abord,
        // sinon deleteById échoue systématiquement en 409 dès qu'un bulletin a des lignes (toujours le cas).
        repo.deleteLinesByReportCardId(id);
        repo.deleteById(id);
    }

    private ReportCard load(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReportCard", id));
    }
}
