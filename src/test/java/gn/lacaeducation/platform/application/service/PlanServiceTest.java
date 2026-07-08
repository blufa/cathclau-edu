package gn.lacaeducation.platform.application.service;

import gn.lacaeducation.common.ConflictException;
import gn.lacaeducation.platform.application.port.in.PlanManagementUseCase.CreatePlanCommand;
import gn.lacaeducation.platform.application.port.out.PlanRepository;
import gn.lacaeducation.platform.domain.Plan;
import gn.lacaeducation.platform.domain.PlanCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock PlanRepository repo;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks PlanService service;

    private CreatePlanCommand cmd(PlanCode code) {
        return new CreatePlanCommand("Plan", code, 500, null, null, "desc",
                null, null, null, null, null, null, null, null, null);
    }

    @Test
    void create_duplicateCode_throwsConflict() {
        when(repo.findByCode(PlanCode.STARTER)).thenReturn(Optional.of(Plan.builder().id(1L).build()));

        assertThatThrownBy(() -> service.create(cmd(PlanCode.STARTER)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void create_defaultsArchivedAndTrialFalse() {
        when(repo.findByCode(any())).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Plan p = service.create(cmd(PlanCode.PREMIUM));

        assertThat(p.isArchived()).isFalse();
        assertThat(p.getTrial()).isFalse();
        assertThat(p.getDefaultTrial()).isFalse();
        assertThat(p.getPublicVisible()).isTrue();
    }

    @Test
    void setDefaultTrial_clearsOthersAndFlagsTarget() {
        Plan target = Plan.builder().id(2L).code(PlanCode.STARTER).build();
        Plan other = Plan.builder().id(1L).code(PlanCode.PREMIUM).defaultTrial(true).build();
        when(repo.findById(2L)).thenReturn(Optional.of(target));
        when(repo.findAll()).thenReturn(List.of(other, target));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Plan result = service.setDefaultTrial(2L);

        assertThat(result.getDefaultTrial()).isTrue();
        assertThat(result.getTrial()).isTrue();
        assertThat(other.getDefaultTrial()).isFalse();
    }
}
