package gn.lacaeducation.platform.adapters.in.web;

import gn.lacaeducation.platform.application.service.ModuleService;
import gn.lacaeducation.platform.domain.ModuleCode;
import gn.lacaeducation.platform.domain.PlanModule;
import gn.lacaeducation.platform.domain.TenantModule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

/**
 * SUPER_ADMIN configuration of functional modules (features/SCHOOL_back_complements.md §M):
 * the module catalogue, modules included per plan, and per-tenant module overrides.
 */
@Tag(name = "Modules", description = "Module catalogue, plan modules and tenant module overrides")
@RestController
@RequestMapping("/api/v1/platform")
@RequiredArgsConstructor
public class ModuleController {

    private final ModuleService moduleService;

    @Operation(summary = "Module catalogue (to compose a plan)")
    @GetMapping("/modules")
    public List<ModuleInfo> catalog() {
        return moduleService.catalog().stream().map(ModuleInfo::from).toList();
    }

    // ── Plan modules ──────────────────────────────────────────────────
    @Operation(summary = "Modules included in a plan")
    @GetMapping("/plans/{id}/modules")
    public List<PlanModuleResponse> planModules(@PathVariable Long id) {
        return moduleService.planModules(id).stream().map(PlanModuleResponse::from).toList();
    }

    @Operation(summary = "Add a module to a plan")
    @PostMapping("/plans/{id}/modules")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public PlanModuleResponse addPlanModule(@PathVariable Long id, @RequestBody ModuleRef req) {
        return PlanModuleResponse.from(moduleService.addPlanModule(id, req.moduleCode()));
    }

    @Operation(summary = "Remove a module from a plan")
    @DeleteMapping("/plans/{id}/modules/{moduleCode}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removePlanModule(@PathVariable Long id, @PathVariable ModuleCode moduleCode) {
        moduleService.removePlanModule(id, moduleCode);
    }

    @Operation(summary = "Activate a plan module")
    @PatchMapping("/plans/{id}/modules/{moduleCode}/activer")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public PlanModuleResponse activatePlanModule(@PathVariable Long id, @PathVariable ModuleCode moduleCode) {
        return PlanModuleResponse.from(moduleService.setPlanModuleActive(id, moduleCode, true));
    }

    @Operation(summary = "Deactivate a plan module")
    @PatchMapping("/plans/{id}/modules/{moduleCode}/desactiver")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public PlanModuleResponse deactivatePlanModule(@PathVariable Long id, @PathVariable ModuleCode moduleCode) {
        return PlanModuleResponse.from(moduleService.setPlanModuleActive(id, moduleCode, false));
    }

    // ── Tenant modules ────────────────────────────────────────────────
    @Operation(summary = "Effective modules of a tenant")
    @GetMapping("/tenants/{id}/modules")
    public List<TenantModuleResponse> tenantModules(@PathVariable Long id) {
        return moduleService.tenantModules(id).stream().map(TenantModuleResponse::from).toList();
    }

    @Operation(summary = "Activate a module for a tenant (optional expiration for temporary access)")
    @PatchMapping("/tenants/{id}/modules/{moduleCode}/activer")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public TenantModuleResponse activateTenantModule(@PathVariable Long id, @PathVariable ModuleCode moduleCode,
                                                      @RequestParam(required = false) LocalDate expiration) {
        return TenantModuleResponse.from(moduleService.setTenantModuleActive(id, moduleCode, true, expiration));
    }

    @Operation(summary = "Deactivate a module for a tenant")
    @PatchMapping("/tenants/{id}/modules/{moduleCode}/desactiver")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public TenantModuleResponse deactivateTenantModule(@PathVariable Long id, @PathVariable ModuleCode moduleCode) {
        return TenantModuleResponse.from(moduleService.setTenantModuleActive(id, moduleCode, false, null));
    }

    /** Modules d'une école « comptabilité seule » : gestion utilisateurs + comptabilité + dépenses. */
    private static final EnumSet<ModuleCode> ACCOUNTING_ONLY =
            EnumSet.of(ModuleCode.IAM, ModuleCode.ACCOUNTING, ModuleCode.EXPENSES);

    @Operation(summary = "Preset « comptabilité seule » : active IAM + Comptabilité + Dépenses, désactive le reste")
    @PostMapping("/tenants/{id}/modules/preset/comptabilite")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<TenantModuleResponse> presetAccountingOnly(@PathVariable Long id) {
        return moduleService.applyModulePreset(id, ACCOUNTING_ONLY).stream()
                .map(TenantModuleResponse::from).toList();
    }

    // ── DTOs ──────────────────────────────────────────────────────────
    record ModuleRef(ModuleCode moduleCode) {}

    record ModuleInfo(String code, String label, boolean premium, List<String> dependencies) {
        static ModuleInfo from(ModuleCode m) {
            return new ModuleInfo(m.name(), m.label(), m.premium(),
                    m.dependencies().stream().map(Enum::name).toList());
        }
    }

    record PlanModuleResponse(Long id, Long planId, String moduleCode, boolean active) {
        static PlanModuleResponse from(PlanModule pm) {
            return new PlanModuleResponse(pm.getId(), pm.getPlanId(), pm.getModuleCode().name(), pm.isActive());
        }
    }

    record TenantModuleResponse(Long id, Long tenantId, String moduleCode, boolean active,
                                LocalDate activationDate, LocalDate expirationDate) {
        static TenantModuleResponse from(TenantModule tm) {
            return new TenantModuleResponse(tm.getId(), tm.getTenantId(), tm.getModuleCode().name(),
                    tm.isActive(), tm.getActivationDate(), tm.getExpirationDate());
        }
    }
}
