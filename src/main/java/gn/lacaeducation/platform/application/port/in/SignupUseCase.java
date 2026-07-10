package gn.lacaeducation.platform.application.port.in;

import gn.lacaeducation.platform.domain.EstablishmentType;
import gn.lacaeducation.platform.domain.TenantStatus;

import java.util.Set;

/** Self-service school registration & email verification (features/01-platform.md §1). */
public interface SignupUseCase {

    /** Registers a school in PENDING_VERIFICATION and emails a verification link. */
    SignupResult signup(SignupCommand command);

    /** Consumes a verification token: PENDING_VERIFICATION → TRIAL. Idempotent. */
    void verifyEmail(String token);

    /** Re-issues a verification link for a school still pending verification. */
    void resendVerification(String email);

    /** Public branding for a school's login page, resolved by slug. */
    BrandingView getBranding(String slug);

    record SignupCommand(String schoolName, String desiredSlug, String adminEmail, String adminUsername,
                         String password, String city, String country, Set<EstablishmentType> establishmentTypes,
                         String director, String desiredPlan,
                         String adminFirstName, String adminLastName, String adminPhone,
                         String payoutMerchantNumber, String payoutOperator) {}

    record SignupResult(Long tenantId, String slug, TenantStatus status, String message) {}

    record BrandingView(Long tenantId, String slug, String schoolName, String logo, String brandColor) {}
}
