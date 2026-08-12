package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.port.in.EarlyPaymentDiscountUseCase;
import gn.lacaeducation.billing.application.port.out.EarlyPaymentDiscountRuleRepository;
import gn.lacaeducation.billing.domain.EarlyPaymentDiscountRule;
import gn.lacaeducation.billing.domain.FeeType;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class EarlyPaymentDiscountService implements EarlyPaymentDiscountUseCase {

    private final EarlyPaymentDiscountRuleRepository repo;

    @Override
    public EarlyPaymentDiscountRule create(Long tenantId, RuleCommand c) {
        validate(c);
        return repo.save(EarlyPaymentDiscountRule.builder()
                .tenantId(tenantId).label(c.label()).percentage(c.percentage()).feeType(c.feeType())
                .active(c.active() == null || c.active())
                .build());
    }

    @Override
    public EarlyPaymentDiscountRule update(Long id, RuleCommand c) {
        validate(c);
        EarlyPaymentDiscountRule r = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EarlyPaymentDiscountRule", id));
        r.setLabel(c.label()); r.setPercentage(c.percentage()); r.setFeeType(c.feeType());
        if (c.active() != null) r.setActive(c.active());
        return repo.save(r);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EarlyPaymentDiscountRule> findByTenant(Long tenantId) {
        return repo.findByTenantId(tenantId);
    }

    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicableDiscount computeApplicable(Long tenantId, FeeType feeType) {
        // La règle la plus spécifique l'emporte : un type de frais précis avant une règle générale (null).
        EarlyPaymentDiscountRule rule = repo.findByTenantId(tenantId).stream()
                .filter(EarlyPaymentDiscountRule::isActive)
                .filter(r -> r.getFeeType() == null || r.getFeeType() == feeType)
                .max(Comparator.comparing(r -> r.getFeeType() != null))
                .orElse(null);
        if (rule == null) {
            return new ApplicableDiscount(BigDecimal.ZERO, null);
        }
        return new ApplicableDiscount(rule.getPercentage(), rule.getLabel());
    }

    private void validate(RuleCommand c) {
        if (c.label() == null || c.label().isBlank()) {
            throw new BusinessRuleException("Le libellé de la règle est requis.");
        }
        if (c.percentage() == null || c.percentage().compareTo(BigDecimal.ZERO) < 0
                || c.percentage().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessRuleException("Le pourcentage doit être compris entre 0 et 100.");
        }
    }
}
