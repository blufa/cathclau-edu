package gn.lacaeducation.billing.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface EarlyPaymentDiscountRuleJpaRepository extends JpaRepository<EarlyPaymentDiscountRuleJpaEntity, Long> {
    List<EarlyPaymentDiscountRuleJpaEntity> findByTenantId(Long tenantId);
}
