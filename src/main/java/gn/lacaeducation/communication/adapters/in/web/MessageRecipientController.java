package gn.lacaeducation.communication.adapters.in.web;
import gn.lacaeducation.communication.application.port.in.MessageRecipientUseCase;
import gn.lacaeducation.communication.domain.MessageRecipient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/communications/messages") @RequiredArgsConstructor
public class MessageRecipientController {
    private final MessageRecipientUseCase useCase;
    @GetMapping("/user/{userId}") public List<MessageRecipient> findByUser(@PathVariable Long userId) { return useCase.findByUserId(userId); }
    @GetMapping("/user/{userId}/unread") public List<MessageRecipient> findUnread(@PathVariable Long userId) { return useCase.findUnreadByUserId(userId); }
    @GetMapping("/user/{userId}/unread/count") public long countUnread(@PathVariable Long userId) { return useCase.countUnreadByUserId(userId); }
    @GetMapping("/communication/{communicationId}") public List<MessageRecipient> findByCommunication(@PathVariable Long communicationId) { return useCase.findByCommunicationId(communicationId); }
    @PutMapping("/{id}/deliver") public ResponseEntity<MessageRecipient> markDelivered(@PathVariable Long id) { return ResponseEntity.ok(useCase.markDelivered(id)); }
    @PutMapping("/{id}/fail") public ResponseEntity<MessageRecipient> markFailed(@PathVariable Long id) { return ResponseEntity.ok(useCase.markFailed(id)); }
    @PutMapping("/{id}/read") public ResponseEntity<MessageRecipient> markRead(@PathVariable Long id) { return ResponseEntity.ok(useCase.markRead(id)); }
}
