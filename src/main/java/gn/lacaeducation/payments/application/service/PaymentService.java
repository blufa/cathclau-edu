package gn.lacaeducation.payments.application.service;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.payments.application.event.PaymentFailedEvent;
import gn.lacaeducation.payments.application.event.PaymentRefundedEvent;
import gn.lacaeducation.payments.application.event.PaymentSucceededEvent;
import gn.lacaeducation.payments.application.port.in.PaymentUseCase;
import gn.lacaeducation.payments.application.port.in.ReceiptUseCase;
import gn.lacaeducation.payments.application.port.out.PaymentProviderGateway;
import gn.lacaeducation.payments.application.port.out.PaymentRepository;
import gn.lacaeducation.payments.application.port.out.PaymentTransactionRepository;
import gn.lacaeducation.payments.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Slf4j
@Service @Transactional @RequiredArgsConstructor
public class PaymentService implements PaymentUseCase {
    private final PaymentRepository repo;
    private final PaymentTransactionRepository transactionRepo;
    private final ReceiptUseCase receiptUseCase;
    private final List<PaymentProviderGateway> gateways;
    private final ApplicationEventPublisher eventPublisher;

    @Override public Payment initiate(InitiatePaymentCommand c) {
        // Facture → Paiement → TransactionPaiement (appel fournisseur) → EN_ATTENTE (§9.1).
        Payment payment = repo.save(Payment.builder().tenantId(c.tenantId()).invoiceId(c.invoiceId()).studentId(c.studentId())
                .reference(c.reference()).amount(c.amount()).channel(c.channel()).type(c.type())
                .status(PaymentStatus.INITIATED).paidAt(LocalDateTime.now()).build());

        PaymentProviderGateway gateway = resolveGateway(c.channel());
        try {
            PaymentTransaction tx = gateway.initiate(payment.getId(), payment.getAmount(), payment.getReference(), null);
            transactionRepo.save(tx);
            payment.transitionTo(PaymentStatus.PENDING);
        } catch (RuntimeException e) {
            log.error("Provider initiation failed for payment id={} channel={}: {}", payment.getId(), c.channel(), e.getMessage());
            payment.transitionTo(PaymentStatus.FAILED);
            repo.save(payment);
            eventPublisher.publishEvent(new PaymentFailedEvent(payment.getTenantId(), payment.getStudentId(),
                    payment.getId(), payment.getInvoiceId(), payment.getAmount()));
            throw e;
        }
        return repo.save(payment);
    }

    @Override public Payment confirmSuccess(Long id) {
        Payment p = load(id);
        p.transitionTo(PaymentStatus.SUCCEEDED);
        p.setPaidAt(LocalDateTime.now());
        Payment saved = repo.save(p);
        // Receipt is generated only on a SUCCEEDED payment (§9.3), idempotently.
        receiptUseCase.generate(p.getId());
        eventPublisher.publishEvent(new PaymentSucceededEvent(p.getTenantId(), p.getStudentId(), p.getId(), p.getInvoiceId(), p.getAmount()));
        return saved;
    }

    @Override public Payment markFailed(Long id) {
        Payment p = load(id);
        p.transitionTo(PaymentStatus.FAILED);
        Payment saved = repo.save(p);
        eventPublisher.publishEvent(new PaymentFailedEvent(p.getTenantId(), p.getStudentId(), p.getId(), p.getInvoiceId(), p.getAmount()));
        return saved;
    }

    @Override public Payment markRefunded(Long id) {
        Payment p = load(id);
        p.transitionTo(PaymentStatus.REFUNDED);
        Payment saved = repo.save(p);
        eventPublisher.publishEvent(new PaymentRefundedEvent(p.getTenantId(), p.getStudentId(), p.getId(), p.getInvoiceId(), p.getAmount()));
        return saved;
    }

    @Override @Transactional(readOnly=true) public Optional<Payment> findById(Long id) { return repo.findById(id); }
    @Override @Transactional(readOnly=true) public Optional<Payment> findByReference(String ref) { return repo.findByReference(ref); }
    @Override @Transactional(readOnly=true) public List<Payment> findByInvoiceId(Long inv) { return repo.findByInvoiceId(inv); }
    @Override @Transactional(readOnly=true) public List<Payment> findByStudentId(Long s) { return repo.findByStudentId(s); }
    @Override @Transactional(readOnly=true) public List<Payment> findByTenantId(Long t) { return repo.findByTenantId(t); }
    @Override @Transactional(readOnly=true) public List<Payment> findByTenantIdAndStatus(Long t, PaymentStatus s) { return repo.findByTenantIdAndStatus(t, s); }
    @Override public void delete(Long id) { repo.deleteById(id); }

    private PaymentProviderGateway resolveGateway(PaymentChannel channel) {
        return gateways.stream().filter(g -> g.getChannel() == channel).findFirst()
                .orElseThrow(() -> new BusinessRuleException("No payment provider configured for channel " + channel));
    }

    private Payment load(Long id) { return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Payment", id)); }
}
