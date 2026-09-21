package gn.lacaeducation.platform.application.event;

/**
 * Publié lorsqu'un compte admin d'école est créé avec un <b>mot de passe temporaire généré</b>
 * (création manuelle par le SUPER_ADMIN sans mot de passe fourni). Consommé par communication pour
 * envoyer les identifiants de connexion à l'administrateur. Le mot de passe en clair ne transite qu'en
 * mémoire (événements non persistés) et n'est jamais stocké. Voir features/01-platform.md §1.1.
 */
public record TenantAdminCredentialsIssuedEvent(
        Long tenantId,
        String schoolName,
        String adminEmail,
        String adminPhone,
        String temporaryPassword
) {}
