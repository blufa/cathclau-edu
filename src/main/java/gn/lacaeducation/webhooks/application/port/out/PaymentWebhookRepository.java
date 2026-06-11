package gn.lacaeducation.webhooks.application.port.out;
import gn.lacaeducation.webhooks.domain.PaymentWebhook;
import gn.lacaeducation.webhooks.domain.WebhookProvider;
import java.util.List;
import java.util.Optional;
public interface PaymentWebhookRepository {
    PaymentWebhook save(PaymentWebhook webhook);
    Optional<PaymentWebhook> findById(Long id);
    List<PaymentWebhook> findByProcessed(boolean processed);
    List<PaymentWebhook> findByProvider(WebhookProvider provider);
}
