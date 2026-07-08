package gn.lacaeducation.platform.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.platform.application.port.in.CreateTenantUseCase;
import gn.lacaeducation.platform.domain.Tenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Self-service branding (logo + colour) for the current school, applied across its whole UI. */
@Tag(name = "Tenant Branding", description = "A school manages its own logo and brand colour")
@RestController
@RequestMapping("/api/v1/tenant/branding")
@RequiredArgsConstructor
public class TenantBrandingController {

    private final CreateTenantUseCase tenantUseCase;

    public record BrandingResponse(Long tenantId, String schoolName, String logo, String brandColor) {}
    public record BrandingRequest(String logo, String brandColor) {}

    @Operation(summary = "Get the current school's branding")
    @GetMapping
    public BrandingResponse get() {
        Tenant t = tenantUseCase.findById(TenantContext.get());
        return new BrandingResponse(t.getId(), t.getSchoolName(), t.getLogo(), t.getBrandColor());
    }

    @Operation(summary = "Update the current school's logo and brand colour")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SCHOOL_ADMIN','DIRECTOR')")
    @PutMapping
    public BrandingResponse update(@RequestBody BrandingRequest r) {
        Tenant t = tenantUseCase.updateBranding(TenantContext.get(), r.logo(), r.brandColor());
        return new BrandingResponse(t.getId(), t.getSchoolName(), t.getLogo(), t.getBrandColor());
    }
}
