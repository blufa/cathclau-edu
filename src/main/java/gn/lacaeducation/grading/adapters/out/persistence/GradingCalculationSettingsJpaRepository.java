package gn.lacaeducation.grading.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

interface GradingCalculationSettingsJpaRepository extends JpaRepository<GradingCalculationSettingsJpaEntity, Long> {
    Optional<GradingCalculationSettingsJpaEntity> findByTenantId(Long tenantId);
}
