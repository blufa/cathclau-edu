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
     * Centralisateur de classe (§retour utilisateur, maquette officielle {@code features/
     * design-pro-officiel-centralisateur.pdf}) : une matière par colonne, un groupe de lignes par élève
     * (coef / 1er semestre [cours, comp, Moy 1] / 2ème semestre [cours, comp, Moy 2] / Annuelle) —
     * s'appuie entièrement sur {@link AnnualReportCardUseCase#findByClass} (mêmes lignes par matière que
     * le bulletin annuel, {@code AnnualReportCardLine}), jamais recalculé différemment pour éviter tout
     * écart entre les deux documents.
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] classCentralizerPdf(Long classId, Long academicYearId) {
        List<gn.lacaeducation.grading.domain.AnnualReportCard> annualCards =
                annualReportCardUseCase.findByClass(null, classId, academicYearId);
        Long tenantId = annualCards.stream()
                .map(c -> studentProvider.findStudent(c.getStudentId()).map(StudentPublicInfo::tenantId).orElse(null))
                .filter(java.util.Objects::nonNull).findFirst().orElse(null);

        var subjectInfos = classSubjectQueryPort.findByClassId(classId);
        var subjectLabels = academicsProvider.subjectLabels(subjectInfos.stream()
                .map(gn.lacaeducation.grading.application.port.out.ClassSubjectQueryPort.ClassSubjectInfo::subjectId)
                .collect(java.util.stream.Collectors.toSet()));
        List<gn.lacaeducation.grading.application.port.out.ClassCentralizerPdfGenerator.SubjectColumn> subjects = subjectInfos.stream()
                .map(i -> new gn.lacaeducation.grading.application.port.out.ClassCentralizerPdfGenerator.SubjectColumn(
                        i.subjectId(), subjectLabels.getOrDefault(i.subjectId(), "Matière " + i.subjectId()), i.coefficient()))
                .toList();

        String levelLabel = academicsProvider.findClass(classId).map(c -> c.levelLabel()).orElse(null);

        List<gn.lacaeducation.grading.application.port.out.ClassCentralizerPdfGenerator.StudentBlock> rows =
                annualCards.stream()
                        .sorted(java.util.Comparator.comparing((gn.lacaeducation.grading.domain.AnnualReportCard c) ->
                                c.getRank() == null ? Integer.MAX_VALUE : c.getRank()))
                        .map(card -> toStudentBlock(card, levelLabel))
                        .toList();

        String className = academicsProvider.findClass(classId)
                .map(c -> c.levelLabel() != null && !c.levelLabel().isBlank() ? c.label() + " (" + c.levelLabel() + ")" : c.label())
                .orElse(null);
        String schoolYear = tenantId == null ? null : academicsProvider.currentAcademicYearLabel(tenantId).orElse(null);

        return centralizerGenerator.generate(new gn.lacaeducation.grading.application.port.out.ClassCentralizerPdfGenerator.Centralizer(
                tenantId, className, levelLabel, schoolYear, subjects, rows));
    }

    private gn.lacaeducation.grading.application.port.out.ClassCentralizerPdfGenerator.StudentBlock toStudentBlock(
            gn.lacaeducation.grading.domain.AnnualReportCard card, String levelLabel) {
        StudentPublicInfo student = studentProvider.findStudent(card.getStudentId()).orElse(null);
        String fullName = student == null ? "Élève #" + card.getStudentId()
                : (safeUpper(student.lastName()) + " " + safe(student.firstName())).trim();

        java.util.Map<Long, BigDecimal> s1Cours = new java.util.LinkedHashMap<>();
        java.util.Map<Long, BigDecimal> s1Compo = new java.util.LinkedHashMap<>();
        java.util.Map<Long, BigDecimal> s1Average = new java.util.LinkedHashMap<>();
        java.util.Map<Long, BigDecimal> s2Cours = new java.util.LinkedHashMap<>();
        java.util.Map<Long, BigDecimal> s2Compo = new java.util.LinkedHashMap<>();
        java.util.Map<Long, BigDecimal> s2Average = new java.util.LinkedHashMap<>();
        java.util.Map<Long, BigDecimal> annualBySubject = new java.util.LinkedHashMap<>();
        for (var line : card.getLines() != null ? card.getLines() : List.<gn.lacaeducation.grading.domain.AnnualReportCardLine>of()) {
            Long subjectId = line.getSubjectId();
            putIfPresent(s1Cours, subjectId, line.getS1Cours());
            putIfPresent(s1Compo, subjectId, line.getS1Compo());
            putIfPresent(s1Average, subjectId, line.getS1Average());
            putIfPresent(s2Cours, subjectId, line.getS2Cours());
            putIfPresent(s2Compo, subjectId, line.getS2Compo());
            putIfPresent(s2Average, subjectId, line.getS2Average());
            putIfPresent(annualBySubject, subjectId, line.getAnnualAverage());
        }

        return new gn.lacaeducation.grading.application.port.out.ClassCentralizerPdfGenerator.StudentBlock(
                card.getStudentId(), student != null ? student.studentNumber() : null, fullName, levelLabel,
                s1Cours, s1Compo, s1Average, s2Cours, s2Compo, s2Average, annualBySubject,
                card.getOverallAverage(), card.getRank());
    }

    private static void putIfPresent(java.util.Map<Long, BigDecimal> map, Long key, BigDecimal value) {
        if (key != null && value != null) map.put(key, value);
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
