package gn.lacaeducation.webhooks.adapters.in.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import gn.lacaeducation.webhooks.application.service.DjomyWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public Djomy payment callback (no JWT auth — protected by the HMAC signature, §10.1).
 * Reads the RAW body so the signature is computed over the exact bytes Djomy signed.
 */
@Tag(name = "Djomy Webhook", description = "Inbound Djomy payment callback (public, signature-protected)")
@RestController
@RequestMapping("/api/v1/webhooks/paiement")
@RequiredArgsConstructor
public class DjomyWebhookController {

    private final DjomyWebhookService service;

    @Operation(summary = "Receive a Djomy payment callback (returns 200 immediately; 400 on bad signature)")
    @PostMapping(value = "/djomy", consumes = MediaType.ALL_VALUE)
    public ResponseEntity<Void> djomy(
            @RequestBody(required = false) String rawBody,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature) throws JsonProcessingException {
        boolean accepted = service.handle(rawBody == null ? "" : rawBody, signature);
        return accepted ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
    }
}
