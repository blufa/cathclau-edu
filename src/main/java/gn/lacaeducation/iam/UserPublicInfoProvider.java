package gn.lacaeducation.iam;

import java.util.Optional;

/**
 * API publique du module iam : identité minimale d'un utilisateur, pour les documents émis par
 * d'autres modules (ex. reçu de paiement — nom de l'agent ayant encaissé), sans exposer le domaine
 * utilisateur interne (mot de passe, rôles…).
 */
public interface UserPublicInfoProvider {

    record UserPublicInfo(Long id, String firstName, String lastName) {}

    Optional<UserPublicInfo> findUser(Long userId);
}
