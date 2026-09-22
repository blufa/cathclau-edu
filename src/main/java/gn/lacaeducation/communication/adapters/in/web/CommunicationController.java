package gn.lacaeducation.communication.adapters.in.web;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.communication.application.port.in.CommunicationUseCase;
import gn.lacaeducation.communication.domain.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@Tag(name = "Communications", description = "Envoyer et gérer les communications SMS, e-mail et WhatsApp vers les parents, élèves et enseignants")
@RestController @RequestMapping("/api/v1/communications") @RequiredArgsConstructor
public class CommunicationController {
    private final CommunicationUseCase useCase;
    /** recipientDestinations : téléphone/e-mail bruts pour un parent/enseignant précis sans compte
     * plateforme lié (recipientUserIds ne suffit pas dans ce cas — voir RecipientTarget). */
    public record SendRequest(Long tenantId, Long senderId, CommunicationChannel channel,
                              RecipientType recipientType, String subject, String content,
                              CommunicationReason reason, List<Long> recipientUserIds,
                              List<String> recipientDestinations,
                              /** Clé d'idempotence générée côté client (double-clic, requête réseau
                               * dupliquée) — optionnelle. */
                              String idempotencyKey) {}
    @Operation(summary = "Envoyer une communication (SMS, e-mail ou WhatsApp) à un ou plusieurs destinataires")
    @PreAuthorize("hasAuthority('communication:send')")
    @PostMapping public ResponseEntity<Communication> send(@RequestBody SendRequest r) {
        List<CommunicationUseCase.RecipientTarget> targets = new java.util.ArrayList<>();
        if (r.recipientUserIds() != null) {
            r.recipientUserIds().forEach(id -> targets.add(new CommunicationUseCase.RecipientTarget(id, null)));
        }
        if (r.recipientDestinations() != null) {
            r.recipientDestinations().stream().filter(d -> d != null && !d.isBlank())
                    .forEach(d -> targets.add(new CommunicationUseCase.RecipientTarget(null, d)));
        }
        // Toujours résolu depuis le contexte tenant du serveur (jamais depuis le corps de la requête) :
        // un appelant frontend qui oublie de renseigner tenantId provoquait une violation de contrainte
        // NOT NULL en base au lieu d'un envoi normal — et faire confiance à un tenantId fourni par le
        // client serait de toute façon un risque d'isolation multi-tenant.
        Long tenantId = TenantContext.get();
        return ResponseEntity.ok(useCase.send(new CommunicationUseCase.SendCommunicationCommand(
                tenantId, r.senderId(), r.channel(), r.recipientType(), r.subject(),
                r.content(), r.reason(), targets, r.idempotencyKey())));
    }
    @Operation(summary = "Consulter une communication par son ID")
    @GetMapping("/{id}") public ResponseEntity<Communication> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @Operation(summary = "Lister les communications d'un établissement")
    @GetMapping("/tenant/{tenantId}") public List<Communication> findByTenant(@PathVariable Long tenantId) { return useCase.findByTenantId(tenantId); }
    @Operation(summary = "Lister les communications envoyées par un utilisateur donné")
    @GetMapping("/sender/{senderId}") public List<Communication> findBySender(@PathVariable Long senderId) { return useCase.findBySenderId(senderId); }
    @Operation(summary = "Lister les communications d'un établissement filtrées par canal (SMS, e-mail, WhatsApp)")
    @GetMapping("/tenant/{tenantId}/channel/{channel}") public List<Communication> findByChannel(@PathVariable Long tenantId, @PathVariable CommunicationChannel channel) {
        return useCase.findByTenantIdAndChannel(tenantId, channel);
    }
    @Operation(summary = "Supprimer un enregistrement de communication")
    @PreAuthorize("hasAuthority('communication:send')")
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { useCase.delete(id); return ResponseEntity.noContent().build(); }
}
