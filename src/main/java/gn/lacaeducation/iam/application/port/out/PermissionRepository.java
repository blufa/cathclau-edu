package gn.lacaeducation.iam.application.port.out;

import gn.lacaeducation.iam.domain.Permission;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository {
    List<Permission> findAll();
    Optional<Permission> findById(Long id);
    List<Permission> findByRoleId(Long roleId);
    /** Toutes les permissions effectives d'un utilisateur (héritées de ses rôles + octroyées directement). */
    List<Permission> findByUserId(Long userId);
    /** Uniquement les permissions octroyées directement à l'utilisateur (hors héritage de rôle). */
    List<Permission> findDirectByUserId(Long userId);
    /** Remplace les permissions octroyées directement à l'utilisateur (n'affecte pas celles du rôle). */
    void assignDirectPermissions(Long userId, List<Long> permissionIds);
}
