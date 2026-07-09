package gn.lacaeducation.billing.adapters.out.persistence;

import gn.lacaeducation.billing.application.port.out.FamilyDiscountRuleRepository;
import gn.lacaeducation.billing.domain.FamilyDiscountRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class FamilyDiscountRulePersistenceAdapter implements FamilyDiscountRuleRepository {

    private final FamilyDiscountRuleJpaRepository jpa;

    @Override
    public FamilyDiscountRule save(FamilyDiscountRule r) {
        return toDomain(jpa.save(toEntity(r)));
    }

    @Override
    public Optional<FamilyDiscountRule> findById(Long id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<FamilyDiscountRule> findByTenantId(Long tenantId) {
        return jpa.findByTenantId(tenantId).stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    private FamilyDiscountRule toDomain(FamilyDiscountRuleJpaEntity e) {
        return FamilyDiscountRule.builder()
                .id(e.getId()).tenantId(e.getTenantId()).label(e.getLabel())
                .minChildren(e.getMinChildren()).maxChildren(e.getMaxChildren())
                .percentage(e.getPercentage()).feeType(e.getFeeType())
                .appliesToYoungest(e.isAppliesToYoungest()).active(e.isActive())
                .build();
    }

    private FamilyDiscountRuleJpaEntity toEntity(FamilyDiscountRule d) {
        FamilyDiscountRuleJpaEntity e = new FamilyDiscountRuleJpaEntity();
        e.setId(d.getId()); e.setTenantId(d.getTenantId()); e.setLabel(d.getLabel());
        e.setMinChildren(d.getMinChildren()); e.setMaxChildren(d.getMaxChildren());
        e.setPercentage(d.getPercentage()); e.setFeeType(d.getFeeType());
        e.setAppliesToYoungest(d.isAppliesToYoungest()); e.setActive(d.isActive());
        return e;
    }
}
