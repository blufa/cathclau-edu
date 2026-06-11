package gn.lacaeducation.communication.adapters.in.web;
import gn.lacaeducation.communication.application.port.in.CommunicationUseCase;
import gn.lacaeducation.communication.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/communications") @RequiredArgsConstructor
public class CommunicationController {
    private final CommunicationUseCase useCase;
    public record SendRequest(Long tenantId, Long senderId, CommunicationChannel channel,
                              RecipientType recipientType, String subject, String content,
                              CommunicationReason reason, List<Long> recipientUserIds) {}
    @PostMapping public ResponseEntity<Communication> send(@RequestBody SendRequest r) {
        return ResponseEntity.ok(useCase.send(new CommunicationUseCase.SendCommunicationCommand(
                r.tenantId(), r.senderId(), r.channel(), r.recipientType(), r.subject(),
                r.content(), r.reason(), r.recipientUserIds())));
    }
    @GetMapping("/{id}") public ResponseEntity<Communication> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/tenant/{tenantId}") public List<Communication> findByTenant(@PathVariable Long tenantId) { return useCase.findByTenantId(tenantId); }
    @GetMapping("/sender/{senderId}") public List<Communication> findBySender(@PathVariable Long senderId) { return useCase.findBySenderId(senderId); }
    @GetMapping("/tenant/{tenantId}/channel/{channel}") public List<Communication> findByChannel(@PathVariable Long tenantId, @PathVariable CommunicationChannel channel) {
        return useCase.findByTenantIdAndChannel(tenantId, channel);
    }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { useCase.delete(id); return ResponseEntity.noContent().build(); }
}
