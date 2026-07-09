package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.port.in.FamilyDiscountUseCase;
import gn.lacaeducation.billing.application.port.out.FamilyDiscountRuleRepository;
import gn.lacaeducation.billing.domain.FamilyDiscountRule;
import gn.lacaeducation.billing.domain.FeeType;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.students.FamilyProvider;
import gn.lacaeducation.students.FamilyProvider.ChildInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class FamilyDiscountService implements FamilyDiscountUseCase {

    private final FamilyDiscountRuleRepository repo;
    private final FamilyProvider familyProvider;

    @Override
    public FamilyDiscountRule create(Long tenantId, RuleCommand c) {
        validate(c);
        return repo.save(FamilyDiscountRule.builder()
                .tenantId(tenantId).label(c.label()).minChildren(c.minChildren()).maxChildren(c.maxChildren())
                .percentage(c.percentage()).feeType(c.feeType())
                .appliesToYoungest(c.appliesToYoungest() == null || c.appliesToYoungest())
                .active(c.active() == null || c.active())
                .build());
    }

    @Override
    public FamilyDiscountRule update(Long id, RuleCommand c) {
        validate(c);
        FamilyDiscountRule r = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FamilyDiscountRule", id));
        r.setLabel(c.label()); r.setMinChildren(c.minChildren()); r.setMaxChildren(c.maxChildren());
        r.setPercentage(c.percentage()); r.setFeeType(c.feeType());
        if (c.appliesToYoungest() != null) r.setAppliesToYoungest(c.appliesToYoungest());
        if (c.active() != null) r.setActive(c.active());
        return repo.save(r);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FamilyDiscountRule> findByTenant(Long tenantId) {
        return repo.findByTenantId(tenantId);
    }

    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicableDiscount computeApplicable(Long tenantId, Long studentId, FeeType feeType) {
        List<ChildInfo> siblings = familyProvider.findSiblings(studentId);
        int childCount = siblings.size();

        // La règle la plus spécifique applicable = celle qui exige le plus d'enfants parmi les éligibles.
        FamilyDiscountRule rule = repo.findByTenantId(tenantId).stream()
                .filter(FamilyDiscountRule::isActive)
                .filter(r -> r.getMinChildren() != null && childCount >= r.getMinChildren())
                .filter(r -> r.getMaxChildren() == null || childCount <= r.getMaxChildren())
                .filter(r -> r.getFeeType() == null || r.getFeeType() == feeType)
                .max(Comparator.comparingInt(FamilyDiscountRule::getMinChildren))
                .orElse(null);

        if (rule == null) {
            return new ApplicableDiscount(BigDecimal.ZERO, null);
        }
        // Si la règle ne vise que le plus jeune, l'élève doit être le plus jeune de la famille.
        if (rule.isAppliesToYoungest() && !studentId.equals(youngestStudentId(siblings))) {
            return new ApplicableDiscount(BigDecimal.ZERO, null);
        }
        return new ApplicableDiscount(rule.getPercentage(), rule.getLabel());
    }

    /** Le plus jeune = la date de naissance la plus récente (les dates nulles sont ignorées). */
    private Long youngestStudentId(List<ChildInfo> siblings) {
        return siblings.stream()
                .filter(c -> c.birthDate() != null)
                .max(Comparator.comparing(ChildInfo::birthDate))
                .map(ChildInfo::studentId)
                .orElse(null);
    }

    private void validate(RuleCommand c) {
        if (c.label() == null || c.label().isBlank()) {
            throw new BusinessRuleException("Le libellé de la règle est requis.");
        }
        if (c.minChildren() == null || c.minChildren() < 1) {
            throw new BusinessRuleException("Le nombre minimum d'enfants doit être ≥ 1.");
        }
        if (c.maxChildren() != null && c.maxChildren() < c.minChildren()) {
            throw new BusinessRuleException("Le maximum d'enfants doit être ≥ au minimum.");
        }
        if (c.percentage() == null || c.percentage().compareTo(BigDecimal.ZERO) < 0
                || c.percentage().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessRuleException("Le pourcentage doit être compris entre 0 et 100.");
        }
    }
}
