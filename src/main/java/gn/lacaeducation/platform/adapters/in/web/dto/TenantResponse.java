package gn.lacaeducation.platform.adapters.in.web.dto;

import gn.lacaeducation.platform.domain.Tenant;
import gn.lacaeducation.platform.domain.TenantStatus;

import java.time.LocalDateTime;

public record TenantResponse(
    Long id,
    String schoolName,
    String email,
    String phone,
    String address,
    String city,
    String country,
    String director,
    TenantStatus status,
    LocalDateTime createdAt
) {
    public static TenantResponse from(Tenant tenant) {
        return new TenantResponse(
            tenant.getId(), tenant.getSchoolName(), tenant.getEmail(),
            tenant.getPhone(), tenant.getAddress(), tenant.getCity(),
            tenant.getCountry(), tenant.getDirector(), tenant.getStatus(), tenant.getCreatedAt()
        );
    }
}
