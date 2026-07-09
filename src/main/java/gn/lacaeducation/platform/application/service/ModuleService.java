package gn.lacaeducation.platform.application.service;

import gn.lacaeducation.common.ConflictException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.platform.ModuleAccessProvider;
import gn.lacaeducation.platform.application.event.PlanModulesModifiedEvent;
import gn.lacaeducation.platform.application.event.TenantModuleActivatedEvent;
import gn.lacaeducation.platform.application.event.TenantModuleDeactivatedEvent;
import gn.lacaeducation.platform.application.port.out.PlanModuleRepository;
import gn.lacaeducation.platform.application.port.out.PlanRepository;
import gn.lacaeducation.platform.application.port.out.TenantModuleRepository;
import gn.lacaeducation.platform.application.port.out.TenantRepository;
import gn.lacaeducation.platform.domain.ModuleCode;
import gn.lacaeducation.platform.domain.PlanModule;
import gn.lacaeducation.platform.domain.TenantModule;
import gn.lacaeducation.platform.domain.TenantStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Plan/tenant modules (features/SCHOOL_back_complements.md §M): module catalogue, modules included per
 * plan, per-tenant effective modules with overrides, provisioning from the plan, and the module-access
 * API consumed by other modules / the front to gate features.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ModuleService implements ModuleAccessProvider {

    private final PlanRepository planRepository;
    private final PlanModuleRepository planModuleRepository;
    private final TenantModuleRepository tenantModuleRepository;
    private final TenantRepository tenantRepository;
    private final ApplicationEventPublisher eventPublisher;

    // ── Catalogue ─────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<ModuleCode> catalog() {
        return List.of(ModuleCode.values());
    }

    // ── Plan modules ──────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<PlanModule> planModules(Long planId) {
        requirePlan(planId);
        return planModuleRepository.findByPlanId(planId);
    }

    public PlanModule addPlanModule(Long planId, ModuleCode code) {
        requirePlan(planId);
        Set<ModuleCode> active = activePlanModuleCodes(planId);
        requireDependencies(code, active, "le plan");
        PlanModule pm = planModuleRepository.findByPlanIdAndModuleCode(planId, code)
                .orElse(PlanModule.builder().planId(planId).moduleCode(code).build());
        pm.setActive(true);
        PlanModule saved = planModuleRepository.save(pm);
        eventPublisher.publishEvent(new PlanModulesModifiedEvent(planId, code, true));
        return saved;
    }

    public void removePlanModule(Long planId, ModuleCode code) {
        planModuleRepository.findByPlanIdAndModuleCode(planId, code)
                .ifPresent(pm -> {
                    planModuleRepository.delete(pm.getId());
                    eventPublisher.publishEvent(new PlanModulesModifiedEvent(planId, code, false));
                });
    }

    public PlanModule setPlanModuleActive(Long planId, ModuleCode code, boolean active) {
        PlanModule pm = planModuleRepository.findByPlanIdAndModuleCode(planId, code)
                .orElseThrow(() -> new ResourceNotFoundException("Module non rattaché au plan : " + code));
        if (active) requireDependencies(code, activePlanModuleCodes(planId), "le plan");
        pm.setActive(active);
        PlanModule saved = planModuleRepository.save(pm);
        eventPublisher.publishEvent(new PlanModulesModifiedEvent(planId, code, active));
        return saved;
    }

    // ── Tenant modules ────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<TenantModule> tenantModules(Long tenantId) {
        return tenantModuleRepository.findByTenantId(tenantId);
    }

    /** Per-tenant override; an expiration date grants temporary access. */
    public TenantModule setTenantModuleActive(Long tenantId, ModuleCode code, boolean active, LocalDate expiration) {
        if (active) requireDependencies(code, activeTenantModuleCodes(tenantId), "l'école");
        TenantModule tm = tenantModuleRepository.findByTenantIdAndModuleCode(tenantId, code)
                .orElse(TenantModule.builder().tenantId(tenantId).moduleCode(code).build());
        tm.setActive(active);
        if (active && tm.getActivationDate() == null) tm.setActivationDate(LocalDate.now());
        tm.setExpirationDate(expiration);
        TenantModule saved = tenantModuleRepository.save(tm);
        eventPublisher.publishEvent(active
                ? new TenantModuleActivatedEvent(tenantId, code, expiration)
                : new TenantModuleDeactivatedEvent(tenantId, code));
        return saved;
    }

    /** Copies the plan's active modules onto the tenant (idempotent). Called at provisioning. */
    public void provisionFromPlan(Long tenantId, Long planId) {
        provisionFromPlan(tenantId, planId, null);
    }

    /**
     * Copies the plan's active modules onto the tenant with an optional expiration date (used for
     * trial plans so access ends with the trial period). Idempotent.
     */
    public void provisionFromPlan(Long tenantId, Long planId, LocalDate expiration) {
        for (PlanModule pm : planModuleRepository.findByPlanId(planId)) {
            if (!pm.isActive()) continue;
            TenantModule tm = tenantModuleRepository.findByTenantIdAndModuleCode(tenantId, pm.getModuleCode())
                    .orElse(TenantModule.builder().tenantId(tenantId).moduleCode(pm.getModuleCode())
                            .activationDate(LocalDate.now()).build());
            tm.setActive(true);
            tm.setExpirationDate(expiration);
            tenantModuleRepository.save(tm);
        }
    }

    /** The default trial plan, if one has been designated by the SUPER_ADMIN. */
    @Transactional(readOnly = true)
    public java.util.Optional<gn.lacaeducation.platform.domain.Plan> defaultTrialPlan() {
        return planRepository.findAll().stream().filter(p -> Boolean.TRUE.equals(p.getDefaultTrial())).findFirst();
    }

    // ── ModuleAccessProvider (public API) ─────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public Set<ModuleCode> getActiveModules(Long tenantId) {
        if (tenantId == null) return EnumSet.allOf(ModuleCode.class);
        // Active free trial → the modules of the configurable default trial plan (set by the SUPER_ADMIN).
        // Menus grey out once the trial ends and no paid plan provisions them.
        if (isTrialActive(tenantId)) return trialModules();
        LocalDate today = LocalDate.now();
        Set<ModuleCode> out = EnumSet.noneOf(ModuleCode.class);
        for (TenantModule tm : tenantModuleRepository.findByTenantId(tenantId)) {
            if (tm.isEffective(today)) out.add(tm.getModuleCode());
        }
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isModuleActive(Long tenantId, ModuleCode moduleCode) {
        if (tenantId == null) return true;
        if (isTrialActive(tenantId)) return trialModules().contains(moduleCode);
        return tenantModuleRepository.findByTenantIdAndModuleCode(tenantId, moduleCode)
                .map(tm -> tm.isEffective(LocalDate.now()))
                .orElse(false);
    }

    /** True while the tenant is in its (unexpired) free-trial window. */
    private boolean isTrialActive(Long tenantId) {
        return tenantRepository.findById(tenantId).map(t ->
                t.getStatus() == TenantStatus.TRIAL
                        && t.getTrialEndsAt() != null
                        && t.getTrialEndsAt().isAfter(java.time.LocalDateTime.now())
        ).orElse(false);
    }

    /** Modules granted during a trial: those of the designated default trial plan, or all if none is set. */
    private Set<ModuleCode> trialModules() {
        return defaultTrialPlan()
                .map(p -> {
                    Set<ModuleCode> set = EnumSet.noneOf(ModuleCode.class);
                    for (PlanModule pm : planModuleRepository.findByPlanId(p.getId())) {
                        if (pm.isActive()) set.add(pm.getModuleCode());
                    }
                    return (Set<ModuleCode>) set;
                })
                .orElseGet(() -> EnumSet.allOf(ModuleCode.class));
    }

    // ── helpers ───────────────────────────────────────────────────────
    private void requirePlan(Long planId) {
        if (!planRepository.existsById(planId)) {
            throw new ResourceNotFoundException("Plan introuvable : " + planId);
        }
    }

    private Set<ModuleCode> activePlanModuleCodes(Long planId) {
        Set<ModuleCode> set = EnumSet.noneOf(ModuleCode.class);
        for (PlanModule pm : planModuleRepository.findByPlanId(planId)) if (pm.isActive()) set.add(pm.getModuleCode());
        return set;
    }

    private Set<ModuleCode> activeTenantModuleCodes(Long tenantId) {
        return getActiveModules(tenantId);
    }

    private void requireDependencies(ModuleCode code, Set<ModuleCode> available, String scope) {
        for (ModuleCode dep : code.dependencies()) {
            if (!available.contains(dep)) {
                throw new ConflictException(
                        "Le module " + code + " requiert " + dep + " (activez-le d'abord sur " + scope + ").");
            }
        }
    }
}
