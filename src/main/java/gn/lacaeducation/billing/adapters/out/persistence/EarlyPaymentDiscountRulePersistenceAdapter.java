package gn.lacaeducation.billing.adapters.out.persistence;

import gn.lacaeducation.billing.application.port.out.EarlyPaymentDiscountRuleRepository;
import gn.lacaeducation.billing.domain.EarlyPaymentDiscountRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class EarlyPaymentDiscountRulePersistenceAdapter implements EarlyPaymentDiscountRuleRepository {

    private final EarlyPaymentDiscountRuleJpaRepository jpa;

    @Override
    public EarlyPaymentDiscountRule save(EarlyPaymentDiscountRule r) {
        return toDomain(jpa.save(toEntity(r)));
    }

    @Override
    public Optional<EarlyPaymentDiscountRule> findById(Long id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<EarlyPaymentDiscountRule> findByTenantId(Long tenantId) {
        return jpa.findByTenantId(tenantId).stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    private EarlyPaymentDiscountRule toDomain(EarlyPaymentDiscountRuleJpaEntity e) {
        return EarlyPaymentDiscountRule.builder()
                .id(e.getId()).tenantId(e.getTenantId()).label(e.getLabel())
                .percentage(e.getPercentage()).feeType(e.getFeeType()).active(e.isActive())
                .build();
    }

    private EarlyPaymentDiscountRuleJpaEntity toEntity(EarlyPaymentDiscountRule d) {
        EarlyPaymentDiscountRuleJpaEntity e = new EarlyPaymentDiscountRuleJpaEntity();
        e.setId(d.getId()); e.setTenantId(d.getTenantId()); e.setLabel(d.getLabel());
        e.setPercentage(d.getPercentage()); e.setFeeType(d.getFeeType()); e.setActive(d.isActive());
        return e;
    }
}
