package gn.lacaeducation.platform.adapters.in.web;

import gn.lacaeducation.common.PageResponse;
import gn.lacaeducation.platform.adapters.in.web.dto.CreateTenantRequest;
import gn.lacaeducation.platform.adapters.in.web.dto.TenantResponse;
import gn.lacaeducation.platform.adapters.in.web.dto.UpdateTenantRequest;
import gn.lacaeducation.platform.application.port.in.CreateTenantUseCase;
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
        var tenant = tenantUseCase.create(new CreateTenantUseCase.CreateTenantCommand(
                req.schoolName(), req.email(), req.phone(), req.address(), req.city(), req.country(), req.director()));
        return ResponseEntity.created(URI.create("/api/v1/platform/tenants/" + tenant.getId()))
                .body(TenantResponse.from(tenant));
    }

    @Operation(summary = "List all tenants (paginated)")
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public PageResponse<TenantResponse> findAll(@PageableDefault(size = 20) Pageable pageable) {
        List<TenantResponse> list = tenantUseCase.findAll().stream().map(TenantResponse::from).toList();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());
        return PageResponse.of(new PageImpl<>(list.subList(start, end), pageable, list.size()));
    }

    @Operation(summary = "Get tenant by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public TenantResponse findById(@PathVariable Long id) {
        return TenantResponse.from(tenantUseCase.findById(id));
    }

    @Operation(summary = "Update tenant details")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public TenantResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTenantRequest req) {
        return TenantResponse.from(tenantUseCase.update(id, new CreateTenantUseCase.UpdateTenantCommand(
                req.schoolName(), req.logo(), req.brandColor(), req.address(), req.city(),
                req.country(), req.phone(), req.email(), req.website(), req.director())));
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

    @Operation(summary = "Soft-delete a tenant (archive)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { tenantUseCase.delete(id); }
}
