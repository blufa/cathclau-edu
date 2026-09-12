package gn.lacaeducation.payroll.adapters.out.persistence;

import gn.lacaeducation.payroll.adapters.out.persistence.mapper.PayrollMapper;
import gn.lacaeducation.payroll.application.port.out.PayslipRepository;
import gn.lacaeducation.payroll.domain.Payslip;
import gn.lacaeducation.payroll.domain.PayslipStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class PayslipPersistenceAdapter implements PayslipRepository {
    private final PayslipJpaRepository jpa;
    private final PayrollMapper mapper;

    @Override public Payslip save(Payslip p) { return mapper.toDomain(jpa.save(mapper.toEntity(p))); }
    @Override public Optional<Payslip> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<Payslip> findByTenantId(Long t) { return jpa.findByTenantId(t).stream().map(mapper::toDomain).toList(); }
    @Override public List<Payslip> findByTenantIdAndTeacherId(Long t, Long te) { return jpa.findByTenantIdAndTeacherId(t, te).stream().map(mapper::toDomain).toList(); }
    @Override public List<Payslip> findByTenantIdAndStatus(Long t, PayslipStatus s) { return jpa.findByTenantIdAndStatus(t, s).stream().map(mapper::toDomain).toList(); }
    @Override public boolean existsByTenantIdAndTeacherIdAndPeriodLabelAndEmployeeType(
            Long t, Long te, String p, gn.lacaeducation.payroll.domain.EmployeeType type) {
        return jpa.existsByTenantIdAndTeacherIdAndPeriodLabelAndEmployeeType(t, te, p, type);
    }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
