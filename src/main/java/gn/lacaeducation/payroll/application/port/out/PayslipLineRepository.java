package gn.lacaeducation.payroll.application.port.out;

import gn.lacaeducation.payroll.domain.PayslipLine;

import java.util.List;
import java.util.Optional;

public interface PayslipLineRepository {
    PayslipLine save(PayslipLine l);
    Optional<PayslipLine> findById(Long id);
    List<PayslipLine> findByPayslipId(Long payslipId);
    void deleteById(Long id);
    void deleteByPayslipId(Long payslipId);
}
