package gn.lacaeducation.webhooks.adapters.in.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import gn.lacaeducation.webhooks.application.service.DjomyWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public Djomy payment callback (no JWT auth — protected by the HMAC signature, §10.1).
 * Reads the RAW body so the signature is computed over the exact bytes Djomy signed.
 */
@Tag(name = "Djomy Webhook", description = "Inbound Djomy payment callback (public, signature-protected)")
@RestController
@RequestMapping("/api/v1/webhooks/paiement")
@RequiredArgsConstructor
@Slf4j
public class DjomyWebhookController {

    private final DjomyWebhookService service;

    /** Header names Djomy may use to carry the webhook signature (sandbox/prod variants). */
    private static final List<String> SIGNATURE_HEADERS = List.of(
            "X-Webhook-Signature", "X-Djomy-Signature", "Djomy-Signature", "X-Signature", "Signature");

    @Operation(summary = "Receive a Djomy payment callback (returns 200 immediately; 400 on bad signature)")
    @PostMapping(value = "/djomy", consumes = MediaType.ALL_VALUE)
    public ResponseEntity<Void> djomy(
            @RequestBody(required = false) String rawBody,
            @RequestHeader MultiValueMap<String, String> headers) throws JsonProcessingException {
        String signature = null;
        for (String name : SIGNATURE_HEADERS) {
            String v = headers.getFirst(name);
            if (v != null && !v.isBlank()) { signature = v; break; }
        }
        boolean accepted = service.handle(rawBody == null ? "" : rawBody, signature);
        if (!accepted) {
            // Diagnostic: log the headers Djomy actually sent so we can match the real signature header.
            log.warn("Djomy webhook rejected. Received headers: {}", headers);
        }
        return accepted ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
    }
}
