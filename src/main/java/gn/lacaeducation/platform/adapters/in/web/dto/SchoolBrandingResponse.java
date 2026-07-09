package gn.lacaeducation.platform.adapters.in.web.dto;

import gn.lacaeducation.platform.application.port.in.SignupUseCase.BrandingView;

/** Public branding for a school login page, resolved by slug (features/01-platform.md §1). */
public record SchoolBrandingResponse(Long tenantId, String slug, String schoolName,
                                     String logo, String brandColor) {
    public static SchoolBrandingResponse from(BrandingView b) {
        return new SchoolBrandingResponse(b.tenantId(), b.slug(), b.schoolName(), b.logo(), b.brandColor());
    }
}
