package gn.lacaeducation.common;

/**
 * Public view of a tenant (school) exposed to other modules (notification, iam, billing/payments
 * PDF headers…). Owned by the platform module, which is the source of truth for tenants.
 * Includes the legal/contact details printed on official documents (logo, address, accreditation,
 * opening authorization).
 */
public record TenantPublicInfo(Long id, String schoolName, String email, String phone,
                               String logo, String address, String city, String country,
                               String website, String director,
                               String accreditationNumber, String openingAuthorization,
                               String brandColor) {}
