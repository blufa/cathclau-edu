package gn.lacaeducation.communication.adapters.in.web;

import gn.lacaeducation.communication.application.service.SmsWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Rappel (webhook) public du fournisseur SMS NimbaSMS : URL à renseigner dans le tableau de bord
 * Nimba (API KEYS &gt; Webhooks) pour recevoir le statut réel de livraison de chaque SMS envoyé.
 * Non authentifié comme tout webhook entrant (voir SecurityConfig) — la corrélation se fait via
 * l'identifiant de message Nimba ({@code messageid}, un UUID non devinable), donc rien de sensible
 * n'est exposé ni modifiable par un tiers qui appellerait cette URL au hasard.
 *
 * <p>Format confirmé par la documentation Nimba : {@code { messageid, status: "received"|"failed",
 * contact, metadata }}.</p>
 */
@Tag(name = "SMS Webhook", description = "Rappel de statut de livraison SMS (NimbaSMS)")
@RestController
@RequestMapping("/api/v1/webhooks/sms")
@RequiredArgsConstructor
public class SmsWebhookController {

    private final SmsWebhookService service;

    public record NimbaWebhookPayload(String messageid, String status, String contact, Object metadata) {}

    @Operation(summary = "Recevoir le rappel de statut de livraison NimbaSMS (public)")
    @PostMapping("/nimba")
    public ResponseEntity<Map<String, String>> receiveNimba(@RequestBody(required = false) NimbaWebhookPayload body) {
        String raw = body == null ? "" : body.toString();
        SmsWebhookService.NimbaCallback callback = body == null ? null
                : new SmsWebhookService.NimbaCallback(body.messageid(), body.status(), body.contact());
        service.receiveNimbaCallback(raw, callback);
        // Toujours 200 : Nimba réessaie jusqu'à 3 fois si la confirmation n'est pas reçue.
        return ResponseEntity.ok(Map.of("status", "OK"));
    }
}
