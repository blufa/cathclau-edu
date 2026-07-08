package gn.lacaeducation.payroll.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface PayslipLineJpaRepository extends JpaRepository<PayslipLineJpaEntity, Long> {
    List<PayslipLineJpaEntity> findByPayslipId(Long payslipId);
    void deleteByPayslipId(Long payslipId);
}
