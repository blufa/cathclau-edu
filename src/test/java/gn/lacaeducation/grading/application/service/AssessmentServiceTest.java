package gn.lacaeducation.grading.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.grading.application.port.in.AssessmentUseCase.CreateAssessmentCommand;
import gn.lacaeducation.grading.application.port.out.AssessmentRepository;
import gn.lacaeducation.grading.domain.Assessment;
import gn.lacaeducation.grading.domain.AssessmentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceTest {

    @Mock AssessmentRepository repo;
    @Mock AcademicsDataProvider academicsDataProvider;
    @InjectMocks AssessmentService service;

    private CreateAssessmentCommand cmd() {
        return new CreateAssessmentCommand(10L, 1L, 100L, 5L, 2L, AssessmentType.HOMEWORK,
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
}
