package gn.lacaeducation.webhooks.adapters.in.web;
import gn.lacaeducation.webhooks.application.port.in.PaymentWebhookUseCase;
import gn.lacaeducation.webhooks.domain.PaymentWebhook;
import gn.lacaeducation.webhooks.domain.WebhookProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@Tag(name = "Payment Webhooks", description = "Receive and process inbound payment callbacks from providers (Orange Money, MTN, Wave, Stripe)")
@RestController @RequestMapping("/api/v1/webhooks/payment") @RequiredArgsConstructor
public class PaymentWebhookController {
    private final PaymentWebhookUseCase useCase;

    public record ReceiveRequest(Long transactionId, WebhookProvider provider, String event,
                                  String rawPayload, String signature, String externalEventId) {}

    @Operation(summary = "Receive an inbound payment callback from a payment provider")
    @PostMapping
    public ResponseEntity<PaymentWebhook> receive(
            @RequestBody ReceiveRequest r,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String headerSignature) {
        // Use header signature if provided, otherwise fall back to body signature field
        String effectiveSignature = headerSignature != null ? headerSignature : r.signature();
        // Idempotency check: if we already processed this external event, return 200 immediately
        if (r.externalEventId() != null && useCase.isAlreadyProcessed(r.externalEventId())) {
            return ResponseEntity.ok().build();
        }
        PaymentWebhook webhook = useCase.receive(new PaymentWebhookUseCase.ReceiveWebhookCommand(
                r.transactionId(), r.provider(), r.event(), r.rawPayload(), effectiveSignature, r.externalEventId()));
        if (!Boolean.TRUE.equals(webhook.getVerified())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(webhook);
    }

    @Operation(summary = "Get a payment webhook record by ID")
    @GetMapping("/{id}") public ResponseEntity<PaymentWebhook> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "List all unprocessed payment webhook events")
    @GetMapping("/unprocessed") public List<PaymentWebhook> findUnprocessed() { return useCase.findUnprocessed(); }

    @Operation(summary = "Mark a payment webhook as processed")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
    @PutMapping("/{id}/process") public ResponseEntity<PaymentWebhook> process(@PathVariable Long id) { return ResponseEntity.ok(useCase.markProcessed(id)); }
}
