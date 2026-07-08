package gn.lacaeducation.billing.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface StaffChildBenefitPolicyJpaRepository extends JpaRepository<StaffChildBenefitPolicyJpaEntity, Long> {
    List<StaffChildBenefitPolicyJpaEntity> findByTenantId(Long tenantId);
}
