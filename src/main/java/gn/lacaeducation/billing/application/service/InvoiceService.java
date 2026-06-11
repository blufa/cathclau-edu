package gn.lacaeducation.billing.application.service;
import gn.lacaeducation.billing.application.event.InvoiceGeneratedEvent;
import gn.lacaeducation.billing.application.port.in.InvoiceUseCase;
import gn.lacaeducation.billing.application.port.out.InvoiceRepository;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.InvoiceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
@Service @Transactional @RequiredArgsConstructor
public class InvoiceService implements InvoiceUseCase {
    private final InvoiceRepository repo;
    private final ApplicationEventPublisher eventPublisher;
    @Override public Invoice create(CreateInvoiceCommand c) {
        Invoice inv = repo.save(Invoice.builder().tenantId(c.tenantId()).studentId(c.studentId()).feeId(c.feeId())
                .reference(c.reference()).totalAmount(c.totalAmount()).paidAmount(BigDecimal.ZERO)
                .remainingBalance(c.totalAmount()).status(InvoiceStatus.UNPAID).issueDate(LocalDate.now()).build());
        eventPublisher.publishEvent(new InvoiceGeneratedEvent(c.tenantId(), c.studentId(), inv.getId(), inv.getReference()));
        return inv;
    }
    @Override @Transactional(readOnly=true) public Optional<Invoice> findById(Long id) { return repo.findById(id); }
    @Override @Transactional(readOnly=true) public Optional<Invoice> findByReference(String ref) { return repo.findByReference(ref); }
    @Override @Transactional(readOnly=true) public List<Invoice> findByStudentId(Long s) { return repo.findByStudentId(s); }
    @Override @Transactional(readOnly=true) public List<Invoice> findByTenantId(Long t) { return repo.findByTenantId(t); }
    @Override @Transactional(readOnly=true) public List<Invoice> findByTenantIdAndStatus(Long t, InvoiceStatus s) { return repo.findByTenantIdAndStatus(t, s); }
    @Override public Invoice applyPayment(Long invoiceId, BigDecimal amount) {
        Invoice inv = load(invoiceId);
        inv.setPaidAmount(inv.getPaidAmount().add(amount));
        inv.setRemainingBalance(inv.getTotalAmount().subtract(inv.getPaidAmount()));
        inv.setStatus(inv.getRemainingBalance().compareTo(BigDecimal.ZERO) <= 0 ? InvoiceStatus.PAID : InvoiceStatus.PARTIAL);
        return repo.save(inv);
    }
    @Override public void cancel(Long id) { repo.deleteById(id); }
    @Override public void delete(Long id) { repo.deleteById(id); }
    @Override @Transactional(readOnly=true) public boolean existsById(Long id) { return repo.existsById(id); }
    private Invoice load(Long id) { return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id)); }
}
