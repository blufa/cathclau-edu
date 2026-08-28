package gn.lacaeducation.iam.application.port.in;

import gn.lacaeducation.iam.domain.Permission;
import gn.lacaeducation.iam.domain.Role;

import java.util.List;

public interface RoleUseCase {
    List<Role> findAll();
    Role findById(Long id);
    Role create(CreateRoleCommand command);
    Role update(Long id, UpdateRoleCommand command);
    void delete(Long id);
    void assignPermissions(Long roleId, List<Long> permissionIds);
    void removeRoleFromUser(Long userId, Long roleId);
    List<Permission> findAllPermissions();
    List<Permission> findPermissionsByRole(Long roleId);
    /** Permissions octroyées directement à l'utilisateur, en plus de celles héritées de son rôle. */
    List<Permission> findDirectPermissionsByUser(Long userId);
    /** Remplace les permissions octroyées directement à l'utilisateur (n'affecte pas celles du rôle). */
    void assignUserPermissions(Long userId, List<Long> permissionIds);

    record CreateRoleCommand(String name, String description) {}
    record UpdateRoleCommand(String name, String description) {}
}
