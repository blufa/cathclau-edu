package gn.lacaeducation.grading.application.service;

import gn.lacaeducation.grading.application.port.in.GradeSimulationUseCase;
import gn.lacaeducation.grading.application.port.out.AssessmentRepository;
import gn.lacaeducation.grading.application.port.out.AssessmentTypeConfigRepository;
import gn.lacaeducation.grading.application.port.out.ClassSubjectQueryPort;
import gn.lacaeducation.grading.application.port.out.ClassSubjectQueryPort.ClassSubjectInfo;
import gn.lacaeducation.grading.application.port.out.GradeRepository;
import gn.lacaeducation.grading.application.port.out.GradingCalculationSettingsRepository;
import gn.lacaeducation.grading.domain.Assessment;
import gn.lacaeducation.grading.domain.AssessmentTypeConfig;
import gn.lacaeducation.grading.domain.Grade;
import gn.lacaeducation.grading.domain.GradingCalculationSettings;
import gn.lacaeducation.grading.domain.SubjectAverageCalculator;
import gn.lacaeducation.grading.domain.SubjectAverageCalculator.WeightedGrade;
import gn.lacaeducation.students.StudentDataProvider;
import gn.lacaeducation.students.StudentPublicInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Simulateur de bulletins (§9). Recalcule à blanc les moyennes/rangs d'une classe et signale les
 * anomalies, sans rien persister. La direction publie les bulletins seulement après validation.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GradeSimulationService implements GradeSimulationUseCase {

    private final AssessmentRepository assessmentRepository;
    private final GradeRepository gradeRepository;
    private final ClassSubjectQueryPort classSubjectQueryPort;
    private final GradingCalculationSettingsRepository calculationSettingsRepository;
    private final AssessmentTypeConfigRepository assessmentTypeConfigRepository;
    private final StudentDataProvider studentDataProvider;

    @Override
    public SimulationReport simulate(Long classId, Long periodId) {
        List<Assessment> assessments = assessmentRepository.findByClassIdAndPeriodId(classId, periodId);
        List<ClassSubjectInfo> subjects = classSubjectQueryPort.findByClassId(classId);
        List<StudentPublicInfo> students = studentDataProvider.findStudentsByClass(classId);
        GradingCalculationSettings settings = resolveSettings(assessments);
        Map<Long, BigDecimal> categoryWeights = resolveCategoryWeights(assessments);

        Map<Long, List<Assessment>> assessmentsBySubject = assessments.stream()
                .collect(Collectors.groupingBy(Assessment::getSubjectId));
        Map<Long, BigDecimal> subjectCoeff = subjects.stream()
                .collect(Collectors.toMap(ClassSubjectInfo::subjectId, ClassSubjectInfo::coefficient, (a, b) -> a));

        List<StudentSimulation> sims = new ArrayList<>();
        for (StudentPublicInfo student : students) {
            sims.add(simulateStudent(student, subjects, assessmentsBySubject, subjectCoeff, settings, categoryWeights));
        }

        // Rang : par moyenne générale décroissante (élèves sans note en fin, non classés).
        assignRanks(sims);

        int withoutGrades = (int) sims.stream().filter(StudentSimulation::hasNoGrades).count();
        int withMissing = (int) sims.stream().filter(s -> !s.missingSubjects().isEmpty()).count();
        return new SimulationReport(classId, periodId, sims.size(), withoutGrades, withMissing, sims);
    }

    private StudentSimulation simulateStudent(StudentPublicInfo student, List<ClassSubjectInfo> subjects,
                                              Map<Long, List<Assessment>> assessmentsBySubject,
                                              Map<Long, BigDecimal> subjectCoeff, GradingCalculationSettings settings,
                                              Map<Long, BigDecimal> categoryWeights) {
        Long studentId = student.id();
        BigDecimal sumWeighted = BigDecimal.ZERO;
        BigDecimal sumCoeff = BigDecimal.ZERO;
        int gradedSubjects = 0;
        List<Long> missing = new ArrayList<>();

        for (ClassSubjectInfo subject : subjects) {
            List<Assessment> subjectAssessments = assessmentsBySubject.getOrDefault(subject.subjectId(), List.of());
            Map<Long, BigDecimal> coeffByAssessment = subjectAssessments.stream()
                    .collect(Collectors.toMap(Assessment::getId,
                            a -> a.getCoefficient() != null ? a.getCoefficient() : BigDecimal.ONE));
            Map<Long, Long> categoryByAssessment = subjectAssessments.stream()
                    .filter(a -> a.getAssessmentTypeId() != null)
                    .collect(Collectors.toMap(Assessment::getId, Assessment::getAssessmentTypeId));
            List<WeightedGrade> grades = subjectAssessments.stream()
                    .flatMap(a -> gradeRepository.findByAssessmentId(a.getId()).stream())
                    .filter(g -> studentId.equals(g.getStudentId()))
                    .map(g -> new WeightedGrade(g.getValue(),
                            coeffByAssessment.getOrDefault(g.getAssessmentId(), BigDecimal.ONE),
                            categoryByAssessment.get(g.getAssessmentId())))
                    .toList();

            if (grades.isEmpty()) {
                missing.add(subject.subjectId());
                continue;
            }
            gradedSubjects++;
            BigDecimal subjectAvg = SubjectAverageCalculator.compute(
                    settings.methodOrDefault(), grades, categoryWeights,
                    settings.decimalsOrDefault(), settings.roundingOrDefault());
            BigDecimal coeff = subjectCoeff.getOrDefault(subject.subjectId(), BigDecimal.ONE);
            sumWeighted = sumWeighted.add(subjectAvg.multiply(coeff));
            sumCoeff = sumCoeff.add(coeff);
        }

        boolean hasNoGrades = gradedSubjects == 0;
        BigDecimal overall = sumCoeff.signum() == 0 ? null
                : sumWeighted.divide(sumCoeff, settings.decimalsOrDefault(),
                        settings.roundingOrDefault().toRoundingMode());
        return new StudentSimulation(studentId, fullName(student), overall, null, gradedSubjects, missing, hasNoGrades);
    }

    private void assignRanks(List<StudentSimulation> sims) {
        List<StudentSimulation> graded = sims.stream()
                .filter(s -> s.overallAverage() != null)
                .sorted(Comparator.comparing(StudentSimulation::overallAverage).reversed())
                .toList();
        Map<Long, Integer> rankByStudent = new java.util.HashMap<>();
        int rank = 1;
        for (StudentSimulation s : graded) {
            rankByStudent.put(s.studentId(), rank++);
        }
        // Réinjecte le rang (records immuables → on remplace les éléments de la liste).
        sims.replaceAll(s -> new StudentSimulation(s.studentId(), s.studentName(), s.overallAverage(),
                rankByStudent.get(s.studentId()), s.gradedSubjects(), s.missingSubjects(), s.hasNoGrades()));
    }

    private GradingCalculationSettings resolveSettings(List<Assessment> assessments) {
        Long tenantId = assessments.stream()
                .map(Assessment::getTenantId).filter(java.util.Objects::nonNull).findFirst().orElse(null);
        if (tenantId == null) return GradingCalculationSettings.defaults();
        return calculationSettingsRepository.findByTenantId(tenantId).orElseGet(GradingCalculationSettings::defaults);
    }

    /**
     * Poids de catégorie (type d'évaluation) du tenant, pour la méthode {@code BY_CATEGORY} :
     * map {@code assessmentTypeId -> poids}. Seuls les types actifs à poids strictement positif sont
     * retenus ; vide si non applicable (retombe alors sur la moyenne pondérée).
     */
    private Map<Long, BigDecimal> resolveCategoryWeights(List<Assessment> assessments) {
        Long tenantId = assessments.stream()
                .map(Assessment::getTenantId).filter(java.util.Objects::nonNull).findFirst().orElse(null);
        if (tenantId == null) return Map.of();
        return assessmentTypeConfigRepository.findByTenantId(tenantId).stream()
                .filter(c -> c.isActive() && c.getWeight() != null && c.getWeight().signum() > 0)
                .collect(Collectors.toMap(AssessmentTypeConfig::getId, AssessmentTypeConfig::getWeight, (a, b) -> a));
    }

    private String fullName(StudentPublicInfo s) {
        String first = s.firstName() != null ? s.firstName() : "";
        String last = s.lastName() != null ? s.lastName() : "";
        return (first + " " + last).trim();
    }
}
