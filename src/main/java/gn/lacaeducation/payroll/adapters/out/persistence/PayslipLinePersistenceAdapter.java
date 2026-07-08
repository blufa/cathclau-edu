package gn.lacaeducation.payroll.adapters.out.persistence;

import gn.lacaeducation.payroll.adapters.out.persistence.mapper.PayrollMapper;
import gn.lacaeducation.payroll.application.port.out.PayslipLineRepository;
import gn.lacaeducation.payroll.domain.PayslipLine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class PayslipLinePersistenceAdapter implements PayslipLineRepository {
    private final PayslipLineJpaRepository jpa;
    private final PayrollMapper mapper;

    @Override public PayslipLine save(PayslipLine l) { return mapper.toDomain(jpa.save(mapper.toEntity(l))); }
    @Override public Optional<PayslipLine> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<PayslipLine> findByPayslipId(Long p) { return jpa.findByPayslipId(p).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
    @Override @Transactional public void deleteByPayslipId(Long p) { jpa.deleteByPayslipId(p); }
}
