package gn.lacaeducation.iam.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {
    Optional<UserJpaEntity> findByEmail(String email);
    Optional<UserJpaEntity> findByEmailAndTenantId(String email, Long tenantId);
    List<UserJpaEntity> findByTenantId(Long tenantId);
}
