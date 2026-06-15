package gn.lacaeducation.payments.application.service;
import gn.lacaeducation.payments.application.event.PaymentSucceededEvent;
import gn.lacaeducation.payments.application.port.in.PaymentUseCase;
import gn.lacaeducation.payments.application.port.out.PaymentRepository;
import gn.lacaeducation.payments.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Service @Transactional @RequiredArgsConstructor
public class PaymentService implements PaymentUseCase {
    private final PaymentRepository repo;
    private final ApplicationEventPublisher eventPublisher;
    @Override public Payment initiate(InitiatePaymentCommand c) {
        return repo.save(Payment.builder().tenantId(c.tenantId()).invoiceId(c.invoiceId()).studentId(c.studentId())
                .reference(c.reference()).amount(c.amount()).channel(c.channel()).type(c.type())
                .status(PaymentStatus.INITIATED).paidAt(LocalDateTime.now()).build());
    }
    @Override public Payment confirmSuccess(Long id) {
        Payment p = load(id); p.setStatus(PaymentStatus.SUCCEEDED);
        Payment saved = repo.save(p);
        eventPublisher.publishEvent(new PaymentSucceededEvent(p.getTenantId(), p.getStudentId(), p.getId(), p.getInvoiceId(), p.getAmount()));
        return saved;
    }
    @Override public Payment markFailed(Long id) { Payment p = load(id); p.setStatus(PaymentStatus.FAILED); return repo.save(p); }
    @Override public Payment markRefunded(Long id) { Payment p = load(id); p.setStatus(PaymentStatus.REFUNDED); return repo.save(p); }
    @Override @Transactional(readOnly=true) public Optional<Payment> findById(Long id) { return repo.findById(id); }
    @Override @Transactional(readOnly=true) public Optional<Payment> findByReference(String ref) { return repo.findByReference(ref); }
    @Override @Transactional(readOnly=true) public List<Payment> findByInvoiceId(Long inv) { return repo.findByInvoiceId(inv); }
    @Override @Transactional(readOnly=true) public List<Payment> findByStudentId(Long s) { return repo.findByStudentId(s); }
    @Override @Transactional(readOnly=true) public List<Payment> findByTenantId(Long t) { return repo.findByTenantId(t); }
    @Override @Transactional(readOnly=true) public List<Payment> findByTenantIdAndStatus(Long t, PaymentStatus s) { return repo.findByTenantIdAndStatus(t, s); }
    @Override public void delete(Long id) { repo.deleteById(id); }
    private Payment load(Long id) { return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Payment not found: " + id)); }
}
