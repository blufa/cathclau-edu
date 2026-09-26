package gn.lacaeducation.billing.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ExemptionRuleJpaRepository extends JpaRepository<ExemptionRuleJpaEntity, Long> {
    List<ExemptionRuleJpaEntity> findByTenantId(Long tenantId);
}
