package gn.lacaeducation.payroll.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface SalaryStructureJpaRepository extends JpaRepository<SalaryStructureJpaEntity, Long> {
    List<SalaryStructureJpaEntity> findByTenantId(Long tenantId);
}
