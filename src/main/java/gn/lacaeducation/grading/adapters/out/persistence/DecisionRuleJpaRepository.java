package gn.lacaeducation.grading.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface DecisionRuleJpaRepository extends JpaRepository<DecisionRuleJpaEntity, Long> {
    List<DecisionRuleJpaEntity> findByTenantId(Long tenantId);
}
