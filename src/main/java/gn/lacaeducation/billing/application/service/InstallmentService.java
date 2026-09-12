package gn.lacaeducation.billing.application.service;
import gn.lacaeducation.billing.application.port.in.InstallmentUseCase;
import gn.lacaeducation.billing.application.port.out.InstallmentRepository;
import gn.lacaeducation.billing.application.port.out.SchoolFeeRepository;
import gn.lacaeducation.billing.domain.Installment;
import gn.lacaeducation.billing.domain.InstallmentStatus;
import gn.lacaeducation.billing.domain.SchoolFee;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.common.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
@Service @Transactional @RequiredArgsConstructor
public class InstallmentService implements InstallmentUseCase {
    private final InstallmentRepository repo;
    private final SchoolFeeRepository schoolFeeRepository;
    private final OverdueInstallmentProcessor overdueInstallmentProcessor;

    @Override
    public void checkOverdueNow() {
        overdueInstallmentProcessor.checkOverdueDeadlinesForTenant(TenantContext.get(), java.time.LocalDate.now());
    }

    @Override public Installment create(CreateInstallmentCommand c) {
        validateInstallmentSum(c.feeId(), null, c.amount());
        return repo.save(Installment.builder()
                .feeId(c.feeId()).label(c.label()).amount(c.amount())
                .dueDate(c.dueDate()).status(InstallmentStatus.PENDING).months(c.months()).build());
    }

    @Override public List<Installment> generateTerms(GenerateTermsCommand c) {
        SchoolFee fee = schoolFeeRepository.findById(c.feeId())
                .orElseThrow(() -> new ResourceNotFoundException("SchoolFee", c.feeId()));
        java.math.BigDecimal total = c.annualTotal() != null && c.annualTotal().signum() > 0
                ? c.annualTotal() : fee.getAmount();
        if (total == null || total.signum() <= 0) {
            throw new BusinessRuleException("Le total annuel doit être positif pour la répartition trimestrielle");
        }
        if (c.terms() == null || c.terms().isEmpty()) {
            throw new BusinessRuleException("Au moins une période est requise");
        }
        java.util.List<Installment> created = new java.util.ArrayList<>();
        for (TermSpec t : c.terms()) {
            java.math.BigDecimal pct = t.percentage() != null ? t.percentage() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal amount = total.multiply(pct)
                    .divide(java.math.BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            created.add(repo.save(Installment.builder()
                    .feeId(c.feeId()).label(t.label()).amount(amount).dueDate(t.dueDate())
                    .status(InstallmentStatus.PENDING).build()));
        }
        return created;
    }

    @Override public Installment update(Long id, UpdateInstallmentCommand c) {
        Installment i = load(id);
        validateInstallmentSum(i.getFeeId(), id, c.amount());
        i.setLabel(c.label()); i.setAmount(c.amount()); i.setDueDate(c.dueDate()); i.setMonths(c.months());
        return repo.save(i);
    }

    @Override
    @Transactional(readOnly=true)
    public Optional<Installment> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    @Transactional(readOnly=true)
    public List<Installment> findByFeeId(Long feeId) {
        return repo.findByFeeId(feeId);
    }

    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }

    /**
     * Validates that adding/updating an installment amount does not exceed the fee total.
     * @param feeId the fee to validate against
     * @param excludeInstallmentId the installment being updated (null for new installments)
     * @param newAmount the new amount to validate
     */
    private void validateInstallmentSum(Long feeId, Long excludeInstallmentId, BigDecimal newAmount) {
        SchoolFee fee = schoolFeeRepository.findById(feeId)
                .orElseThrow(() -> new ResourceNotFoundException("SchoolFee", feeId));

        // Pour une MENSUALITE, chaque échéance est un mois autonome (montant complet) : pas de plafond
        // « somme = total » — le total annuel = montant mensuel × nombre de mois.
        if (fee.getType() == gn.lacaeducation.billing.domain.FeeType.MONTHLY) {
            return;
        }

        BigDecimal existingSum = repo.findByFeeId(feeId).stream()
                .filter(i -> excludeInstallmentId == null || !excludeInstallmentId.equals(i.getId()))
                .map(Installment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal newTotal = existingSum.add(newAmount);
        if (newTotal.compareTo(fee.getAmount()) > 0) {
            throw new BusinessRuleException(
                    "Installments sum " + newTotal + " exceeds fee total amount " + fee.getAmount()
                            + " for fee id=" + feeId);
        }
    }

    private Installment load(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Installment", id));
    }
}
