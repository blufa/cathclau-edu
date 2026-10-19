package gn.lacaeducation.grading.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.grading.application.port.in.GradeSimulationUseCase.SimulationReport;
import gn.lacaeducation.grading.application.port.in.GradeSimulationUseCase.StudentSimulation;
import gn.lacaeducation.grading.application.port.out.AssessmentRepository;
import gn.lacaeducation.grading.application.port.out.ClassSubjectQueryPort;
import gn.lacaeducation.grading.application.port.out.ClassSubjectQueryPort.ClassSubjectInfo;
import gn.lacaeducation.grading.application.port.out.GradeRepository;
import gn.lacaeducation.grading.domain.Assessment;
import gn.lacaeducation.grading.domain.AssessmentType;
import gn.lacaeducation.grading.domain.Grade;
import gn.lacaeducation.students.StudentDataProvider;
import gn.lacaeducation.students.StudentPublicInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradeSimulationServiceTest {

    @Mock AssessmentRepository assessmentRepository;
    @Mock GradeRepository gradeRepository;
    @Mock ClassSubjectQueryPort classSubjectQueryPort;
    @Mock AcademicsDataProvider academicsDataProvider;
    @Mock StudentDataProvider studentDataProvider;
    @InjectMocks GradeSimulationService service;

    private StudentPublicInfo student(long id, String first) {
        return new StudentPublicInfo(id, 99L, first, "N", null, null, "M-" + id, null, null, "M", null);
    }

    @Test
    void simulate_computesAveragesRanksAndAnomalies() {
        Assessment math = Assessment.builder().id(10L).tenantId(99L).classId(1L).subjectId(1L)
                .periodId(2L).type(AssessmentType.QUIZ).date(LocalDate.of(2026, 10, 5))
                .coefficient(BigDecimal.ONE).maxScore(BigDecimal.valueOf(20)).build();
        Assessment french = Assessment.builder().id(20L).tenantId(99L).classId(1L).subjectId(2L)
                .periodId(2L).type(AssessmentType.QUIZ).date(LocalDate.of(2026, 10, 5))
                .coefficient(BigDecimal.ONE).maxScore(BigDecimal.valueOf(20)).build();
        when(assessmentRepository.findByClassIdAndPeriodId(1L, 2L)).thenReturn(List.of(math, french));
        when(classSubjectQueryPort.findByClassId(1L)).thenReturn(List.of(
                new ClassSubjectInfo(1L, new BigDecimal("5"), null),   // Maths coeff 5
                new ClassSubjectInfo(2L, new BigDecimal("4"), null))); // Français coeff 4
        when(studentDataProvider.findStudentsByClass(1L)).thenReturn(List.of(
                student(1, "Awa"), student(2, "Bah"), student(3, "Cissé")));
        // Maths : élève 1 = 14, élève 2 = 8 (élève 3 : rien)
        when(gradeRepository.findByAssessmentId(10L)).thenReturn(List.of(
                Grade.builder().assessmentId(10L).studentId(1L).value(new BigDecimal("14")).build(),
                Grade.builder().assessmentId(10L).studentId(2L).value(new BigDecimal("8")).build()));
        // Français : seul l'élève 1 a une note
        when(gradeRepository.findByAssessmentId(20L)).thenReturn(List.of(
                Grade.builder().assessmentId(20L).studentId(1L).value(new BigDecimal("12")).build()));

        SimulationReport report = service.simulate(1L, 2L);

        assertThat(report.studentCount()).isEqualTo(3);
        assertThat(report.studentsWithoutGrades()).isEqualTo(1);   // élève 3
        assertThat(report.studentsWithMissingSubjects()).isEqualTo(2); // élèves 2 et 3

        StudentSimulation s1 = byId(report, 1L);
        // (14×5 + 12×4) / 9 = 118/9 = 13.11
        assertThat(s1.overallAverage()).isEqualByComparingTo("13.11");
        assertThat(s1.rank()).isEqualTo(1);
        assertThat(s1.missingSubjects()).isEmpty();

        StudentSimulation s2 = byId(report, 2L);
        assertThat(s2.overallAverage()).isEqualByComparingTo("8.00"); // 8×5/5
        assertThat(s2.rank()).isEqualTo(2);
        assertThat(s2.missingSubjects()).containsExactly(2L);

        StudentSimulation s3 = byId(report, 3L);
        assertThat(s3.hasNoGrades()).isTrue();
        assertThat(s3.overallAverage()).isNull();
        assertThat(s3.rank()).isNull();
    }

    private StudentSimulation byId(SimulationReport r, Long id) {
        return r.students().stream().filter(s -> s.studentId().equals(id)).findFirst().orElseThrow();
    }
}
