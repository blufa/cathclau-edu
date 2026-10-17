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
        boolean temporaryPassword,
        /** Identifiant de connexion choisi pour l'admin (login par username OU email) ; généré si null. */
        String adminUsername,
        /** Téléphone PERSONNEL de l'administrateur (son identifiant de connexion) — distinct du
         * téléphone de l'école, qui reste sur {@code Tenant#phone}/{@code phone2} ; voir
         * {@code CreateTenantRequest#adminPhone}. */
        String adminPhone
) {}
