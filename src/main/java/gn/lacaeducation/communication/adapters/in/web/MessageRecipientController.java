package gn.lacaeducation.communication.adapters.in.web;
import gn.lacaeducation.communication.application.port.in.MessageRecipientUseCase;
import gn.lacaeducation.communication.domain.MessageRecipient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@Tag(name = "Message Recipients", description = "Track delivery and read status of messages per recipient user")
@RestController @RequestMapping("/api/v1/communications/messages") @RequiredArgsConstructor
public class MessageRecipientController {
    private final MessageRecipientUseCase useCase;
    @Operation(summary = "List all messages received by a user")
    @GetMapping("/user/{userId}") public List<MessageRecipient> findByUser(@PathVariable Long userId) { return useCase.findByUserId(userId); }
    @Operation(summary = "List unread messages for a user")
    @GetMapping("/user/{userId}/unread") public List<MessageRecipient> findUnread(@PathVariable Long userId) { return useCase.findUnreadByUserId(userId); }
    @Operation(summary = "Count unread messages for a user")
    @GetMapping("/user/{userId}/unread/count") public long countUnread(@PathVariable Long userId) { return useCase.countUnreadByUserId(userId); }
    @Operation(summary = "List all recipients for a given communication")
    @GetMapping("/communication/{communicationId}") public List<MessageRecipient> findByCommunication(@PathVariable Long communicationId) { return useCase.findByCommunicationId(communicationId); }
    @Operation(summary = "Mark a message as delivered to a recipient")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
    @PutMapping("/{id}/deliver") public ResponseEntity<MessageRecipient> markDelivered(@PathVariable Long id) { return ResponseEntity.ok(useCase.markDelivered(id)); }
    @Operation(summary = "Mark a message delivery as failed for a recipient")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
    @PutMapping("/{id}/fail") public ResponseEntity<MessageRecipient> markFailed(@PathVariable Long id) { return ResponseEntity.ok(useCase.markFailed(id)); }
    @Operation(summary = "Mark a message as read by a recipient")
    @PutMapping("/{id}/read") public ResponseEntity<MessageRecipient> markRead(@PathVariable Long id) { return ResponseEntity.ok(useCase.markRead(id)); }
}
