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
}
