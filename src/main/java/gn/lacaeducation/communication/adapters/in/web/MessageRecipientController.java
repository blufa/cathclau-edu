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
@Tag(name = "Message Recipients", description = "Suivre le statut de livraison et de lecture des messages par destinataire")
@RestController @RequestMapping("/api/v1/communications/messages") @RequiredArgsConstructor
public class MessageRecipientController {
    private final MessageRecipientUseCase useCase;
    @Operation(summary = "Lister tous les messages reçus par un utilisateur")
    @GetMapping("/user/{userId}") public List<MessageRecipient> findByUser(@PathVariable Long userId) { return useCase.findByUserId(userId); }
    @Operation(summary = "Lister les messages non lus d'un utilisateur")
    @GetMapping("/user/{userId}/unread") public List<MessageRecipient> findUnread(@PathVariable Long userId) { return useCase.findUnreadByUserId(userId); }
    @Operation(summary = "Compter les messages non lus d'un utilisateur")
    @GetMapping("/user/{userId}/unread/count") public long countUnread(@PathVariable Long userId) { return useCase.countUnreadByUserId(userId); }
    @Operation(summary = "Lister tous les destinataires d'une communication donnée")
    @GetMapping("/communication/{communicationId}") public List<MessageRecipient> findByCommunication(@PathVariable Long communicationId) { return useCase.findByCommunicationId(communicationId); }
    @Operation(summary = "Marquer un message comme livré à un destinataire")
    @PreAuthorize("hasAuthority('communication:read')")
    @PutMapping("/{id}/deliver") public ResponseEntity<MessageRecipient> markDelivered(@PathVariable Long id) { return ResponseEntity.ok(useCase.markDelivered(id)); }
    @Operation(summary = "Marquer la livraison d'un message comme échouée pour un destinataire")
    @PreAuthorize("hasAuthority('communication:read')")
    @PutMapping("/{id}/fail") public ResponseEntity<MessageRecipient> markFailed(@PathVariable Long id) { return ResponseEntity.ok(useCase.markFailed(id)); }
    @Operation(summary = "Marquer un message comme lu par un destinataire")
    @PutMapping("/{id}/read") public ResponseEntity<MessageRecipient> markRead(@PathVariable Long id) { return ResponseEntity.ok(useCase.markRead(id)); }
}
