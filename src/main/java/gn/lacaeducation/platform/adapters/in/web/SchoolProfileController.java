package gn.lacaeducation.platform.adapters.in.web;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.platform.adapters.in.web.dto.TenantResponse;
import gn.lacaeducation.platform.adapters.in.web.dto.UpdateTenantRequest;
import gn.lacaeducation.platform.application.port.in.CreateTenantUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Self-service school profile for the tenant's own admin (ADMIN_ECOLE): read and update the school's
 * information (name, logo, address, contacts, director, accreditation, opening authorization) shown
 * on official documents. The tenant is resolved from {@link TenantContext} — an admin can only edit
 * their own school, never another tenant's.
 */
@Tag(name = "School Profile", description = "Édition autonome des informations de l'école par son administrateur")
@RestController
@RequestMapping("/api/v1/school/profile")
@RequiredArgsConstructor
public class SchoolProfileController {

    private final CreateTenantUseCase tenantUseCase;

    @Operation(summary = "Consulter le profil de l'école courante")
    @GetMapping
    @PreAuthorize("hasAuthority('ecole:profil')")
    public TenantResponse current() {
        return TenantResponse.from(tenantUseCase.findById(requireTenant()));
    }

    @Operation(summary = "Modifier le profil de l'école courante")
    @PutMapping
    @PreAuthorize("hasAuthority('ecole:profil')")
    public TenantResponse update(@Valid @RequestBody UpdateTenantRequest req) {
        return TenantResponse.from(tenantUseCase.update(requireTenant(), new CreateTenantUseCase.UpdateTenantCommand(
                req.schoolName(), req.logo(), req.brandColor(), req.address(), req.city(),
                req.country(), req.phone(), req.email(), req.website(), req.director(),
                req.accreditationNumber(), req.openingAuthorization(),
                req.payoutMerchantNumber(), req.payoutOperator(), req.payoutAccountName(),
                req.motto(), req.slogan(), req.ire(), req.dpeLabel(), req.sousPrefectureLabel(),
                req.matriculeFormat(), req.matriculeDigits())));
    }

    private Long requireTenant() {
        Long tenantId = TenantContext.get();
        if (tenantId == null) {
            throw new BusinessRuleException("Aucune école résolue pour l'utilisateur courant");
        }
        return tenantId;
    }
}
