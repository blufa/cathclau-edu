package gn.lacaeducation.platform.adapters.in.web;

import gn.lacaeducation.platform.adapters.in.web.dto.TenantBrandingResponse;
import gn.lacaeducation.platform.application.port.in.CreateTenantUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public, unauthenticated branding endpoint for tenant login pages (§1.1). */
@Tag(name = "Public", description = "Unauthenticated tenant branding for login pages")
@RestController
@RequestMapping("/api/v1/public/tenants")
@RequiredArgsConstructor
public class PublicTenantController {

    private final CreateTenantUseCase tenantUseCase;

    @Operation(summary = "Get a tenant's public branding (logo, colors) for the login page")
    @GetMapping("/{id}/branding")
    public TenantBrandingResponse branding(@PathVariable Long id) {
        return TenantBrandingResponse.from(tenantUseCase.findById(id));
    }
}
