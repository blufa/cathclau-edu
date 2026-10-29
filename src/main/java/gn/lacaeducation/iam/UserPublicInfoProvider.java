package gn.lacaeducation.iam;

import java.util.List;
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

    /** Tous les utilisateurs d'un tenant (identité minimale) — pour un sélecteur "par utilisateur" qui
     * ne doit pas exiger le droit complet de gestion des comptes ({@code user:read}), ex. le filtre du
     * journal d'audit (§retour comptable : certains rôles ont {@code audit:read} sans {@code
     * user:read}). */
    List<UserPublicInfo> findUsersByTenant(Long tenantId);

    /** Identifiants des comptes actifs d'un tenant portant au moins un des rôles donnés (dédupliqués) —
     * pour notifier in-app (cloche) tout le personnel concerné par une alerte, ex. échéance de
     * paiement proche (§retour comptable). Codes de rôle en chaîne (ex. "ACCOUNTANT"), jamais
     * {@code RoleCode} (domaine interne du module iam, non exposé hors module). */
    List<Long> findUserIdsByRoles(Long tenantId, Set<String> roleCodes);
}
