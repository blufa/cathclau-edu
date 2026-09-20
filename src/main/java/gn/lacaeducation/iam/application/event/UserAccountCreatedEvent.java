package gn.lacaeducation.iam.application.event;

/**
 * Publié à la création d'un compte utilisateur (personnel ou parent) muni d'un numéro de téléphone
 * et/ou d'un email — déclenche l'envoi de ses identifiants de connexion, par email s'il en a un
 * (canal préféré : message plus complet, sans limite de caractères), sinon par SMS (§2.2). La
 * connexion elle-même reste toujours possible par numéro de téléphone quel que soit le canal
 * d'envoi des identifiants. {@code plainPassword} n'est disponible qu'en mémoire à cet instant
 * (jamais persisté en clair), d'où sa présence directe ici.
 */
public record UserAccountCreatedEvent(Long tenantId, Long userId, String phone, String email,
                                      String firstName, String lastName, String username,
                                      String plainPassword) {}
