package gn.lacaeducation.payroll.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.payroll.application.port.in.SalaryFeeDeductionUseCase;
import gn.lacaeducation.payroll.application.port.out.SalaryFeeDeductionRuleRepository;
import gn.lacaeducation.payroll.domain.SalaryFeeDeductionRule;
import gn.lacaeducation.staff.TeacherPublicInfoProvider;
import gn.lacaeducation.students.ParentAccessProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class SalaryFeeDeductionService implements SalaryFeeDeductionUseCase {

    private final SalaryFeeDeductionRuleRepository repo;
    private final TeacherPublicInfoProvider teacherProvider;
    private final ParentAccessProvider parentAccessProvider;

    @Override
    public SalaryFeeDeductionRule create(Long tenantId, RuleCommand c) {
        validate(c);
        return repo.save(SalaryFeeDeductionRule.builder()
                .tenantId(tenantId).label(c.label()).amountPerChild(c.amountPerChild())
                .maxChildren(c.maxChildren()).cap(c.cap())
                .active(c.active() == null || c.active())
                .build());
    }

    @Override
    public SalaryFeeDeductionRule update(Long id, RuleCommand c) {
        validate(c);
        SalaryFeeDeductionRule r = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalaryFeeDeductionRule", id));
        r.setLabel(c.label()); r.setAmountPerChild(c.amountPerChild());
        r.setMaxChildren(c.maxChildren()); r.setCap(c.cap());
        if (c.active() != null) r.setActive(c.active());
        return repo.save(r);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalaryFeeDeductionRule> findByTenant(Long tenantId) {
        return repo.findByTenantId(tenantId);
    }

    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public FeeDeduction computeFeeDeduction(Long tenantId, Long teacherId) {
        Long userId = teacherProvider.findUserIdByTeacherId(teacherId).orElse(null);
        if (userId == null) {
            return new FeeDeduction(BigDecimal.ZERO, null, 0);
        }
        // Ne compte que les enfants effectivement scolarisés (inscription VALIDÉE) : un enfant parti
        // (abandon/annulation) ou jamais formellement inscrit ne doit pas continuer à faire retenir de
        // l'argent sur le salaire indéfiniment.
        int childCount = parentAccessProvider.findChildStudentIdsWithValidatedEnrollment(userId).size();
        if (childCount == 0) {
            return new FeeDeduction(BigDecimal.ZERO, null, 0);
        }
        SalaryFeeDeductionRule rule = repo.findByTenantId(tenantId).stream()
                .filter(SalaryFeeDeductionRule::isActive)
                .findFirst()
                .orElse(null);
        if (rule == null) {
            return new FeeDeduction(BigDecimal.ZERO, null, 0);
        }
        int concerned = rule.getMaxChildren() != null ? Math.min(childCount, rule.getMaxChildren()) : childCount;
        BigDecimal amount = rule.getAmountPerChild().multiply(BigDecimal.valueOf(concerned));
        if (rule.getCap() != null && amount.compareTo(rule.getCap()) > 0) {
            amount = rule.getCap();
        }
        return new FeeDeduction(amount, rule.getLabel(), concerned);
    }

    private void validate(RuleCommand c) {
        if (c.label() == null || c.label().isBlank()) {
            throw new BusinessRuleException("Le libellé de la règle est requis.");
        }
        if (c.amountPerChild() == null || c.amountPerChild().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Le montant par enfant doit être ≥ 0.");
        }
        if (c.maxChildren() != null && c.maxChildren() < 1) {
            throw new BusinessRuleException("Le nombre maximum d'enfants doit être ≥ 1.");
        }
    }
}
