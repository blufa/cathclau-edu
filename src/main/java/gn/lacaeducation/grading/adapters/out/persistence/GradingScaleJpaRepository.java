package gn.lacaeducation.grading.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

interface GradingScaleJpaRepository extends JpaRepository<GradingScaleJpaEntity, Long> {
    Optional<GradingScaleJpaEntity> findByTenantId(Long tenantId);
}
