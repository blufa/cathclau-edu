package gn.lacaeducation.payroll.application.port.out;

import gn.lacaeducation.payroll.domain.SalaryRule;

import java.util.List;
import java.util.Optional;

public interface SalaryRuleRepository {
    SalaryRule save(SalaryRule r);
    Optional<SalaryRule> findById(Long id);
    List<SalaryRule> findByStructureIdOrderBySequenceAsc(Long structureId);
    void deleteById(Long id);
}
