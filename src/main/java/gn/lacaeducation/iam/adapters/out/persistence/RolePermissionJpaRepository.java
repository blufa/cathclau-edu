package gn.lacaeducation.iam.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface RolePermissionJpaRepository extends JpaRepository<RolePermissionJpaEntity, Long> {
    List<RolePermissionJpaEntity> findByRoleId(Long roleId);
    void deleteByRoleId(Long roleId);
}
