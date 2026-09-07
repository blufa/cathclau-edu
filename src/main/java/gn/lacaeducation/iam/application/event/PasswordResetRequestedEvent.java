package gn.lacaeducation.iam.application.event;

/**
 * Publié quand un utilisateur demande la réinitialisation de son mot de passe (« mot de passe
 * oublié ») — déclenche l'envoi du lien de réinitialisation par email. Sans ce lien, l'utilisateur
 * n'a aucun moyen de connaître le jeton créé en base (§2.1).
 */
public record PasswordResetRequestedEvent(Long tenantId, Long userId, String email, String resetToken) {}
