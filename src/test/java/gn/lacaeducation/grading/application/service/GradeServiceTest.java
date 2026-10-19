package gn.lacaeducation.grading.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.grading.application.event.GradesValidatedEvent;
import gn.lacaeducation.grading.application.port.in.GradeUseCase.EnterGradeCommand;
import gn.lacaeducation.grading.application.port.out.AssessmentRepository;
import gn.lacaeducation.grading.application.port.out.GradeRepository;
import gn.lacaeducation.grading.domain.Assessment;
import gn.lacaeducation.grading.domain.Grade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradeServiceTest {

    @Mock GradeRepository repo;
    @Mock AssessmentRepository assessmentRepository;
    @Mock AcademicsDataProvider academicsDataProvider;
    @Mock gn.lacaeducation.grading.application.port.out.GradeHistoryRepository historyRepository;
    @Mock gn.lacaeducation.students.StudentDataProvider studentDataProvider;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks GradeService service;

    private Assessment assessment() {
        return Assessment.builder().id(1L).periodId(2L).maxScore(BigDecimal.valueOf(20)).build();
    }

    @Test
    void enter_closedPeriod_throws() {
        when(assessmentRepository.findById(1L)).thenReturn(Optional.of(assessment()));
        when(academicsDataProvider.isPeriodOpenForGrades(2L)).thenReturn(false);

        assertThatThrownBy(() -> service.enter(new EnterGradeCommand(1L, 10L, BigDecimal.valueOf(12))))
                .isInstanceOf(BusinessRuleException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void enter_valueAboveMax_throws() {
        when(assessmentRepository.findById(1L)).thenReturn(Optional.of(assessment()));
        when(academicsDataProvider.isPeriodOpenForGrades(2L)).thenReturn(true);

        assertThatThrownBy(() -> service.enter(new EnterGradeCommand(1L, 10L, BigDecimal.valueOf(25))))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void enter_valid_saves() {
        when(assessmentRepository.findById(1L)).thenReturn(Optional.of(assessment()));
        when(academicsDataProvider.isPeriodOpenForGrades(2L)).thenReturn(true);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        Grade g = service.enter(new EnterGradeCommand(1L, 10L, BigDecimal.valueOf(12)));

        org.assertj.core.api.Assertions.assertThat(g.getValue()).isEqualByComparingTo("12");
    }

    @Test
    void update_validatedGrade_throwsLocked() {
        when(repo.findById(7L)).thenReturn(Optional.of(Grade.builder().id(7L).assessmentId(1L).validated(true).build()));

        assertThatThrownBy(() -> service.update(7L, BigDecimal.valueOf(10)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void validate_publishesNotesValidees() {
        when(repo.findById(7L)).thenReturn(Optional.of(Grade.builder().id(7L).assessmentId(1L).validated(false).build()));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.validate(7L);

        verify(eventPublisher).publishEvent(any(GradesValidatedEvent.class));
    }

    @Test
    void validate_publishesEventEnrichedWithClassAndPeriod() {
        when(repo.findById(7L)).thenReturn(Optional.of(Grade.builder().id(7L).assessmentId(1L).validated(false).build()));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(assessmentRepository.findById(1L)).thenReturn(
                Optional.of(Assessment.builder().id(1L).classId(55L).periodId(2L).build()));

        service.validate(7L, 99L);

        org.mockito.ArgumentCaptor<GradesValidatedEvent> captor =
                org.mockito.ArgumentCaptor.forClass(GradesValidatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().classId()).isEqualTo(55L);
        org.assertj.core.api.Assertions.assertThat(captor.getValue().periodId()).isEqualTo(2L);
    }

    @Test
    void update_recordsHistoryWithOldNewValueAuthorAndReason() {
        when(repo.findById(7L)).thenReturn(Optional.of(
                Grade.builder().id(7L).assessmentId(1L).studentId(10L).value(BigDecimal.valueOf(8)).validated(false).build()));
        when(assessmentRepository.findById(1L)).thenReturn(Optional.of(assessment()));
        when(academicsDataProvider.isPeriodOpenForGrades(2L)).thenReturn(true);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.update(7L, BigDecimal.valueOf(14), 99L, "erreur de saisie");

        org.mockito.ArgumentCaptor<gn.lacaeducation.grading.domain.GradeHistory> captor =
                org.mockito.ArgumentCaptor.forClass(gn.lacaeducation.grading.domain.GradeHistory.class);
        verify(historyRepository).save(captor.capture());
        gn.lacaeducation.grading.domain.GradeHistory h = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(h.getAction())
                .isEqualTo(gn.lacaeducation.grading.domain.GradeAction.UPDATED);
        org.assertj.core.api.Assertions.assertThat(h.getOldValue()).isEqualByComparingTo("8");
        org.assertj.core.api.Assertions.assertThat(h.getNewValue()).isEqualByComparingTo("14");
        org.assertj.core.api.Assertions.assertThat(h.getChangedBy()).isEqualTo(99L);
        org.assertj.core.api.Assertions.assertThat(h.getReason()).isEqualTo("erreur de saisie");
    }

    @Test
    void update_withoutReason_throwsMotifRequired() {
        when(repo.findById(7L)).thenReturn(Optional.of(
                Grade.builder().id(7L).assessmentId(1L).studentId(10L).value(BigDecimal.valueOf(8)).validated(false).build()));

        assertThatThrownBy(() -> service.update(7L, BigDecimal.valueOf(14), 99L, "  "))
                .isInstanceOf(BusinessRuleException.class);
        verify(repo, never()).save(any());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void update_publishesGradeModifiedEventWithMotif() {
        when(repo.findById(7L)).thenReturn(Optional.of(
                Grade.builder().id(7L).assessmentId(1L).studentId(10L).value(BigDecimal.valueOf(8)).validated(false).build()));
        when(assessmentRepository.findById(1L)).thenReturn(Optional.of(
                Assessment.builder().id(1L).tenantId(3L).periodId(2L).maxScore(BigDecimal.valueOf(20)).build()));
        when(academicsDataProvider.isPeriodOpenForGrades(2L)).thenReturn(true);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.update(7L, BigDecimal.valueOf(14), 99L, "erreur de saisie");

        org.mockito.ArgumentCaptor<gn.lacaeducation.grading.application.event.GradeModifiedEvent> captor =
                org.mockito.ArgumentCaptor.forClass(gn.lacaeducation.grading.application.event.GradeModifiedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        gn.lacaeducation.grading.application.event.GradeModifiedEvent e = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(e.tenantId()).isEqualTo(3L);
        org.assertj.core.api.Assertions.assertThat(e.studentId()).isEqualTo(10L);
        org.assertj.core.api.Assertions.assertThat(e.oldValue()).isEqualByComparingTo("8");
        org.assertj.core.api.Assertions.assertThat(e.newValue()).isEqualByComparingTo("14");
        org.assertj.core.api.Assertions.assertThat(e.reason()).isEqualTo("erreur de saisie");
    }

    @Test
    void gradeSheet_joinsClassRosterWithGrades() {
        when(assessmentRepository.findById(1L)).thenReturn(
                Optional.of(Assessment.builder().id(1L).classId(55L).periodId(2L).build()));
        when(repo.findByAssessmentId(1L)).thenReturn(java.util.List.of(
                Grade.builder().id(100L).assessmentId(1L).studentId(10L).value(BigDecimal.valueOf(12)).validated(false).build()));
        when(studentDataProvider.findStudentsByClass(55L)).thenReturn(java.util.List.of(
                new gn.lacaeducation.students.StudentPublicInfo(10L, 3L, "Awa", "Bah", null, null, "MAT-1", null, null, null, null),
                new gn.lacaeducation.students.StudentPublicInfo(20L, 3L, "Moussa", "Camara", null, null, "MAT-2", null, null, null, null)));

        java.util.List<gn.lacaeducation.grading.application.port.in.GradeUseCase.GradeSheetRow> rows =
                service.gradeSheet(1L);

        org.assertj.core.api.Assertions.assertThat(rows).hasSize(2);
        // Élève noté
        var graded = rows.stream().filter(r -> r.studentId().equals(10L)).findFirst().orElseThrow();
        org.assertj.core.api.Assertions.assertThat(graded.fullName()).isEqualTo("Awa Bah");
        org.assertj.core.api.Assertions.assertThat(graded.value()).isEqualByComparingTo("12");
        org.assertj.core.api.Assertions.assertThat(graded.gradeId()).isEqualTo(100L);
        // Élève sans note saisie → valeurs nulles mais présent dans la feuille
        var blank = rows.stream().filter(r -> r.studentId().equals(20L)).findFirst().orElseThrow();
        org.assertj.core.api.Assertions.assertThat(blank.value()).isNull();
        org.assertj.core.api.Assertions.assertThat(blank.gradeId()).isNull();
        org.assertj.core.api.Assertions.assertThat(blank.studentNumber()).isEqualTo("MAT-2");
    }
}
