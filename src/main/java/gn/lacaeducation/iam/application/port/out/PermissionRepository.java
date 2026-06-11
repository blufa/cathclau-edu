package gn.lacaeducation.iam.application.port.out;

import gn.lacaeducation.iam.domain.Permission;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository {
    List<Permission> findAll();
    Optional<Permission> findById(Long id);
    List<Permission> findByRoleId(Long roleId);
    List<Permission> findByUserId(Long userId);
}
