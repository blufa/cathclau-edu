package gn.lacaeducation.grading.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.grading.application.port.in.AssessmentUseCase;
import gn.lacaeducation.grading.application.port.out.AssessmentRepository;
import gn.lacaeducation.grading.application.port.out.ClassSubjectQueryPort;
import gn.lacaeducation.grading.application.port.out.ClassSubjectQueryPort.ClassSubjectInfo;
import gn.lacaeducation.grading.application.port.out.DocumentStoragePort;
import gn.lacaeducation.grading.application.port.out.ExamConvocationPdfGenerator;
import gn.lacaeducation.grading.application.port.out.ExamRepository;
import gn.lacaeducation.grading.application.port.out.ExamSupervisionRepository;
import gn.lacaeducation.grading.domain.Assessment;
import gn.lacaeducation.grading.domain.AssessmentType;
import gn.lacaeducation.grading.domain.Exam;
import gn.lacaeducation.grading.domain.ExamStatus;
import gn.lacaeducation.grading.domain.ExamType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamServiceTest {

    @Mock ExamRepository repo;
    @Mock ExamSupervisionRepository supervisionRepo;
    @Mock ExamConvocationPdfGenerator pdfGenerator;
    @Mock DocumentStoragePort documentStorage;
    @Mock AcademicsDataProvider academicsProvider;
    @Mock AssessmentRepository assessmentRepository;
    @Mock ClassSubjectQueryPort classSubjectQueryPort;
    @Mock AssessmentUseCase assessmentUseCase;
    @InjectMocks ExamService service;

    private Exam exam() {
        return Exam.builder().id(1L).tenantId(10L).classId(55L).periodId(2L).type(ExamType.SEMESTER)
                .label("Composition Semestre 1").scheduledDate(LocalDate.of(2026, 12, 10)).status(ExamStatus.PLANNED).build();
    }

    @Test
    void gradeEntryAssessment_existingAssessment_returnsItWithoutCreating() {
        when(repo.findById(1L)).thenReturn(Optional.of(exam()));
        Assessment existing = Assessment.builder().id(99L).examId(1L).subjectId(3L).type(AssessmentType.EXAM).build();
        when(assessmentRepository.findByExamId(1L)).thenReturn(List.of(existing));

        Assessment result = service.gradeEntryAssessment(1L, 3L);

        assertThat(result.getId()).isEqualTo(99L);
        verify(assessmentUseCase, never()).create(any());
    }

    @Test
    void gradeEntryAssessment_firstTime_createsAssessmentLinkedToExam() {
        when(repo.findById(1L)).thenReturn(Optional.of(exam()));
        when(assessmentRepository.findByExamId(1L)).thenReturn(List.of());
        when(classSubjectQueryPort.findByClassId(55L)).thenReturn(List.of(
                new ClassSubjectInfo(3L, new BigDecimal("2"), 42L)));
        when(assessmentUseCase.create(any())).thenAnswer(i -> {
            AssessmentUseCase.CreateAssessmentCommand c = i.getArgument(0);
            return Assessment.builder().id(100L).tenantId(c.tenantId()).classId(c.classId()).subjectId(c.subjectId())
                    .teacherId(c.teacherId()).periodId(c.periodId()).type(c.type()).label(c.label()).date(c.date())
                    .coefficient(c.coefficient()).maxScore(c.maxScore()).examId(c.examId()).build();
        });

        Assessment result = service.gradeEntryAssessment(1L, 3L);

        ArgumentCaptor<AssessmentUseCase.CreateAssessmentCommand> captor = ArgumentCaptor.forClass(AssessmentUseCase.CreateAssessmentCommand.class);
        verify(assessmentUseCase).create(captor.capture());
        AssessmentUseCase.CreateAssessmentCommand cmd = captor.getValue();
        assertThat(cmd.classId()).isEqualTo(55L);
        assertThat(cmd.subjectId()).isEqualTo(3L);
        assertThat(cmd.teacherId()).isEqualTo(42L);
        assertThat(cmd.periodId()).isEqualTo(2L);
        assertThat(cmd.type()).isEqualTo(AssessmentType.EXAM);
        assertThat(cmd.label()).isEqualTo("Composition Semestre 1");
        assertThat(cmd.date()).isEqualTo(LocalDate.of(2026, 12, 10));
        assertThat(cmd.examId()).isEqualTo(1L);
        assertThat(result.getExamId()).isEqualTo(1L);
    }

    @Test
    void gradeEntryAssessment_noScheduledDate_throws() {
        Exam undated = exam();
        undated.setScheduledDate(null);
        when(repo.findById(1L)).thenReturn(Optional.of(undated));
        when(assessmentRepository.findByExamId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.gradeEntryAssessment(1L, 3L))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void gradeEntryAssessment_noTeacherAssignedToSubject_throws() {
        when(repo.findById(1L)).thenReturn(Optional.of(exam()));
        when(assessmentRepository.findByExamId(1L)).thenReturn(List.of());
        when(classSubjectQueryPort.findByClassId(55L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.gradeEntryAssessment(1L, 3L))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void assessmentsByExam_delegatesToRepository() {
        Assessment a = Assessment.builder().id(5L).examId(1L).build();
        when(assessmentRepository.findByExamId(1L)).thenReturn(List.of(a));

        assertThat(service.assessmentsByExam(1L)).containsExactly(a);
    }
}
