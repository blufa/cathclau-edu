package gn.lacaeducation.grants.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.grants.application.port.out.GrantPaymentRepository;
import gn.lacaeducation.grants.application.port.out.GrantRepository;
import gn.lacaeducation.grants.domain.Grant;
import gn.lacaeducation.grants.domain.GrantPayment;
import gn.lacaeducation.grants.domain.GrantStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Subventions scolaires (Manuel §4.3) : recettes hors frais parents, avec suivi des versements reçus.
 * Le statut est dérivé du montant reçu vs le montant total attendu.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class GrantService {

    private final GrantRepository grantRepo;
    private final GrantPaymentRepository paymentRepo;

    public record GrantCommand(Long tenantId, String source, String reference, String purpose,
                               String academicYear, BigDecimal totalAmount, String notes) {}
    public record PaymentCommand(LocalDate paymentDate, BigDecimal amount, String method, String reference,
                                 Long treasuryAccountId, String notes) {}

    public Grant create(GrantCommand c) {
        if (c.source() == null || c.source().isBlank()) throw new BusinessRuleException("La source de la subvention est requise");
        return grantRepo.save(Grant.builder()
                .tenantId(c.tenantId()).source(c.source()).reference(c.reference()).purpose(c.purpose())
                .academicYear(c.academicYear()).totalAmount(nz(c.totalAmount())).receivedAmount(BigDecimal.ZERO)
                .status(GrantStatus.EXPECTED).notes(c.notes()).createdAt(LocalDateTime.now()).build());
    }

    public Grant update(Long id, GrantCommand c) {
        Grant g = load(id);
        g.setSource(c.source()); g.setReference(c.reference()); g.setPurpose(c.purpose());
        g.setAcademicYear(c.academicYear()); g.setTotalAmount(nz(c.totalAmount())); g.setNotes(c.notes());
        recomputeStatus(g);
        return grantRepo.save(g);
    }

    public Grant cancel(Long id) {
        Grant g = load(id);
        g.setStatus(GrantStatus.CANCELLED);
        return grantRepo.save(g);
    }

    public void delete(Long id) {
        paymentRepo.deleteByGrantId(id);
        grantRepo.deleteById(id);
    }

    public GrantPayment addPayment(Long grantId, PaymentCommand c) {
        Grant g = load(grantId);
        if (c.amount() == null || c.amount().signum() <= 0) throw new BusinessRuleException("Le montant du versement doit être positif");
        GrantPayment p = paymentRepo.save(GrantPayment.builder()
                .tenantId(g.getTenantId()).grantId(grantId)
                .paymentDate(c.paymentDate() != null ? c.paymentDate() : LocalDate.now())
                .amount(c.amount()).method(c.method()).reference(c.reference())
                .treasuryAccountId(c.treasuryAccountId()).notes(c.notes()).createdAt(LocalDateTime.now()).build());
        g.setReceivedAmount(nz(g.getReceivedAmount()).add(c.amount()));
        recomputeStatus(g);
        grantRepo.save(g);
        return p;
    }

    public void deletePayment(Long paymentId) {
        GrantPayment p = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("GrantPayment", paymentId));
        paymentRepo.deleteById(paymentId);
        Grant g = load(p.getGrantId());
        g.setReceivedAmount(nz(g.getReceivedAmount()).subtract(p.getAmount()).max(BigDecimal.ZERO));
        recomputeStatus(g);
        grantRepo.save(g);
    }

    private void recomputeStatus(Grant g) {
        if (g.getStatus() == GrantStatus.CANCELLED) return;
        BigDecimal received = nz(g.getReceivedAmount());
        BigDecimal total = nz(g.getTotalAmount());
        if (received.signum() <= 0) g.setStatus(GrantStatus.EXPECTED);
        else if (received.compareTo(total) >= 0 && total.signum() > 0) g.setStatus(GrantStatus.RECEIVED);
        else g.setStatus(GrantStatus.PARTIAL);
    }

    @Transactional(readOnly = true)
    public List<Grant> list(Long tenantId) { return grantRepo.findByTenantId(tenantId); }

    @Transactional(readOnly = true)
    public Grant get(Long id) { return load(id); }

    @Transactional(readOnly = true)
    public List<GrantPayment> payments(Long grantId) { return paymentRepo.findByGrantId(grantId); }

    private static BigDecimal nz(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
    private Grant load(Long id) { return grantRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Grant", id)); }
}
