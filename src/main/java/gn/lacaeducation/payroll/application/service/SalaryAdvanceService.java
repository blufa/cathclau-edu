package gn.lacaeducation.payroll.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.payroll.application.port.in.SalaryAdvanceUseCase;
import gn.lacaeducation.payroll.application.port.out.PayslipRepository;
import gn.lacaeducation.payroll.application.port.out.SalaryAdvanceRepository;
import gn.lacaeducation.payroll.application.port.out.SalaryAdvanceVoucherPdfGenerator;
import gn.lacaeducation.payroll.application.port.out.TeacherCompensationRepository;
import gn.lacaeducation.payroll.domain.EmployeeType;
import gn.lacaeducation.payroll.domain.PayMode;
import gn.lacaeducation.payroll.domain.Payslip;
import gn.lacaeducation.payroll.domain.SalaryAdvance;
import gn.lacaeducation.payroll.domain.SalaryAdvanceStatus;
import gn.lacaeducation.staff.EmployeePublicInfoProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Avances sur salaire (§payroll) : octroi plafonné au salaire de référence du salarié, remboursement
 * automatique par retenue sur les fiches de paie suivantes jusqu'à épuisement du solde — voir
 * {@link #applyDeduction}, appelée depuis {@code PayrollService.generate}.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class SalaryAdvanceService implements SalaryAdvanceUseCase {

    private final SalaryAdvanceRepository repo;
    private final PayslipRepository payslipRepo;
    private final TeacherCompensationRepository compRepo;
    private final EmployeePublicInfoProvider employeeProvider;
    private final SalaryAdvanceVoucherPdfGenerator pdfGenerator;

    @Override
    public SalaryAdvance grant(GrantCommand c) {
        if (c.teacherId() == null) throw new BusinessRuleException("Le salarié est requis.");
        if (c.amount() == null || c.amount().signum() <= 0) {
            throw new BusinessRuleException("Le montant de l'avance doit être positif.");
        }
        EmployeeType type = c.employeeType() != null ? c.employeeType() : EmployeeType.TEACHER;

        boolean hasActive = repo.findByTenantIdAndTeacherIdAndEmployeeType(c.tenantId(), c.teacherId(), type).stream()
                .anyMatch(a -> a.getStatus() == SalaryAdvanceStatus.ACTIVE);
        if (hasActive) {
            throw new BusinessRuleException(
                    "Ce salarié a déjà une avance sur salaire en cours de remboursement — "
                            + "soldez-la avant d'en accorder une nouvelle.");
        }

        BigDecimal reference = referenceSalary(c.tenantId(), c.teacherId(), type);
        if (reference == null) {
            throw new BusinessRuleException(
                    "Aucune fiche de paie ni salaire de base connu pour ce salarié — "
                            + "impossible de vérifier le plafond de l'avance.");
        }
        if (c.amount().compareTo(reference) > 0) {
            throw new BusinessRuleException(
                    "L'avance demandée dépasse le salaire de référence de ce salarié (plafond : "
                            + reference.stripTrailingZeros().toPlainString() + " GNF).");
        }

        return repo.save(SalaryAdvance.builder()
                .tenantId(c.tenantId()).employeeType(type).teacherId(c.teacherId()).teacherName(c.teacherName())
                .amount(c.amount()).remainingBalance(c.amount()).reason(c.reason())
                .status(SalaryAdvanceStatus.ACTIVE).grantedAt(LocalDateTime.now()).build());
    }

    /** Salaire de référence pour le plafond : net de la dernière fiche de paie générée pour ce salarié
     * (fiable quel que soit le mode de paie) ; à défaut, salaire de base (personnel) ou salaire fixe
     * (enseignant en mode FIXE) ; {@code null} si aucune référence n'est trouvable. */
    private BigDecimal referenceSalary(Long tenantId, Long teacherId, EmployeeType type) {
        List<Payslip> slips = payslipRepo.findByTenantIdAndTeacherId(tenantId, teacherId).stream()
                .filter(p -> p.getEmployeeType() == type)
                .sorted(Comparator.comparing(Payslip::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder())).reversed())
                .toList();
        if (!slips.isEmpty()) {
            BigDecimal net = slips.get(0).getNetSalary();
            if (net != null && net.signum() > 0) return net;
        }
        if (type == EmployeeType.TEACHER) {
            return compRepo.findByTeacherId(teacherId)
                    .filter(c -> c.getPayMode() == PayMode.FIXED)
                    .map(gn.lacaeducation.payroll.domain.TeacherCompensation::getFixedSalary)
                    .filter(v -> v != null && v.signum() > 0)
                    .orElse(null);
        }
        return employeeProvider.findEmployee(teacherId)
                .map(EmployeePublicInfoProvider.EmployeeInfo::baseSalary)
                .filter(v -> v != null && v.signum() > 0)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalaryAdvance> findByTenant(Long tenantId) {
        return repo.findByTenantId(tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalaryAdvance> findByTeacherId(Long tenantId, Long teacherId, EmployeeType employeeType) {
        return repo.findByTenantIdAndTeacherIdAndEmployeeType(tenantId, teacherId, employeeType);
    }

    @Override
    public SalaryAdvance cancel(Long id) {
        SalaryAdvance a = load(id);
        if (a.getStatus() != SalaryAdvanceStatus.ACTIVE || a.getRemainingBalance() == null
                || a.getRemainingBalance().compareTo(a.getAmount()) != 0) {
            throw new BusinessRuleException(
                    "Cette avance a déjà été partiellement déduite ou n'est plus en cours — elle ne peut plus être annulée.");
        }
        a.setStatus(SalaryAdvanceStatus.CANCELLED);
        return repo.save(a);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] voucherPdf(Long id) {
        return pdfGenerator.generate(load(id));
    }

    @Override
    public AdvanceDeductionResult applyDeduction(Long tenantId, Long teacherId, EmployeeType employeeType, BigDecimal availableNet) {
        if (availableNet == null || availableNet.signum() <= 0) {
            return new AdvanceDeductionResult(BigDecimal.ZERO, null);
        }
        SalaryAdvance active = repo.findByTenantIdAndTeacherIdAndEmployeeType(tenantId, teacherId, employeeType).stream()
                .filter(a -> a.getStatus() == SalaryAdvanceStatus.ACTIVE)
                .findFirst().orElse(null);
        if (active == null || active.getRemainingBalance() == null || active.getRemainingBalance().signum() <= 0) {
            return new AdvanceDeductionResult(BigDecimal.ZERO, null);
        }
        BigDecimal deducted = active.getRemainingBalance().min(availableNet);
        active.setRemainingBalance(active.getRemainingBalance().subtract(deducted));
        active.setLastDeductedAt(LocalDateTime.now());
        if (active.getRemainingBalance().signum() <= 0) {
            active.setStatus(SalaryAdvanceStatus.REPAID);
        }
        repo.save(active);
        return new AdvanceDeductionResult(deducted, "Avance sur salaire");
    }

    private SalaryAdvance load(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("SalaryAdvance", id));
    }
}
