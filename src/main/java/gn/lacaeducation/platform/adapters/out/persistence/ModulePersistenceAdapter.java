package gn.lacaeducation.platform.adapters.out.persistence;

import gn.lacaeducation.platform.application.port.out.PlanModuleRepository;
import gn.lacaeducation.platform.application.port.out.TenantModuleRepository;
import gn.lacaeducation.platform.domain.ModuleCode;
import gn.lacaeducation.platform.domain.PlanModule;
import gn.lacaeducation.platform.domain.TenantModule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Persistence adapter for plan/tenant module associations (manual mapping, enum-backed). */
@Component
@RequiredArgsConstructor
class ModulePersistenceAdapter implements PlanModuleRepository, TenantModuleRepository {

    private final PlanModuleJpaRepository planModuleRepo;
    private final TenantModuleJpaRepository tenantModuleRepo;

    // ── PlanModuleRepository ──────────────────────────────────────────
    @Override
    public PlanModule save(PlanModule pm) {
        PlanModuleJpaEntity e = pm.getId() != null
                ? planModuleRepo.findById(pm.getId()).orElseGet(PlanModuleJpaEntity::new)
                : new PlanModuleJpaEntity();
        e.setPlanId(pm.getPlanId());
        e.setModuleCode(pm.getModuleCode());
        e.setActive(pm.isActive());
        return toDomain(planModuleRepo.save(e));
    }

    @Override
    public List<PlanModule> findByPlanId(Long planId) {
        return planModuleRepo.findByPlanId(planId).stream().map(ModulePersistenceAdapter::toDomain).toList();
    }

    @Override
    public Optional<PlanModule> findByPlanIdAndModuleCode(Long planId, ModuleCode moduleCode) {
        return planModuleRepo.findByPlanIdAndModuleCode(planId, moduleCode).map(ModulePersistenceAdapter::toDomain);
    }

    @Override
    public void delete(Long id) {
        planModuleRepo.deleteById(id);
    }

    // ── TenantModuleRepository ────────────────────────────────────────
    @Override
    public TenantModule save(TenantModule tm) {
        TenantModuleJpaEntity e = tm.getId() != null
                ? tenantModuleRepo.findById(tm.getId()).orElseGet(TenantModuleJpaEntity::new)
                : new TenantModuleJpaEntity();
        e.setTenantId(tm.getTenantId());
        e.setModuleCode(tm.getModuleCode());
        e.setActive(tm.isActive());
        e.setActivationDate(tm.getActivationDate());
        e.setExpirationDate(tm.getExpirationDate());
        return toDomain(tenantModuleRepo.save(e));
    }

    @Override
    public List<TenantModule> findByTenantId(Long tenantId) {
        return tenantModuleRepo.findByTenantId(tenantId).stream().map(ModulePersistenceAdapter::toDomain).toList();
    }

    @Override
    public Optional<TenantModule> findByTenantIdAndModuleCode(Long tenantId, ModuleCode moduleCode) {
        return tenantModuleRepo.findByTenantIdAndModuleCode(tenantId, moduleCode).map(ModulePersistenceAdapter::toDomain);
    }

    // ── mapping ───────────────────────────────────────────────────────
    private static PlanModule toDomain(PlanModuleJpaEntity e) {
        return PlanModule.builder().id(e.getId()).planId(e.getPlanId())
                .moduleCode(e.getModuleCode()).active(e.isActive()).build();
    }

    private static TenantModule toDomain(TenantModuleJpaEntity e) {
        return TenantModule.builder().id(e.getId()).tenantId(e.getTenantId())
                .moduleCode(e.getModuleCode()).active(e.isActive())
                .activationDate(e.getActivationDate()).expirationDate(e.getExpirationDate()).build();
    }
}
