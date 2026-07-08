package gn.lacaeducation.grading.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface EliminatoryRuleJpaRepository extends JpaRepository<EliminatoryRuleJpaEntity, Long> {
    List<EliminatoryRuleJpaEntity> findByTenantId(Long tenantId);
}
