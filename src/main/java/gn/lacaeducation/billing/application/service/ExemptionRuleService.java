package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.port.in.ExemptionRuleUseCase;
import gn.lacaeducation.billing.application.port.out.ExemptionRuleRepository;
import gn.lacaeducation.billing.domain.ExemptionRule;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ExemptionRuleService implements ExemptionRuleUseCase {

    private final ExemptionRuleRepository repo;

    @Override
    public ExemptionRule create(Long tenantId, RuleCommand c) {
        validate(c);
        return repo.save(ExemptionRule.builder()
                .tenantId(tenantId).label(c.label()).type(c.type())
                .percentage(c.percentage()).amount(c.amount()).feeTypes(c.feeTypes())
                .staffBenefit(c.staffBenefit() != null && c.staffBenefit())
                .active(c.active() == null || c.active())
                .build());
    }

    @Override
    public ExemptionRule update(Long id, RuleCommand c) {
        validate(c);
        ExemptionRule r = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExemptionRule", id));
        r.setLabel(c.label()); r.setType(c.type());
        r.setPercentage(c.percentage()); r.setAmount(c.amount()); r.setFeeTypes(c.feeTypes());
        if (c.staffBenefit() != null) r.setStaffBenefit(c.staffBenefit());
        if (c.active() != null) r.setActive(c.active());
        return repo.save(r);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExemptionRule> findByTenant(Long tenantId) {
        return repo.findByTenantId(tenantId);
    }

    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }

    private void validate(RuleCommand c) {
        if (c.label() == null || c.label().isBlank()) throw new BusinessRuleException("Le libellé est requis");
        if (c.type() == null) throw new BusinessRuleException("Le type d'exonération est requis");
    }
}
