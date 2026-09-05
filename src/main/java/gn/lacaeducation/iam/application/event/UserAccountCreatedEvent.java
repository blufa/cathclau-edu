package gn.lacaeducation.iam.application.event;

/**
 * Publié à la création d'un compte utilisateur (personnel ou parent) muni d'un numéro de téléphone —
 * déclenche l'envoi de ses identifiants de connexion par SMS (§2.2). {@code plainPassword} n'est
 * disponible qu'en mémoire à cet instant (jamais persisté en clair), d'où sa présence directe ici.
 */
public record UserAccountCreatedEvent(Long tenantId, Long userId, String phone, String firstName,
                                      String lastName, String username, String plainPassword) {}
