package gn.lacaeducation.billing.application.service;
import gn.lacaeducation.billing.application.event.InvoiceGeneratedEvent;
import gn.lacaeducation.billing.application.event.InvoiceIssuedEvent;
import gn.lacaeducation.billing.application.event.InvoicePaidEvent;
import gn.lacaeducation.billing.application.port.in.InvoiceUseCase;
import gn.lacaeducation.billing.application.port.out.EnrollmentQueryPort;
import gn.lacaeducation.billing.application.port.out.InvoiceRepository;
import gn.lacaeducation.billing.application.port.out.SchoolFeeRepository;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.InvoiceStatus;
import gn.lacaeducation.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Slf4j
@Service @Transactional @RequiredArgsConstructor
public class InvoiceService implements InvoiceUseCase {
    private final InvoiceRepository repo;
    private final SchoolFeeRepository schoolFeeRepository;
    private final EnrollmentQueryPort enrollmentQueryPort;
    private final ApplicationEventPublisher eventPublisher;

    @Override public Invoice create(CreateInvoiceCommand c) {
        String reference = (c.reference() == null || c.reference().isBlank())
                ? generateReference(c.tenantId()) : c.reference();
        Invoice inv = repo.save(Invoice.builder().tenantId(c.tenantId()).studentId(c.studentId()).feeId(c.feeId())
                .reference(reference).totalAmount(c.totalAmount()).paidAmount(BigDecimal.ZERO)
                .remainingBalance(c.totalAmount()).status(InvoiceStatus.UNPAID).issueDate(LocalDate.now()).build());
        eventPublisher.publishEvent(new InvoiceGeneratedEvent(c.tenantId(), c.studentId(), inv.getId(), inv.getReference()));
        eventPublisher.publishEvent(new InvoiceIssuedEvent(inv.getId(), c.studentId(), c.tenantId()));
        return inv;
    }

    /** Tenant-scoped unique invoice reference (FAC-&lt;tenant&gt;-&lt;date&gt;-&lt;suffix&gt;). */
    private String generateReference(Long tenantId) {
        String date = LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "FAC-" + tenantId + "-" + date + "-" + suffix;
    }

    @Override public List<Invoice> generateBulk(BulkGenerateCommand cmd) {
        schoolFeeRepository.findById(cmd.feeId())
                .orElseThrow(() -> new ResourceNotFoundException("SchoolFee", cmd.feeId()));

        List<Long> studentIds = enrollmentQueryPort.findValidatedStudentIdsByClassId(cmd.classId());
        List<Invoice> created = new ArrayList<>();

        for (Long studentId : studentIds) {
            if (repo.existsByStudentIdAndFeeId(studentId, cmd.feeId())) {
                log.debug("Skipping bulk invoice: already exists for student id={} fee id={}", studentId, cmd.feeId());
                continue;
            }
            String reference = "BULK-" + cmd.tenantId() + "-" + studentId + "-" + cmd.feeId()
                    + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            var fee = schoolFeeRepository.findById(cmd.feeId()).orElseThrow();
            Invoice inv = create(new CreateInvoiceCommand(cmd.tenantId(), studentId, cmd.feeId(), reference, fee.getAmount()));
            created.add(inv);
        }
        log.info("Bulk invoice generation: created {} invoices for class id={} fee id={}", created.size(), cmd.classId(), cmd.feeId());
        return created;
    }

    @Override @Transactional(readOnly=true) public Optional<Invoice> findById(Long id) { return repo.findById(id); }
    @Override @Transactional(readOnly=true) public Optional<Invoice> findByReference(String ref) { return repo.findByReference(ref); }
    @Override @Transactional(readOnly=true) public List<Invoice> findByStudentId(Long s) { return repo.findByStudentId(s); }
    @Override @Transactional(readOnly=true) public List<Invoice> findByTenantId(Long t) { return repo.findByTenantId(t); }
    @Override @Transactional(readOnly=true) public List<Invoice> findByTenantIdAndStatus(Long t, InvoiceStatus s) { return repo.findByTenantIdAndStatus(t, s); }
    @Override @Transactional(readOnly=true) public List<Invoice> findByFeeId(Long feeId) { return repo.findByFeeId(feeId); }
    @Override @Transactional(readOnly=true) public boolean existsByStudentIdAndFeeId(Long studentId, Long feeId) { return repo.existsByStudentIdAndFeeId(studentId, feeId); }
    @Override public Invoice applyPayment(Long invoiceId, BigDecimal amount) {
        Invoice inv = load(invoiceId);
        inv.setPaidAmount(inv.getPaidAmount().add(amount));
        inv.setRemainingBalance(inv.getTotalAmount().subtract(inv.getPaidAmount()));
        boolean fullySettled = inv.getRemainingBalance().compareTo(BigDecimal.ZERO) <= 0;
        inv.setStatus(fullySettled ? InvoiceStatus.PAID : InvoiceStatus.PARTIAL);
        Invoice saved = repo.save(inv);
        if (fullySettled) {
            eventPublisher.publishEvent(
                    new InvoicePaidEvent(inv.getTenantId(), inv.getStudentId(), inv.getId(), inv.getReference()));
        }
        return saved;
    }
    @Override public void cancel(Long id) { repo.deleteById(id); }
    @Override public void delete(Long id) { repo.deleteById(id); }
    @Override @Transactional(readOnly=true) public boolean existsById(Long id) { return repo.existsById(id); }
    private Invoice load(Long id) { return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invoice", id)); }
}
