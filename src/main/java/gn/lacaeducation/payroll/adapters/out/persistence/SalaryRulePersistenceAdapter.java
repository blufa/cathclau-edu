package gn.lacaeducation.payroll.adapters.out.persistence;

import gn.lacaeducation.payroll.adapters.out.persistence.mapper.PayrollMapper;
import gn.lacaeducation.payroll.application.port.out.SalaryRuleRepository;
import gn.lacaeducation.payroll.domain.SalaryRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class SalaryRulePersistenceAdapter implements SalaryRuleRepository {
    private final SalaryRuleJpaRepository jpa;
    private final PayrollMapper mapper;

    @Override public SalaryRule save(SalaryRule r) { return mapper.toDomain(jpa.save(mapper.toEntity(r))); }
    @Override public Optional<SalaryRule> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<SalaryRule> findByStructureIdOrderBySequenceAsc(Long s) { return jpa.findByStructureIdOrderBySequenceAsc(s).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
