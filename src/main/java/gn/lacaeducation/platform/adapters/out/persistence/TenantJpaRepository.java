package gn.lacaeducation.platform.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface TenantJpaRepository extends JpaRepository<TenantJpaEntity, Long> {
    Optional<TenantJpaEntity> findByEmail(String email);
    boolean existsByEmail(String email);
}
