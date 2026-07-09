package gn.lacaeducation.platform.application.event;

/**
 * Published on self-service school signup (features/01-platform.md §1, POST /public/signup).
 * Consumed by iam (creates the unverified ADMIN_ECOLE account with the provided password) and
 * communication (sends the email-verification link). The tenant is PENDING_VERIFICATION until the
 * link is consumed.
 */
public record TenantSignupRequestedEvent(
        Long tenantId,
        String schoolName,
        String slug,
        String adminEmail,
        String director,
        String rawPassword,
        String verificationToken,
        String adminFirstName,
        String adminLastName,
        String adminPhone,
        /** Identifiant de connexion souhaité (username) ; si vide, généré depuis le nom. */
        String adminUsername,
        /** true = compte admin actif immédiatement (inscription sans vérification email). */
        boolean autoActivate
) {}
