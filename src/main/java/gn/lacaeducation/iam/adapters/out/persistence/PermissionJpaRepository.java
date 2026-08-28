package gn.lacaeducation.iam.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

interface PermissionJpaRepository extends JpaRepository<PermissionJpaEntity, Long> {

    @Query("""
            SELECT DISTINCT p FROM PermissionJpaEntity p
            WHERE p.id IN (
                SELECT rp.permissionId FROM RolePermissionJpaEntity rp
                WHERE rp.roleId IN (
                    SELECT ur.roleId FROM UserRoleJpaEntity ur WHERE ur.userId = :userId
                )
            )
            OR p.id IN (
                SELECT up.permissionId FROM UserPermissionJpaEntity up WHERE up.userId = :userId
            )
            """)
    List<PermissionJpaEntity> findByUserId(Long userId);
}
