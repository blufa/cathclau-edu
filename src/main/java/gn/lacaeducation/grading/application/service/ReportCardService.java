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
    private final gn.lacaeducation.grading.application.port.out.ClassHonorRollPdfGenerator honorRollGenerator;
    private final gn.lacaeducation.grading.application.port.out.ClassCentralizerPdfGenerator centralizerGenerator;

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
    public byte[] classHonorRollPdf(Long classId, Long periodId) {
        List<ReportCard> cards = repo.findByClassIdAndPeriodId(classId, periodId);
        // Classement : par rang (croissant, nuls en fin), puis moyenne décroissante.
        var rows = cards.stream()
                .sorted(java.util.Comparator
                        .comparing((ReportCard rc) -> rc.getRank() == null ? Integer.MAX_VALUE : rc.getRank())
                        .thenComparing(rc -> rc.getOverallAverage() == null ? java.math.BigDecimal.valueOf(-1) : rc.getOverallAverage(),
                                java.util.Comparator.reverseOrder()))
                .map(rc -> {
                    StudentPublicInfo s = studentProvider.findStudent(rc.getStudentId()).orElse(null);
                    return new gn.lacaeducation.grading.application.port.out.ClassHonorRollPdfGenerator.Row(
                            rc.getRank(), studentName(rc.getStudentId()),
                            s != null ? s.firstName() : null, s != null ? s.lastName() : null,
                            rc.getOverallAverage(), rc.getHonor(), s != null ? s.photo() : null);
                })
                .toList();
        Long tenantId = cards.stream()
                .map(rc -> studentProvider.findStudent(rc.getStudentId()).map(StudentPublicInfo::tenantId).orElse(null))
                .filter(java.util.Objects::nonNull).findFirst().orElse(null);
        String className = academicsProvider.findClass(classId)
                .map(c -> c.levelLabel() != null && !c.levelLabel().isBlank() ? c.label() + " (" + c.levelLabel() + ")" : c.label())
                .orElse(null);
        String periodLabel = academicsProvider.findPeriod(periodId).map(p -> p.label()).orElse(null);
        String schoolYear = tenantId == null ? null : academicsProvider.currentAcademicYearLabel(tenantId).orElse(null);
        java.math.BigDecimal classAvg = cards.isEmpty() ? null : cards.get(0).getClassAverage();
        return honorRollGenerator.generate(new gn.lacaeducation.grading.application.port.out.ClassHonorRollPdfGenerator.HonorRoll(
                tenantId, className, periodLabel, schoolYear, classAvg, rows));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] classCentralizerPdf(Long classId, Long periodId) {
        List<ReportCard> cards = repo.findByClassIdAndPeriodId(classId, periodId);
        var subjectInfos = classSubjectQueryPort.findByClassId(classId);
        List<Long> subjectIds = subjectInfos.stream()
                .map(gn.lacaeducation.grading.application.port.out.ClassSubjectQueryPort.ClassSubjectInfo::subjectId)
                .toList();
        java.util.Map<Long, java.math.BigDecimal> coeffs = subjectInfos.stream()
                .collect(java.util.stream.Collectors.toMap(
                        gn.lacaeducation.grading.application.port.out.ClassSubjectQueryPort.ClassSubjectInfo::subjectId,
                        gn.lacaeducation.grading.application.port.out.ClassSubjectQueryPort.ClassSubjectInfo::coefficient,
                        (a, b) -> a));

        var rows = cards.stream()
                .sorted(java.util.Comparator
                        .comparing((ReportCard rc) -> rc.getRank() == null ? Integer.MAX_VALUE : rc.getRank()))
                .map(rc -> {
                    StudentPublicInfo student = studentProvider.findStudent(rc.getStudentId()).orElse(null);
                    java.util.Map<Long, gn.lacaeducation.grading.application.port.out.ClassCentralizerPdfGenerator.SubjectCell> bySubject =
                            repo.findLines(rc.getId()).stream()
                                    .filter(l -> l.getSubjectId() != null)
                                    .collect(java.util.stream.Collectors.toMap(ReportCardLine::getSubjectId,
                                            l -> new gn.lacaeducation.grading.application.port.out.ClassCentralizerPdfGenerator.SubjectCell(
                                                    l.getCoursAverage(), l.getCompoAverage()),
                                            (a, b) -> a));
                    return new gn.lacaeducation.grading.application.port.out.ClassCentralizerPdfGenerator.StudentRow(
                            studentName(rc.getStudentId()), student != null ? student.studentNumber() : null,
                            bySubject, rc.getOverallAverage(), rc.getRank());
                })
                .toList();

        Long tenantId = cards.stream()
                .map(rc -> studentProvider.findStudent(rc.getStudentId()).map(StudentPublicInfo::tenantId).orElse(null))
                .filter(java.util.Objects::nonNull).findFirst().orElse(null);
        String className = academicsProvider.findClass(classId)
                .map(c -> c.levelLabel() != null && !c.levelLabel().isBlank() ? c.label() + " (" + c.levelLabel() + ")" : c.label())
                .orElse(null);
        String periodLabel = academicsProvider.findPeriod(periodId).map(p -> p.label()).orElse(null);
        String schoolYear = tenantId == null ? null : academicsProvider.currentAcademicYearLabel(tenantId).orElse(null);

        return centralizerGenerator.generate(new gn.lacaeducation.grading.application.port.out.ClassCentralizerPdfGenerator.Centralizer(
                tenantId, className, periodLabel, schoolYear, subjectIds, academicsProvider.subjectLabels(subjectIds),
                coeffs, rows));
    }

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
