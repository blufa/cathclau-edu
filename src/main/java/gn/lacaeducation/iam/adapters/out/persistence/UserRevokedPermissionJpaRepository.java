package gn.lacaeducation.iam.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface UserRevokedPermissionJpaRepository extends JpaRepository<UserRevokedPermissionJpaEntity, Long> {
    List<UserRevokedPermissionJpaEntity> findByUserId(Long userId);
}
