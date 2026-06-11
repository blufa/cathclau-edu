package gn.lacaeducation.communication.adapters.in.web;
import gn.lacaeducation.communication.application.port.in.CommunicationUseCase;
import gn.lacaeducation.communication.domain.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@Tag(name = "Communications", description = "Send and manage SMS, email, and WhatsApp communications to parents, students, and teachers")
@RestController @RequestMapping("/api/v1/communications") @RequiredArgsConstructor
public class CommunicationController {
    private final CommunicationUseCase useCase;
    public record SendRequest(Long tenantId, Long senderId, CommunicationChannel channel,
                              RecipientType recipientType, String subject, String content,
                              CommunicationReason reason, List<Long> recipientUserIds) {}
    @Operation(summary = "Send a communication (SMS, email, or WhatsApp) to one or more recipients")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY','TEACHER')")
    @PostMapping public ResponseEntity<Communication> send(@RequestBody SendRequest r) {
        return ResponseEntity.ok(useCase.send(new CommunicationUseCase.SendCommunicationCommand(
                r.tenantId(), r.senderId(), r.channel(), r.recipientType(), r.subject(),
                r.content(), r.reason(), r.recipientUserIds())));
    }
    @Operation(summary = "Get a communication by ID")
    @GetMapping("/{id}") public ResponseEntity<Communication> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @Operation(summary = "List all communications for a tenant")
    @GetMapping("/tenant/{tenantId}") public List<Communication> findByTenant(@PathVariable Long tenantId) { return useCase.findByTenantId(tenantId); }
    @Operation(summary = "List all communications sent by a specific user")
    @GetMapping("/sender/{senderId}") public List<Communication> findBySender(@PathVariable Long senderId) { return useCase.findBySenderId(senderId); }
    @Operation(summary = "List communications for a tenant filtered by channel (SMS, email, WhatsApp)")
    @GetMapping("/tenant/{tenantId}/channel/{channel}") public List<Communication> findByChannel(@PathVariable Long tenantId, @PathVariable CommunicationChannel channel) {
        return useCase.findByTenantIdAndChannel(tenantId, channel);
    }
    @Operation(summary = "Delete a communication record")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { useCase.delete(id); return ResponseEntity.noContent().build(); }
}
