package gn.lacaeducation.webhooks.application.service;
import gn.lacaeducation.webhooks.application.port.in.PaymentWebhookUseCase;
import gn.lacaeducation.webhooks.application.port.out.PaymentWebhookRepository;
import gn.lacaeducation.webhooks.domain.PaymentWebhook;
import gn.lacaeducation.webhooks.domain.WebhookProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Service @Transactional @RequiredArgsConstructor
public class PaymentWebhookService implements PaymentWebhookUseCase {
    private final PaymentWebhookRepository repo;
    @Override public PaymentWebhook receive(ReceiveWebhookCommand c) {
        return repo.save(PaymentWebhook.builder().transactionId(c.transactionId()).provider(c.provider())
                .event(c.event()).rawPayload(c.rawPayload()).signature(c.signature())
                .verified(false).processed(false).receivedAt(LocalDateTime.now()).build());
    }
    @Override public PaymentWebhook verify(Long id, String secret) {
        PaymentWebhook wh = load(id);
        wh.setVerified(wh.getSignature() != null && !wh.getSignature().isBlank());
        return repo.save(wh);
    }
    @Override public PaymentWebhook markProcessed(Long id) {
        PaymentWebhook wh = load(id); wh.setProcessed(true); return repo.save(wh);
    }
    @Override @Transactional(readOnly=true) public Optional<PaymentWebhook> findById(Long id) { return repo.findById(id); }
    @Override @Transactional(readOnly=true) public List<PaymentWebhook> findUnprocessed() { return repo.findByProcessed(false); }
    @Override @Transactional(readOnly=true) public List<PaymentWebhook> findByProvider(WebhookProvider p) { return repo.findByProvider(p); }
    private PaymentWebhook load(Long id) { return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("PaymentWebhook not found: " + id)); }
}
