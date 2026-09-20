package gn.lacaeducation.iam.application.event;

/**
 * Publié quand un utilisateur demande la réinitialisation de son mot de passe par code OTP (SMS) —
 * alternative à {@link PasswordResetRequestedEvent} (lien email) pour les utilisateurs sans email.
 * Déclenche l'envoi du code par SMS ; sans lui, le code créé en base n'est jamais communiqué.
 */
public record PasswordResetOtpRequestedEvent(Long tenantId, Long userId, String phone, String code) {}
