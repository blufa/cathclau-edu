package gn.lacaeducation.communication.adapters.in.web;
import gn.lacaeducation.communication.application.port.in.MessageTemplateUseCase;
import gn.lacaeducation.communication.application.port.in.MessageTemplateUseCase.SaveTemplateCommand;
import gn.lacaeducation.communication.domain.CommunicationChannel;
import gn.lacaeducation.communication.domain.CommunicationReason;
import gn.lacaeducation.communication.domain.MessageTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@Tag(name = "Message templates", description = "Per-tenant message templates with {{variable}} placeholders (§11.2)")
@RestController @RequestMapping("/api/v1/templates-message") @RequiredArgsConstructor
public class MessageTemplateController {
    private final MessageTemplateUseCase useCase;

    public record TemplateRequest(Long tenantId, CommunicationReason reason, CommunicationChannel channel,
                                  String subject, String body) {}

    @Operation(summary = "Create a message template for a given reason")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @PostMapping public ResponseEntity<MessageTemplate> create(@RequestBody TemplateRequest r) {
        MessageTemplate t = useCase.create(new SaveTemplateCommand(r.tenantId(), r.reason(), r.channel(), r.subject(), r.body()));
        return ResponseEntity.created(URI.create("/api/v1/templates-message/" + t.getId())).body(t);
    }

    @Operation(summary = "List message templates for a tenant")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
    @GetMapping public List<MessageTemplate> findByTenant(@RequestParam Long tenantId) { return useCase.findByTenantId(tenantId); }

    @Operation(summary = "Get a message template by ID")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
    @GetMapping("/{id}") public ResponseEntity<MessageTemplate> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Update a message template")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @PutMapping("/{id}") public ResponseEntity<MessageTemplate> update(@PathVariable Long id, @RequestBody TemplateRequest r) {
        return ResponseEntity.ok(useCase.update(id, new SaveTemplateCommand(r.tenantId(), r.reason(), r.channel(), r.subject(), r.body())));
    }

    @Operation(summary = "Delete a message template")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { useCase.delete(id); return ResponseEntity.noContent().build(); }
}
