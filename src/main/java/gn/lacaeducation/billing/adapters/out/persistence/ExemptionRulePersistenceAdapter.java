package gn.lacaeducation.billing.adapters.out.persistence;

import gn.lacaeducation.billing.application.port.out.ExemptionRuleRepository;
import gn.lacaeducation.billing.domain.ExemptionRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class ExemptionRulePersistenceAdapter implements ExemptionRuleRepository {

    private final ExemptionRuleJpaRepository jpa;

    @Override
    public ExemptionRule save(ExemptionRule r) {
        return toDomain(jpa.save(toEntity(r)));
    }

    @Override
    public Optional<ExemptionRule> findById(Long id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<ExemptionRule> findByTenantId(Long tenantId) {
        return jpa.findByTenantId(tenantId).stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    private ExemptionRule toDomain(ExemptionRuleJpaEntity e) {
        return ExemptionRule.builder()
                .id(e.getId()).tenantId(e.getTenantId()).label(e.getLabel()).type(e.getType())
                .percentage(e.getPercentage()).amount(e.getAmount()).feeTypes(e.getFeeTypes())
                .staffBenefit(e.isStaffBenefit()).active(e.isActive())
                .build();
    }

    private ExemptionRuleJpaEntity toEntity(ExemptionRule d) {
        ExemptionRuleJpaEntity e = new ExemptionRuleJpaEntity();
        e.setId(d.getId()); e.setTenantId(d.getTenantId()); e.setLabel(d.getLabel()); e.setType(d.getType());
        e.setPercentage(d.getPercentage()); e.setAmount(d.getAmount()); e.setFeeTypes(d.getFeeTypes());
        e.setStaffBenefit(d.isStaffBenefit()); e.setActive(d.isActive());
        return e;
    }
}
