package gn.lacaeducation.webhooks.adapters.in.web;
import gn.lacaeducation.webhooks.application.port.in.WebhookEndpointUseCase;
import gn.lacaeducation.webhooks.domain.WebhookEndpoint;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@Tag(name = "Webhook Endpoints", description = "Configure outbound webhook endpoints to notify third-party systems of school events")
@RestController @RequestMapping("/api/v1/webhooks/endpoints") @RequiredArgsConstructor
public class WebhookEndpointController {
    private final WebhookEndpointUseCase useCase;
    public record CreateRequest(Long tenantId, String url, String secret, String events) {}
    public record UpdateRequest(String url, String secret, String events, Boolean active) {}
    @Operation(summary = "Register a new webhook endpoint")
    @PreAuthorize("hasAuthority('webhook:manage')")
    @PostMapping public ResponseEntity<WebhookEndpoint> create(@RequestBody CreateRequest r) {
        return ResponseEntity.ok(useCase.create(new WebhookEndpointUseCase.CreateEndpointCommand(r.tenantId(), r.url(), r.secret(), r.events())));
    }
    @Operation(summary = "Update a webhook endpoint configuration")
    @PreAuthorize("hasAuthority('webhook:manage')")
    @PutMapping("/{id}") public ResponseEntity<WebhookEndpoint> update(@PathVariable Long id, @RequestBody UpdateRequest r) {
        return ResponseEntity.ok(useCase.update(id, new WebhookEndpointUseCase.UpdateEndpointCommand(r.url(), r.secret(), r.events(), r.active())));
    }
    @Operation(summary = "Get a webhook endpoint by ID")
    @GetMapping("/{id}") public ResponseEntity<WebhookEndpoint> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @Operation(summary = "List all webhook endpoints for a tenant")
    @GetMapping("/tenant/{tenantId}") public List<WebhookEndpoint> findByTenant(@PathVariable Long tenantId) { return useCase.findByTenantId(tenantId); }
    @Operation(summary = "Activate a webhook endpoint")
    @PreAuthorize("hasAuthority('webhook:manage')")
    @PutMapping("/{id}/activate") public ResponseEntity<Void> activate(@PathVariable Long id) { useCase.activate(id); return ResponseEntity.ok().build(); }
    @Operation(summary = "Deactivate a webhook endpoint")
    @PreAuthorize("hasAuthority('webhook:manage')")
    @PutMapping("/{id}/deactivate") public ResponseEntity<Void> deactivate(@PathVariable Long id) { useCase.deactivate(id); return ResponseEntity.ok().build(); }
    @Operation(summary = "Delete a webhook endpoint")
    @PreAuthorize("hasAuthority('webhook:manage')")
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { useCase.delete(id); return ResponseEntity.noContent().build(); }
}
