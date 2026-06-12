package gn.lacaeducation.platform.adapters.in.web.dto;

import gn.lacaeducation.platform.domain.Tenant;

/** Public branding for a tenant's login page (logo + colors). */
public record TenantBrandingResponse(Long id, String schoolName, String logo, String brandColor) {
    public static TenantBrandingResponse from(Tenant t) {
        return new TenantBrandingResponse(t.getId(), t.getSchoolName(), t.getLogo(), t.getBrandColor());
    }
}
