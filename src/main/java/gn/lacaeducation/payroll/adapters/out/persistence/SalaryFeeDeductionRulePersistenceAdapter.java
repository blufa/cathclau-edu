package gn.lacaeducation.payroll.adapters.out.persistence;

import gn.lacaeducation.payroll.application.port.out.SalaryFeeDeductionRuleRepository;
import gn.lacaeducation.payroll.domain.SalaryFeeDeductionRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class SalaryFeeDeductionRulePersistenceAdapter implements SalaryFeeDeductionRuleRepository {

    private final SalaryFeeDeductionRuleJpaRepository jpa;

    @Override
    public SalaryFeeDeductionRule save(SalaryFeeDeductionRule r) {
        return toDomain(jpa.save(toEntity(r)));
    }

    @Override
    public Optional<SalaryFeeDeductionRule> findById(Long id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<SalaryFeeDeductionRule> findByTenantId(Long tenantId) {
        return jpa.findByTenantId(tenantId).stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    private SalaryFeeDeductionRule toDomain(SalaryFeeDeductionRuleJpaEntity e) {
        return SalaryFeeDeductionRule.builder()
                .id(e.getId()).tenantId(e.getTenantId()).label(e.getLabel())
                .amountPerChild(e.getAmountPerChild()).maxChildren(e.getMaxChildren())
                .cap(e.getCap()).active(e.isActive())
                .build();
    }

    private SalaryFeeDeductionRuleJpaEntity toEntity(SalaryFeeDeductionRule d) {
        SalaryFeeDeductionRuleJpaEntity e = new SalaryFeeDeductionRuleJpaEntity();
        e.setId(d.getId()); e.setTenantId(d.getTenantId()); e.setLabel(d.getLabel());
        e.setAmountPerChild(d.getAmountPerChild()); e.setMaxChildren(d.getMaxChildren());
        e.setCap(d.getCap()); e.setActive(d.isActive());
        return e;
    }
}
