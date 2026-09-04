package gn.lacaeducation.webhooks.adapters.in.web;
import gn.lacaeducation.webhooks.application.port.in.WebhookEventUseCase;
import gn.lacaeducation.webhooks.domain.WebhookDeliveryStatus;
import gn.lacaeducation.webhooks.domain.WebhookEvent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@Tag(name = "Webhook Events", description = "List and manage outbound webhook event delivery records")
@RestController @RequestMapping("/api/v1/webhooks/events") @RequiredArgsConstructor
public class WebhookEventController {
    private final WebhookEventUseCase useCase;

    @Operation(summary = "List webhook events, optionally filtered by status and/or endpoint")
    @PreAuthorize("hasAuthority('webhook:manage')")
    @GetMapping
    public List<WebhookEvent> list(
            @RequestParam(required = false) WebhookDeliveryStatus status,
            @RequestParam(required = false) Long endpointId) {
        if (endpointId != null) {
            return useCase.findByEndpointId(endpointId);
        }
        if (status != null) {
            return useCase.findByStatus(status);
        }
        return useCase.findPendingEvents();
    }

    @Operation(summary = "Get a webhook event by ID")
    @PreAuthorize("hasAuthority('webhook:manage')")
    @GetMapping("/{id}")
    public ResponseEntity<WebhookEvent> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Replay a webhook event (reset to PENDING and reschedule immediately)")
    @PreAuthorize("hasAuthority('webhook:manage')")
    @PostMapping("/{id}/replay")
    public ResponseEntity<WebhookEvent> replay(@PathVariable Long id) {
        return ResponseEntity.ok(useCase.replay(id));
    }
}
