package gn.lacaeducation.platform.adapters.in.web;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.PageResponse;
import gn.lacaeducation.platform.adapters.in.web.dto.CreateTenantRequest;
import gn.lacaeducation.platform.adapters.in.web.dto.TenantDetailResponse;
import gn.lacaeducation.platform.adapters.in.web.dto.TenantResponse;
import gn.lacaeducation.platform.adapters.in.web.dto.UpdateTenantRequest;
import gn.lacaeducation.platform.application.port.in.CreateTenantUseCase;
import gn.lacaeducation.platform.domain.TenantStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Tag(name = "Tenants", description = "School tenant management (SUPER_ADMIN only)")
@RestController
@RequestMapping("/api/v1/platform/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final CreateTenantUseCase tenantUseCase;

    @Operation(summary = "Create a new school tenant")
    @ApiResponse(responseCode = "201", description = "Tenant created")
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<TenantResponse> create(@Valid @RequestBody CreateTenantRequest req) {
        var created = tenantUseCase.create(new CreateTenantUseCase.CreateTenantCommand(
                req.schoolName(), req.slug(), req.email(), req.phone(), req.address(), req.city(),
                req.country(), req.director(), req.establishmentTypes(), req.initialState(), req.adminPassword(),
                req.username()));
        var tenant = created.tenant();
        return ResponseEntity.created(URI.create("/api/v1/platform/tenants/" + tenant.getId()))
                .body(TenantResponse.from(tenant, created.temporaryPassword()));
    }

    @Operation(summary = "List/search tenants (filters: status, city)")
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public PageResponse<TenantResponse> findAll(@RequestParam(required = false) String status,
                                                @RequestParam(required = false) String city,
                                                @PageableDefault(size = 20) Pageable pageable) {
        List<TenantResponse> list = tenantUseCase.findAll(status, city).stream().map(TenantResponse::from).toList();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());
        return PageResponse.of(new PageImpl<>(list.subList(start, end), pageable, list.size()));
    }

    @Operation(summary = "Get tenant detail (current subscription + consumed quota)")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public TenantDetailResponse findById(@PathVariable Long id) {
        return TenantDetailResponse.from(tenantUseCase.getDetail(id));
    }

    @Operation(summary = "Update tenant details")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public TenantResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTenantRequest req) {
        return TenantResponse.from(tenantUseCase.update(id, new CreateTenantUseCase.UpdateTenantCommand(
                req.schoolName(), req.logo(), req.brandColor(), req.address(), req.city(),
                req.country(), req.phone(), req.email(), req.website(), req.director(), req.founderPhone(),
                req.accreditationNumber(), req.openingAuthorization(),
                req.payoutMerchantNumber(), req.payoutOperator(), req.payoutAccountName(),
                req.motto(), req.slogan(), req.ire(), req.dpeLabel(), req.sousPrefectureLabel(),
                req.matriculeFormat(), req.matriculeDigits())));
    }

    @Operation(summary = "Suspend a tenant (blocks all users)")
    @PatchMapping("/{id}/suspend")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void suspend(@PathVariable Long id) { tenantUseCase.suspend(id); }

    @Operation(summary = "Reactivate a suspended tenant")
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activate(@PathVariable Long id) { tenantUseCase.activate(id); }

    @Operation(summary = "Active/désactive l'alerte SMS au personnel (fondateur + encaisseur) à chaque "
            + "paiement reçu — en plus du SMS parent, toujours envoyé. Désactivé par défaut.")
    @PatchMapping("/{id}/notify-staff-on-payment")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setNotifyStaffOnPayment(@PathVariable Long id, @RequestParam boolean enabled) {
        tenantUseCase.setNotifyStaffOnPayment(id, enabled);
    }

    @Operation(summary = "Change tenant status (ACTIVATE | SUSPEND | ARCHIVE) via the lifecycle state machine")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeStatus(@PathVariable Long id, @RequestParam String action) {
        var target = switch (action.toUpperCase()) {
            case "ACTIVATE" -> TenantStatus.ACTIVE;
            case "SUSPEND" -> TenantStatus.SUSPENDED;
            case "ARCHIVE" -> TenantStatus.ARCHIVED;
            default -> throw new BusinessRuleException("Unknown status action: " + action);
        };
        tenantUseCase.changeStatus(id, target);
    }

    @Operation(summary = "Soft-delete a tenant (archive)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { tenantUseCase.delete(id); }
}
