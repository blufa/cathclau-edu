package gn.lacaeducation.communication.application.service;

import gn.lacaeducation.communication.application.port.out.MessageRecipientRepository;
import gn.lacaeducation.communication.domain.DeliveryStatus;
import gn.lacaeducation.communication.domain.MessageRecipient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Traite le rappel (webhook) de statut de livraison envoyé par NimbaSMS après l'envoi initial —
 * recale {@link MessageRecipient#getDeliveryStatus()} sur le statut réel constaté par le fournisseur
 * (le statut posé à l'envoi n'est que « accepté par le fournisseur »).
 *
 * <p>Format confirmé par la documentation Nimba (POST /v1/webhooks) : {@code messageid} (uuid),
 * {@code status} ("received" ou "failed"), {@code contact}, {@code metadata}.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SmsWebhookService {

    private final MessageRecipientRepository recipientRepo;

    public record NimbaCallback(String messageid, String status, String contact) {}

    @Transactional
    public void receiveNimbaCallback(String rawPayload, NimbaCallback payload) {
        log.info("[SMS webhook] Nimba rawPayload={}", rawPayload);
        if (payload == null || payload.messageid() == null || payload.messageid().isBlank()) {
            log.warn("[SMS webhook] Corps de rappel Nimba illisible ou sans messageid, ignoré.");
            return;
        }
        boolean matched = recipientRepo.findByProviderMessageId(payload.messageid()).map(r -> {
            DeliveryStatus resolved = resolveStatus(payload.status());
            r.setDeliveryStatus(resolved);
            if (resolved == DeliveryStatus.DELIVERED && r.getDeliveredAt() == null) {
                r.setDeliveredAt(LocalDateTime.now());
            }
            recipientRepo.save(r);
            log.info("[SMS webhook] Recipient {} (providerMessageId={}) -> {}", r.getId(), payload.messageid(), resolved);
            return true;
        }).orElse(false);
        if (!matched) {
            log.info("[SMS webhook] Aucun message correspondant trouvé pour messageid={}", payload.messageid());
        }
    }

    /** Nimba n'a que deux statuts de rappel : "received" (livré au mobile) et "failed". */
    private DeliveryStatus resolveStatus(String rawStatus) {
        if (rawStatus == null) return DeliveryStatus.PENDING;
        String s = rawStatus.trim().toLowerCase();
        if (s.equals("failed")) return DeliveryStatus.FAILED;
        if (s.equals("received")) return DeliveryStatus.DELIVERED;
        return DeliveryStatus.PENDING;
    }
}
