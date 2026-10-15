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

    // La devise/le slogan/la hiérarchie administrative (IRE, DPE/DCE, DSSEE) sont inclus ici (et non
    // uniquement dans /school/profile, réservé à SCHOOL_ADMIN) car tout utilisateur du tenant pouvant
    // émettre un badge (ex. SECRETARY) doit pouvoir en imprimer le verso officiel. `phone` (numéro de
    // l'école) est imprimé sur les badges élève, pour joindre l'établissement en cas de besoin.
    public record BrandingResponse(Long tenantId, String schoolName, String logo, String brandColor,
                                    String motto, String slogan, String ire, String dpeLabel,
                                    String sousPrefectureLabel, String phone, String phone2, String address, String email) {}
    public record BrandingRequest(String logo, String brandColor) {}

    @Operation(summary = "Get the current school's branding")
    @GetMapping
    public BrandingResponse get() {
        return toResponse(tenantUseCase.findById(TenantContext.get()));
    }

    @Operation(summary = "Update the current school's logo and brand colour")
    @PreAuthorize("hasAuthority('ecole:profil')")
    @PutMapping
    public BrandingResponse update(@RequestBody BrandingRequest r) {
        return toResponse(tenantUseCase.updateBranding(TenantContext.get(), r.logo(), r.brandColor()));
    }

    private BrandingResponse toResponse(Tenant t) {
        return new BrandingResponse(t.getId(), t.getSchoolName(), t.getLogo(), t.getBrandColor(), t.getMotto(), t.getSlogan(),
                t.getIre(), t.getDpeLabel(), t.getSousPrefectureLabel(), t.getPhone(), t.getPhone2(), t.getAddress(), t.getEmail());
    }
}
