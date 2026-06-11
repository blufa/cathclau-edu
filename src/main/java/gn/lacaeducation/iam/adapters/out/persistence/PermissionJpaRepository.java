package gn.lacaeducation.iam.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface PermissionJpaRepository extends JpaRepository<PermissionJpaEntity, Long> {
}
