package gn.lacaeducation.iam;

import java.util.Optional;
import java.util.Set;

/**
 * API publique du module iam : identité minimale d'un utilisateur, pour les documents émis par
 * d'autres modules (ex. reçu de paiement — nom de l'agent ayant encaissé), sans exposer le domaine
 * utilisateur interne (mot de passe, rôles…).
 */
public interface UserPublicInfoProvider {

    record UserPublicInfo(Long id, String firstName, String lastName, String phone) {}

    Optional<UserPublicInfo> findUser(Long userId);

    /** Vrai si l'utilisateur porte au moins un des codes de rôle donnés (ex. {@code "SCHOOL_ADMIN"},
     * {@code "SUPER_ADMIN"}) — utilisé par d'autres modules pour exempter certains rôles de règles
     * métier qui ne concernent normalement que le personnel de guichet (ex. barrière caisse). */
    boolean hasAnyRole(Long userId, Set<String> roleCodes);
}
