package gn.lacaeducation.webhooks.application.port.in;
import gn.lacaeducation.webhooks.domain.PaymentWebhook;
import gn.lacaeducation.webhooks.domain.WebhookProvider;
import java.util.List;
import java.util.Optional;
public interface PaymentWebhookUseCase {
    record ReceiveWebhookCommand(Long transactionId, WebhookProvider provider, String event, String rawPayload, String signature) {}
    PaymentWebhook receive(ReceiveWebhookCommand command);
    PaymentWebhook verify(Long id, String secret);
    PaymentWebhook markProcessed(Long id);
    Optional<PaymentWebhook> findById(Long id);
    List<PaymentWebhook> findUnprocessed();
    List<PaymentWebhook> findByProvider(WebhookProvider provider);
}
