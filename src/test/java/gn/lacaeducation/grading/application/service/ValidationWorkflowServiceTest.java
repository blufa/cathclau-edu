package gn.lacaeducation.grading.application.service;

import gn.lacaeducation.grading.application.port.out.ValidationWorkflowRepository;
import gn.lacaeducation.grading.domain.ValidationStep;
import gn.lacaeducation.grading.domain.ValidationWorkflow;
import gn.lacaeducation.grading.domain.WorkflowStage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidationWorkflowServiceTest {

    @Mock ValidationWorkflowRepository repo;
    @InjectMocks ValidationWorkflowService service;

    private ValidationWorkflow at(WorkflowStage stage) {
        return ValidationWorkflow.builder().id(1L).tenantId(9L).classId(2L).periodId(3L).currentStage(stage).build();
    }

    @Test
    void advance_fromDraft_teacherSubmits() {
        when(repo.findById(1L)).thenReturn(Optional.of(at(WorkflowStage.DRAFT)));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        ValidationWorkflow w = service.advance(1L, 7L, List.of("TEACHER"), "notes prêtes");

        assertThat(w.getCurrentStage()).isEqualTo(WorkflowStage.SUBMITTED);
        ArgumentCaptor<ValidationStep> step = ArgumentCaptor.forClass(ValidationStep.class);
        verify(repo).saveStep(step.capture());
        assertThat(step.getValue().getAction()).isEqualTo("ADVANCE");
        assertThat(step.getValue().getActorId()).isEqualTo(7L);
        assertThat(step.getValue().getActedAt()).isNotNull();
    }

    @Test
    void advance_wrongRoleForStage_denied() {
        // Un TEACHER ne peut pas franchir DEPARTMENT_APPROVED (réservé responsable pédagogique/direction).
        when(repo.findById(1L)).thenReturn(Optional.of(at(WorkflowStage.SUBMITTED)));

        assertThatThrownBy(() -> service.advance(1L, 7L, List.of("TEACHER"), null))
                .isInstanceOf(AccessDeniedException.class);
        verify(repo, never()).saveStep(any());
    }

    @Test
    void advance_superAdmin_passesAnyStage() {
        when(repo.findById(1L)).thenReturn(Optional.of(at(WorkflowStage.PRINCIPAL_APPROVED)));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        ValidationWorkflow w = service.advance(1L, 1L, List.of("SUPER_ADMIN"), null);

        assertThat(w.getCurrentStage()).isEqualTo(WorkflowStage.PUBLISHED);
    }

    @Test
    void advance_atLocked_throws() {
        when(repo.findById(1L)).thenReturn(Optional.of(at(WorkflowStage.LOCKED)));

        assertThatThrownBy(() -> service.advance(1L, 1L, List.of("SUPER_ADMIN"), null))
                .isInstanceOf(gn.lacaeducation.common.BusinessRuleException.class);
    }

    @Test
    void reject_toEarlierStage_movesBack() {
        when(repo.findById(1L)).thenReturn(Optional.of(at(WorkflowStage.STUDIES_APPROVED)));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        ValidationWorkflow w = service.reject(1L, WorkflowStage.SUBMITTED, 5L, List.of("DIRECTOR"), "corrections");

        assertThat(w.getCurrentStage()).isEqualTo(WorkflowStage.SUBMITTED);
        ArgumentCaptor<ValidationStep> step = ArgumentCaptor.forClass(ValidationStep.class);
        verify(repo).saveStep(step.capture());
        assertThat(step.getValue().getAction()).isEqualTo("REJECT");
    }

    @Test
    void reject_toLaterStage_throws() {
        when(repo.findById(1L)).thenReturn(Optional.of(at(WorkflowStage.SUBMITTED)));

        assertThatThrownBy(() -> service.reject(1L, WorkflowStage.PUBLISHED, 5L, List.of("DIRECTOR"), null))
                .isInstanceOf(gn.lacaeducation.common.BusinessRuleException.class);
    }
}
