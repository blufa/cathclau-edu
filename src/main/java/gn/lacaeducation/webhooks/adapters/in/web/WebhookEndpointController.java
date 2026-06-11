package gn.lacaeducation.webhooks.adapters.in.web;
import gn.lacaeducation.webhooks.application.port.in.WebhookEndpointUseCase;
import gn.lacaeducation.webhooks.domain.WebhookEndpoint;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/webhooks/endpoints") @RequiredArgsConstructor
public class WebhookEndpointController {
    private final WebhookEndpointUseCase useCase;
    public record CreateRequest(Long tenantId, String url, String secret, String events) {}
    public record UpdateRequest(String url, String secret, String events, Boolean active) {}
    @PostMapping public ResponseEntity<WebhookEndpoint> create(@RequestBody CreateRequest r) {
        return ResponseEntity.ok(useCase.create(new WebhookEndpointUseCase.CreateEndpointCommand(r.tenantId(), r.url(), r.secret(), r.events())));
    }
    @PutMapping("/{id}") public ResponseEntity<WebhookEndpoint> update(@PathVariable Long id, @RequestBody UpdateRequest r) {
        return ResponseEntity.ok(useCase.update(id, new WebhookEndpointUseCase.UpdateEndpointCommand(r.url(), r.secret(), r.events(), r.active())));
    }
    @GetMapping("/{id}") public ResponseEntity<WebhookEndpoint> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/tenant/{tenantId}") public List<WebhookEndpoint> findByTenant(@PathVariable Long tenantId) { return useCase.findByTenantId(tenantId); }
    @PutMapping("/{id}/activate") public ResponseEntity<Void> activate(@PathVariable Long id) { useCase.activate(id); return ResponseEntity.ok().build(); }
    @PutMapping("/{id}/deactivate") public ResponseEntity<Void> deactivate(@PathVariable Long id) { useCase.deactivate(id); return ResponseEntity.ok().build(); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { useCase.delete(id); return ResponseEntity.noContent().build(); }
}
