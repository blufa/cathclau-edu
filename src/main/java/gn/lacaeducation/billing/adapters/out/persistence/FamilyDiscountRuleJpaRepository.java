package gn.lacaeducation.billing.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface FamilyDiscountRuleJpaRepository extends JpaRepository<FamilyDiscountRuleJpaEntity, Long> {
    List<FamilyDiscountRuleJpaEntity> findByTenantId(Long tenantId);
}
