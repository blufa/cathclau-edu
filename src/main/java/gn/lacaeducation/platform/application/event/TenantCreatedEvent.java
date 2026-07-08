package gn.lacaeducation.platform.application.event;

/**
 * Published when a tenant (school) is created. Consumed by iam (provision the ADMIN_ECOLE
 * user + default roles) and communication (welcome email). See features/01-platform.md §1.1.
 */
public record TenantCreatedEvent(
        Long tenantId,
        String schoolName,
        String adminEmail,
        String director,
        String adminPassword,
        /** true si le mot de passe est temporaire (généré) et doit être changé à la 1re connexion. */
        boolean temporaryPassword
) {}
