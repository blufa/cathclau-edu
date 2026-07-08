package gn.lacaeducation.platform.application.service;

import gn.lacaeducation.common.ConflictException;
import gn.lacaeducation.platform.application.port.out.PlanModuleRepository;
import gn.lacaeducation.platform.application.port.out.PlanRepository;
import gn.lacaeducation.platform.application.port.out.TenantModuleRepository;
import gn.lacaeducation.platform.domain.ModuleCode;
import gn.lacaeducation.platform.domain.PlanModule;
import gn.lacaeducation.platform.domain.TenantModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModuleServiceTest {

    @Mock PlanRepository planRepository;
    @Mock PlanModuleRepository planModuleRepository;
    @Mock TenantModuleRepository tenantModuleRepository;
    @Mock gn.lacaeducation.platform.application.port.out.TenantRepository tenantRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks ModuleService service;

    @Test
    void addPlanModule_missingDependency_throwsConflict() {
        when(planRepository.existsById(1L)).thenReturn(true);
        when(planModuleRepository.findByPlanId(1L)).thenReturn(List.of());

        // GRADING requires ACADEMICS + STUDENTS, none active on the plan.
        assertThatThrownBy(() -> service.addPlanModule(1L, ModuleCode.GRADING))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void addPlanModule_withDependencies_saves() {
        when(planRepository.existsById(1L)).thenReturn(true);
        when(planModuleRepository.findByPlanId(1L)).thenReturn(List.of(
                PlanModule.builder().planId(1L).moduleCode(ModuleCode.STUDENTS).active(true).build(),
                PlanModule.builder().planId(1L).moduleCode(ModuleCode.ACADEMICS).active(true).build()));
        when(planModuleRepository.findByPlanIdAndModuleCode(1L, ModuleCode.GRADING)).thenReturn(Optional.empty());
        when(planModuleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PlanModule saved = service.addPlanModule(1L, ModuleCode.GRADING);

        assertThat(saved.getModuleCode()).isEqualTo(ModuleCode.GRADING);
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    void provisionFromPlan_copiesOnlyActiveModules() {
        when(planModuleRepository.findByPlanId(9L)).thenReturn(List.of(
                PlanModule.builder().planId(9L).moduleCode(ModuleCode.STUDENTS).active(true).build(),
                PlanModule.builder().planId(9L).moduleCode(ModuleCode.BILLING).active(true).build(),
                PlanModule.builder().planId(9L).moduleCode(ModuleCode.ACCOUNTING).active(false).build()));
        when(tenantModuleRepository.findByTenantIdAndModuleCode(any(), any())).thenReturn(Optional.empty());
        when(tenantModuleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.provisionFromPlan(5L, 9L);

        // STUDENTS + BILLING provisioned, ACCOUNTING (inactive) skipped.
        verify(tenantModuleRepository, times(2)).save(any());
    }

    @Test
    void isModuleActive_expiredOverride_returnsFalse() {
        when(tenantModuleRepository.findByTenantIdAndModuleCode(5L, ModuleCode.ACCOUNTING))
                .thenReturn(Optional.of(TenantModule.builder().tenantId(5L).moduleCode(ModuleCode.ACCOUNTING)
                        .active(true).expirationDate(LocalDate.now().minusDays(1)).build()));

        assertThat(service.isModuleActive(5L, ModuleCode.ACCOUNTING)).isFalse();
    }

    @Test
    void getActiveModules_nullTenant_returnsAll() {
        assertThat(service.getActiveModules(null)).containsExactlyInAnyOrder(ModuleCode.values());
    }
}
