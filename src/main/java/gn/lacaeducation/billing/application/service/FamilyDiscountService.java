package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.port.in.FamilyDiscountUseCase;
import gn.lacaeducation.billing.application.port.out.EnrollmentQueryPort;
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
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class FamilyDiscountService implements FamilyDiscountUseCase {

    private final FamilyDiscountRuleRepository repo;
    private final FamilyProvider familyProvider;
    private final EnrollmentQueryPort enrollmentQueryPort;

    @Override
    public FamilyDiscountRule create(Long tenantId, RuleCommand c) {
        validate(c);
        return repo.save(FamilyDiscountRule.builder()
                .tenantId(tenantId).label(c.label()).minChildren(c.minChildren()).maxChildren(c.maxChildren())
                .percentage(c.percentage()).feeTypes(c.feeTypes())
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
        r.setPercentage(c.percentage()); r.setFeeTypes(c.feeTypes());
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
                .filter(r -> r.getFeeTypes() == null || r.getFeeTypes().isEmpty() || r.getFeeTypes().contains(feeType))
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

    @Override
    @Transactional(readOnly = true)
    public List<Long> eligibleYoungest(Long tenantId, List<Long> studentIds) {
        List<FamilyDiscountRule> youngestRules = repo.findByTenantId(tenantId).stream()
                .filter(FamilyDiscountRule::isActive)
                .filter(FamilyDiscountRule::isAppliesToYoungest)
                .toList();
        if (youngestRules.isEmpty()) {
            return List.of();
        }
        return studentIds.stream()
                .filter(id -> {
                    List<ChildInfo> siblings = familyProvider.findSiblings(id);
                    int childCount = siblings.size();
                    boolean anyRuleMatches = youngestRules.stream().anyMatch(r ->
                            r.getMinChildren() != null && childCount >= r.getMinChildren()
                                    && (r.getMaxChildren() == null || childCount <= r.getMaxChildren()));
                    return anyRuleMatches && id.equals(youngestStudentId(siblings));
                })
                .toList();
    }

    /**
     * Le cadet = l'enfant inscrit dans le niveau le plus bas selon la progression pédagogique
     * officielle guinéenne (garderie → petite/moyenne/grande section → CP1…CM2 → 7e…10e →
     * 11e…terminale, cf. {@link NiveauCanonicalOrder}) — plus fiable que le seul {@code sort_order}
     * que chaque école saisit elle-même à la main et peut mal ordonner (ex. grande section classée
     * avant moyenne section, ce qui faisait alors ressortir DEUX cadets dans une même fratrie). En cas
     * d'égalité de niveau ou de niveau non reconnu, la date de naissance la plus récente départage.
     * Repli complet sur la date de naissance si aucun enfant de la fratrie n'a d'inscription active
     * (cas rare).
     */
    private Long youngestStudentId(List<ChildInfo> siblings) {
        record Ranked(Long studentId, Integer cycleOrder, Integer withinCycleOrder, java.time.LocalDate birthDate) {}
        List<Ranked> ranked = siblings.stream()
                .map(c -> {
                    EnrollmentQueryPort.ActiveLevel level = enrollmentQueryPort.findActiveLevel(c.studentId());
                    return new Ranked(c.studentId(),
                            NiveauCanonicalOrder.cycleRank(level == null ? null : level.cycle()),
                            NiveauCanonicalOrder.effectiveWithinCycleRank(level), c.birthDate());
                })
                .toList();
        boolean anyLevelKnown = ranked.stream().anyMatch(r -> r.cycleOrder() != null);
        if (anyLevelKnown) {
            Comparator<Ranked> byLevelThenLatestBirth = Comparator
                    .comparing(Ranked::cycleOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Ranked::withinCycleOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Ranked::birthDate, Comparator.nullsFirst(Comparator.<java.time.LocalDate>naturalOrder()).reversed());
            return ranked.stream().min(byLevelThenLatestBirth).map(Ranked::studentId).orElse(null);
        }
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
