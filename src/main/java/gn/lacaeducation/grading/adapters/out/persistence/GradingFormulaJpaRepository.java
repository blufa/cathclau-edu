package gn.lacaeducation.grading.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface GradingFormulaJpaRepository extends JpaRepository<GradingFormulaJpaEntity, Long> {
    List<GradingFormulaJpaEntity> findByTenantId(Long tenantId);
    Optional<GradingFormulaJpaEntity> findByTenantIdAndPeriodTypeAndActiveTrue(Long tenantId, String periodType);
}
