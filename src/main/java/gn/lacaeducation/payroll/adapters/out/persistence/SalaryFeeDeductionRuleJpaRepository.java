package gn.lacaeducation.payroll.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface SalaryFeeDeductionRuleJpaRepository extends JpaRepository<SalaryFeeDeductionRuleJpaEntity, Long> {
    List<SalaryFeeDeductionRuleJpaEntity> findByTenantId(Long tenantId);
}
