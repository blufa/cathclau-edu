package gn.lacaeducation.payroll.application.service;

import gn.lacaeducation.billing.StudentFeePaymentApi;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.payroll.application.port.in.SalaryFeeDeductionUseCase;
import gn.lacaeducation.payroll.application.port.out.SalaryFeeDeductionRuleRepository;
import gn.lacaeducation.payroll.domain.EmployeeType;
import gn.lacaeducation.payroll.domain.SalaryFeeDeductionRule;
import gn.lacaeducation.staff.EmployeePublicInfoProvider;
import gn.lacaeducation.staff.TeacherPublicInfoProvider;
import gn.lacaeducation.students.ParentAccessProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class SalaryFeeDeductionService implements SalaryFeeDeductionUseCase {

    private final SalaryFeeDeductionRuleRepository repo;
    private final TeacherPublicInfoProvider teacherProvider;
    private final EmployeePublicInfoProvider employeeProvider;
    private final ParentAccessProvider parentAccessProvider;
    private final StudentFeePaymentApi studentFeePaymentApi;

    /** Résout le compte utilisateur du salarié dans le bon référentiel — enseignant ou personnel
     * administratif/direction/support (§retour comptable : la retenue scolarité ne doit pas se limiter
     * aux enseignants). */
    private Long resolveUserId(EmployeeType employeeType, Long staffId) {
        if (employeeType == EmployeeType.TEACHER) {
            return teacherProvider.findUserIdByTeacherId(staffId).orElse(null);
        }
        return employeeProvider.findUserIdByEmployeeId(staffId).orElse(null);
    }

    /** Enfants scolarisés (inscription active, PENDING comprise — retour comptable : pas de refuge en
     * laissant l'inscription non validée) de ce salarié, qu'il ait un compte de connexion (lien par
     * {@code userId} partagé avec sa fiche Parent) OU non (lien direct {@code staffType}/{@code staffId},
     * personnel sans smartphone Android — voir ParentInput côté inscription). Les deux chemins sont
     * consultés et fusionnés : sans ça, un salarié inscrit "Personnel de l'école" sans compte ne
     * déclenchait jamais aucune retenue, même avec un enfant correctement rattaché. Exclut ensuite tout
     * enfant dont un parent a explicitement choisi de payer lui-même (voir
     * ParentAccessProvider#hasOptedOutOfStaffAutoDeduction). */
    private List<Long> resolveChildStudentIds(EmployeeType employeeType, Long staffId) {
        Long userId = resolveUserId(employeeType, staffId);
        java.util.LinkedHashSet<Long> ids = new java.util.LinkedHashSet<>();
        if (userId != null) {
            ids.addAll(parentAccessProvider.findChildStudentIdsEnrolled(userId));
        }
        String staffType = employeeType == EmployeeType.TEACHER ? "TEACHER" : "EMPLOYEE";
        ids.addAll(parentAccessProvider.findChildStudentIdsEnrolledByStaffLink(staffType, staffId));
        return ids.stream().filter(id -> !parentAccessProvider.hasOptedOutOfStaffAutoDeduction(id)).toList();
    }

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
    public FeeDeduction computeFeeDeduction(Long tenantId, EmployeeType employeeType, Long staffId) {
        // Ne compte que les enfants effectivement scolarisés (inscription VALIDÉE) : un enfant parti
        // (abandon/annulation) ou jamais formellement inscrit ne doit pas continuer à faire retenir de
        // l'argent sur le salaire indéfiniment.
        int childCount = resolveChildStudentIds(employeeType, staffId).size();
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
    public void applyDeductionAsInvoicePayment(Long tenantId, EmployeeType employeeType, Long staffId) {
        List<Long> childIds = resolveChildStudentIds(employeeType, staffId);
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
    public StaffBenefitRemainderDeduction computeStaffBenefitRemainder(Long tenantId, EmployeeType employeeType, Long staffId,
                                                                        LocalDate periodStart, String periodLabel) {
        BigDecimal total = BigDecimal.ZERO;
        int concerned = 0;
        for (Long studentId : resolveChildStudentIds(employeeType, staffId)) {
            BigDecimal remainder = studentFeePaymentApi.outstandingStaffBenefitRemainder(studentId, periodStart, periodLabel);
            if (remainder != null && remainder.signum() > 0) {
                total = total.add(remainder);
                concerned++;
            }
        }
        return new StaffBenefitRemainderDeduction(total, concerned);
    }

    @Override
    public void applyStaffBenefitRemainderAsInvoicePayment(Long tenantId, EmployeeType employeeType, Long staffId,
                                                            LocalDate periodStart, String periodLabel) {
        for (Long studentId : resolveChildStudentIds(employeeType, staffId)) {
            BigDecimal remainder = studentFeePaymentApi.outstandingStaffBenefitRemainder(studentId, periodStart, periodLabel);
            if (remainder != null && remainder.signum() > 0) {
                studentFeePaymentApi.applyStaffBenefitRemainderPayment(studentId, periodStart, periodLabel, remainder);
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
