package gn.lacaeducation.grading.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.academics.ClassPublicInfo;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.grading.application.port.in.AssessmentUseCase.CreateAssessmentCommand;
import gn.lacaeducation.grading.application.port.out.AssessmentGradeSheetPdfGenerator;
import gn.lacaeducation.grading.application.port.out.AssessmentRepository;
import gn.lacaeducation.grading.application.port.out.GradeRepository;
import gn.lacaeducation.grading.domain.Assessment;
import gn.lacaeducation.grading.domain.AssessmentType;
import gn.lacaeducation.grading.domain.Grade;
import gn.lacaeducation.students.StudentDataProvider;
import gn.lacaeducation.students.StudentPublicInfo;
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
class AssessmentServiceTest {

    @Mock AssessmentRepository repo;
    @Mock AcademicsDataProvider academicsDataProvider;
    @Mock StudentDataProvider studentDataProvider;
    @Mock GradeRepository gradeRepository;
    @Mock gn.lacaeducation.grading.application.port.out.AssessmentPlanPdfGenerator planPdfGenerator;
    @Mock AssessmentGradeSheetPdfGenerator gradeSheetPdfGenerator;
    @Mock org.springframework.context.ApplicationEventPublisher eventPublisher;
    @InjectMocks AssessmentService service;

    private CreateAssessmentCommand cmd() {
        return new CreateAssessmentCommand(10L, 1L, 100L, 5L, 2L, AssessmentType.HOMEWORK, null,
                "Devoir 1", LocalDate.of(2026, 1, 15), BigDecimal.ONE, BigDecimal.valueOf(20));
    }

    @Test
    void create_closedPeriod_throws() {
        when(academicsDataProvider.isPeriodOpenForGrades(2L)).thenReturn(false);

        assertThatThrownBy(() -> service.create(cmd())).isInstanceOf(BusinessRuleException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void create_openPeriod_saves() {
        when(academicsDataProvider.isPeriodOpenForGrades(2L)).thenReturn(true);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        Assessment a = service.create(cmd());

        org.assertj.core.api.Assertions.assertThat(a.getLabel()).isEqualTo("Devoir 1");
    }

    @Test
    void assessmentGradeSheetPdf_includesAlreadyEnteredGrades() {
        Assessment a = Assessment.builder().id(9L).tenantId(10L).classId(1L).subjectId(100L)
                .type(AssessmentType.HOMEWORK).label("Anglais — Oral — Octobre")
                .date(LocalDate.of(2026, 10, 15)).maxScore(BigDecimal.valueOf(20)).build();
        when(repo.findById(9L)).thenReturn(Optional.of(a));
        when(academicsDataProvider.findClass(1L)).thenReturn(Optional.of(
                new ClassPublicInfo(1L, "6eme A", "6eme", "NORMALE", null, null, "COLLEGE")));
        when(academicsDataProvider.subjectLabels(java.util.Set.of(100L))).thenReturn(java.util.Map.of(100L, "Anglais"));
        StudentPublicInfo s1 = new StudentPublicInfo(1L, 10L, "Aissatou", "Diallo", null, null, null, null, null, null);
        StudentPublicInfo s2 = new StudentPublicInfo(2L, 10L, "Mamadou", "Bah", null, null, null, null, null, null);
        when(studentDataProvider.findStudentsByClass(1L)).thenReturn(List.of(s1, s2));
        when(gradeRepository.findByAssessmentId(9L)).thenReturn(List.of(
                Grade.builder().assessmentId(9L).studentId(1L).value(BigDecimal.valueOf(16.5)).build()));
        when(gradeSheetPdfGenerator.generate(any())).thenReturn(new byte[]{1, 2, 3});

        byte[] pdf = service.assessmentGradeSheetPdf(9L);

        assertThat(pdf).isEqualTo(new byte[]{1, 2, 3});
        ArgumentCaptor<AssessmentGradeSheetPdfGenerator.AssessmentGradeSheet> captor =
                ArgumentCaptor.forClass(AssessmentGradeSheetPdfGenerator.AssessmentGradeSheet.class);
        verify(gradeSheetPdfGenerator).generate(captor.capture());
        AssessmentGradeSheetPdfGenerator.AssessmentGradeSheet sheet = captor.getValue();
        assertThat(sheet.subjectName()).isEqualTo("Anglais");
        assertThat(sheet.rows()).hasSize(2);
        // Bah (sans note) trié avant Diallo par nom de famille.
        assertThat(sheet.rows().get(0).studentName()).isEqualTo("Mamadou Bah");
        assertThat(sheet.rows().get(0).value()).isNull();
        assertThat(sheet.rows().get(1).studentName()).isEqualTo("Aissatou Diallo");
        assertThat(sheet.rows().get(1).value()).isEqualByComparingTo("16.5");
    }
}
