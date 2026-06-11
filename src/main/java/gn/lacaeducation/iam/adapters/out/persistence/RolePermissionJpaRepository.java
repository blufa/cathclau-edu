package gn.lacaeducation.iam.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface RolePermissionJpaRepository extends JpaRepository<RolePermissionJpaEntity, Long> {
}
