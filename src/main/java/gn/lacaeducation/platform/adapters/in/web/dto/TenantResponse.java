package gn.lacaeducation.platform.adapters.in.web.dto;

import gn.lacaeducation.platform.domain.EstablishmentType;
import gn.lacaeducation.platform.domain.Tenant;
import gn.lacaeducation.platform.domain.TenantStatus;

import java.time.LocalDateTime;
import java.util.Set;

public record TenantResponse(
    Long id,
    String schoolName,
    String slug,
    String email,
    String phone,
    String logo,
    String brandColor,
    String address,
    String city,
    String country,
    String website,
    String director,
    String accreditationNumber,
    String openingAuthorization,
    String motto,
    String slogan,
    String payoutMerchantNumber,
    String payoutOperator,
    String payoutAccountName,
    Set<EstablishmentType> establishmentTypes,
    TenantStatus status,
    LocalDateTime trialEndsAt,
    LocalDateTime activatedAt,
    LocalDateTime createdAt,
    /** Mot de passe temporaire généré, renvoyé une seule fois à la création ; null sinon. */
    String temporaryPassword
) {
    public static TenantResponse from(Tenant tenant) {
        return from(tenant, null);
    }

    public static TenantResponse from(Tenant tenant, String temporaryPassword) {
        return new TenantResponse(
            tenant.getId(), tenant.getSchoolName(), tenant.getSlug(), tenant.getEmail(),
            tenant.getPhone(), tenant.getLogo(), tenant.getBrandColor(), tenant.getAddress(), tenant.getCity(),
            tenant.getCountry(), tenant.getWebsite(), tenant.getDirector(),
            tenant.getAccreditationNumber(), tenant.getOpeningAuthorization(),
            tenant.getMotto(), tenant.getSlogan(),
            tenant.getPayoutMerchantNumber(), tenant.getPayoutOperator(), tenant.getPayoutAccountName(),
            tenant.getEstablishmentTypes(),
            tenant.getStatus(), tenant.getTrialEndsAt(), tenant.getActivatedAt(), tenant.getCreatedAt(),
            temporaryPassword
        );
    }
}
