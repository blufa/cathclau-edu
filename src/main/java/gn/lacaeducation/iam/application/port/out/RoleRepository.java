package gn.lacaeducation.iam.application.port.out;

import gn.lacaeducation.iam.domain.Role;
import gn.lacaeducation.iam.domain.RoleCode;

import java.util.List;
import java.util.Optional;

public interface RoleRepository {
    Optional<Role> findById(Long id);
    Optional<Role> findByCode(RoleCode code);
    List<Role> findUserRoles(Long userId);
    List<Role> findAll();
    Role save(Role role);
    void deleteById(Long id);
    boolean existsById(Long id);
    void assignPermissions(Long roleId, List<Long> permissionIds);
    void removeRoleFromUser(Long userId, Long roleId);
}
