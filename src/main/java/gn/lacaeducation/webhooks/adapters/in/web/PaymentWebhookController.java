package gn.lacaeducation.webhooks.adapters.in.web;
import gn.lacaeducation.webhooks.application.port.in.PaymentWebhookUseCase;
import gn.lacaeducation.webhooks.domain.PaymentWebhook;
import gn.lacaeducation.webhooks.domain.WebhookProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/webhooks/payment") @RequiredArgsConstructor
public class PaymentWebhookController {
    private final PaymentWebhookUseCase useCase;
    public record ReceiveRequest(Long transactionId, WebhookProvider provider, String event, String rawPayload, String signature) {}
    @PostMapping public ResponseEntity<PaymentWebhook> receive(@RequestBody ReceiveRequest r) {
        return ResponseEntity.ok(useCase.receive(new PaymentWebhookUseCase.ReceiveWebhookCommand(r.transactionId(), r.provider(), r.event(), r.rawPayload(), r.signature())));
    }
    @GetMapping("/{id}") public ResponseEntity<PaymentWebhook> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/unprocessed") public List<PaymentWebhook> findUnprocessed() { return useCase.findUnprocessed(); }
    @PutMapping("/{id}/process") public ResponseEntity<PaymentWebhook> process(@PathVariable Long id) { return ResponseEntity.ok(useCase.markProcessed(id)); }
}
