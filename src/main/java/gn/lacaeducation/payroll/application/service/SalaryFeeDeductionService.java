package gn.lacaeducation.payroll.application.service;

import gn.lacaeducation.billing.StudentFeePaymentApi;
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
import java.math.RoundingMode;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class SalaryFeeDeductionService implements SalaryFeeDeductionUseCase {

    private final SalaryFeeDeductionRuleRepository repo;
    private final TeacherPublicInfoProvider teacherProvider;
    private final ParentAccessProvider parentAccessProvider;
    private final StudentFeePaymentApi studentFeePaymentApi;

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

    @Override
    public void applyDeductionAsInvoicePayment(Long tenantId, Long teacherId) {
        Long userId = teacherProvider.findUserIdByTeacherId(teacherId).orElse(null);
        if (userId == null) return;
        List<Long> childIds = parentAccessProvider.findChildStudentIdsWithValidatedEnrollment(userId);
        if (childIds.isEmpty()) return;
        SalaryFeeDeductionRule rule = repo.findByTenantId(tenantId).stream()
                .filter(SalaryFeeDeductionRule::isActive)
                .findFirst()
                .orElse(null);
        if (rule == null) return;
        int concerned = rule.getMaxChildren() != null ? Math.min(childIds.size(), rule.getMaxChildren()) : childIds.size();
        if (concerned == 0) return;
        BigDecimal perChild = rule.getAmountPerChild();
        BigDecimal total = perChild.multiply(BigDecimal.valueOf(concerned));
        if (rule.getCap() != null && total.compareTo(rule.getCap()) > 0) {
            // Plafond atteint : répartir le plafond également entre les enfants concernés, pour ne
            // jamais reverser sur les factures plus que ce qui a réellement été retenu sur le salaire.
            perChild = rule.getCap().divide(BigDecimal.valueOf(concerned), 2, RoundingMode.HALF_UP);
        }
        for (Long studentId : childIds.subList(0, concerned)) {
            studentFeePaymentApi.applyMonthlyPayment(tenantId, studentId, perChild);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public StaffBenefitRemainderDeduction computeStaffBenefitRemainder(Long tenantId, Long teacherId) {
        Long userId = teacherProvider.findUserIdByTeacherId(teacherId).orElse(null);
        if (userId == null) return new StaffBenefitRemainderDeduction(BigDecimal.ZERO, 0);
        BigDecimal total = BigDecimal.ZERO;
        int concerned = 0;
        for (Long studentId : parentAccessProvider.findChildStudentIdsWithValidatedEnrollment(userId)) {
            BigDecimal remainder = studentFeePaymentApi.outstandingStaffBenefitRemainder(studentId);
            if (remainder != null && remainder.signum() > 0) {
                total = total.add(remainder);
                concerned++;
            }
        }
        return new StaffBenefitRemainderDeduction(total, concerned);
    }

    @Override
    public void applyStaffBenefitRemainderAsInvoicePayment(Long tenantId, Long teacherId) {
        Long userId = teacherProvider.findUserIdByTeacherId(teacherId).orElse(null);
        if (userId == null) return;
        for (Long studentId : parentAccessProvider.findChildStudentIdsWithValidatedEnrollment(userId)) {
            BigDecimal remainder = studentFeePaymentApi.outstandingStaffBenefitRemainder(studentId);
            if (remainder != null && remainder.signum() > 0) {
                studentFeePaymentApi.applyMonthlyPayment(tenantId, studentId, remainder);
            }
        }
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
