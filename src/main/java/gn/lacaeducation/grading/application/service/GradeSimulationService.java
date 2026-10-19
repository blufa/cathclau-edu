package gn.lacaeducation.grading.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.grading.application.port.in.GradeSimulationUseCase;
import gn.lacaeducation.grading.application.port.out.AssessmentRepository;
import gn.lacaeducation.grading.application.port.out.ClassSubjectQueryPort;
import gn.lacaeducation.grading.application.port.out.ClassSubjectQueryPort.ClassSubjectInfo;
import gn.lacaeducation.grading.application.port.out.GradeRepository;
import gn.lacaeducation.grading.domain.Assessment;
import gn.lacaeducation.grading.domain.CycleGroup;
import gn.lacaeducation.grading.domain.Grade;
import gn.lacaeducation.grading.domain.SubjectSemesterAverage;
import gn.lacaeducation.students.StudentDataProvider;
import gn.lacaeducation.students.StudentPublicInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Simulateur de bulletins (§9). Recalcule à blanc les moyennes/rangs d'une classe et signale les
 * anomalies, sans rien persister. La direction publie les bulletins seulement après validation.
 * Utilise exactement la même formule que la génération réelle des bulletins (voir
 * {@link SubjectSemesterAverage}, branchée dans {@link GradeCalculationService}).
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GradeSimulationService implements GradeSimulationUseCase {

    private final AssessmentRepository assessmentRepository;
    private final GradeRepository gradeRepository;
    private final ClassSubjectQueryPort classSubjectQueryPort;
    private final AcademicsDataProvider academicsDataProvider;
    private final StudentDataProvider studentDataProvider;

    @Override
    public SimulationReport simulate(Long classId, Long periodId) {
        List<Assessment> assessments = assessmentRepository.findByClassIdAndPeriodId(classId, periodId);
        List<ClassSubjectInfo> subjects = classSubjectQueryPort.findByClassId(classId);
        List<StudentPublicInfo> students = studentDataProvider.findStudentsByClass(classId);
        CycleGroup cycleGroup = resolveCycleGroup(classId);

        Map<Long, List<Assessment>> assessmentsBySubject = assessments.stream()
                .collect(Collectors.groupingBy(Assessment::getSubjectId));
        Map<Long, BigDecimal> subjectCoeff = subjects.stream()
                .collect(Collectors.toMap(ClassSubjectInfo::subjectId, ClassSubjectInfo::coefficient, (a, b) -> a));

        List<StudentSimulation> sims = new ArrayList<>();
        for (StudentPublicInfo student : students) {
            sims.add(simulateStudent(student, subjects, assessmentsBySubject, subjectCoeff, cycleGroup));
        }

        // Rang : par moyenne générale décroissante (élèves sans note en fin, non classés).
        assignRanks(sims);

        int withoutGrades = (int) sims.stream().filter(StudentSimulation::hasNoGrades).count();
        int withMissing = (int) sims.stream().filter(s -> !s.missingSubjects().isEmpty()).count();
        return new SimulationReport(classId, periodId, sims.size(), withoutGrades, withMissing, sims);
    }

    private StudentSimulation simulateStudent(StudentPublicInfo student, List<ClassSubjectInfo> subjects,
                                              Map<Long, List<Assessment>> assessmentsBySubject,
                                              Map<Long, BigDecimal> subjectCoeff, CycleGroup cycleGroup) {
        Long studentId = student.id();
        BigDecimal sumWeighted = BigDecimal.ZERO;
        BigDecimal sumCoeff = BigDecimal.ZERO;
        int gradedSubjects = 0;
        List<Long> missing = new ArrayList<>();

        for (ClassSubjectInfo subject : subjects) {
            List<Assessment> subjectAssessments = assessmentsBySubject.getOrDefault(subject.subjectId(), List.of());
            List<Grade> grades = subjectAssessments.stream()
                    .flatMap(a -> gradeRepository.findByAssessmentId(a.getId()).stream())
                    .filter(g -> studentId.equals(g.getStudentId()))
                    .toList();

            if (grades.isEmpty()) {
                missing.add(subject.subjectId());
                continue;
            }
            // Primaire (§retour pédagogique, même règle que GradeCalculationService) : coefficient
            // toujours neutralisé à 1, jamais celui configuré sur la matière.
            BigDecimal coeff = cycleGroup == CycleGroup.PRIMAIRE
                    ? BigDecimal.ONE
                    : subjectCoeff.getOrDefault(subject.subjectId(), BigDecimal.ONE);
            BigDecimal subjectAvg = SubjectSemesterAverage.compute(cycleGroup, coeff, subjectAssessments, grades).finalValue();
            if (subjectAvg == null) {
                missing.add(subject.subjectId());
                continue;
            }
            gradedSubjects++;
            // Le coefficient est déjà appliqué dans subjectAvg (voir SubjectSemesterAverage) : ne pas
            // le remultiplier ici.
            sumWeighted = sumWeighted.add(subjectAvg);
            sumCoeff = sumCoeff.add(coeff);
        }

        boolean hasNoGrades = gradedSubjects == 0;
        BigDecimal overall = sumCoeff.signum() == 0 ? null
                : sumWeighted.divide(sumCoeff, 2, java.math.RoundingMode.HALF_UP);
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

    /** Groupe de cycle (primaire/secondaire) de la classe, résolu via son niveau (module academics). */
    private CycleGroup resolveCycleGroup(Long classId) {
        if (classId == null) return CycleGroup.SECONDAIRE;
        return academicsDataProvider.findClass(classId)
                .map(c -> CycleGroup.fromSchoolCycle(c.schoolCycle()))
                .orElse(CycleGroup.SECONDAIRE);
    }

    private String fullName(StudentPublicInfo s) {
        String first = s.firstName() != null ? s.firstName() : "";
        String last = s.lastName() != null ? s.lastName() : "";
        return (first + " " + last).trim();
    }
}
