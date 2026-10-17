package gn.lacaeducation.communication.application.service;

import gn.lacaeducation.communication.application.port.out.MessageRecipientRepository;
import gn.lacaeducation.communication.application.port.out.NotificationGateway;
import gn.lacaeducation.communication.application.port.out.SmsRetryQueueRepository;
import gn.lacaeducation.communication.domain.CommunicationChannel;
import gn.lacaeducation.communication.domain.DeliveryStatus;
import gn.lacaeducation.communication.domain.SmsRetryEntry;
import gn.lacaeducation.communication.domain.SmsRetryStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Renvoie automatiquement les SMS mis en file après un échec d'envoi (Nimba injoignable, timeout
 * réseau…) — même principe que {@code OutgoingWebhookDispatcher} (§webhooks), appliqué au canal SMS,
 * avec une différence volontaire : PAS d'abandon après un nombre fixe de tentatives (§retour direction —
 * « les SMS doivent être conservés jusqu'à ce que le système détecte la connexion »). Tente toutes les
 * minutes les entrées dont l'heure de prochaine tentative est passée, avec un délai exponentiel entre
 * chaque essai plafonné à {@link #MAX_BACKOFF_SECONDS} pour ne pas attendre indéfiniment plus longtemps
 * une fois la connexion revenue — mais retente indéfiniment, jusqu'à succès.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SmsRetryDispatchScheduler {

    private static final int BACKOFF_BASE_SECONDS = 30;
    /** Au-delà de quelques tentatives, le délai exponentiel plafonne ici (30 minutes) plutôt que de
     * croître indéfiniment — l'entrée continue d'être retentée à ce rythme tant qu'elle n'a pas réussi. */
    private static final long MAX_BACKOFF_SECONDS = 1800;

    private final SmsRetryQueueRepository queueRepository;
    private final MessageRecipientRepository recipientRepository;
    private final List<NotificationGateway> gateways;

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void dispatchPendingSms() {
        NotificationGateway gateway = gateways.stream()
                .filter(g -> g.getChannel() == CommunicationChannel.SMS).findFirst().orElse(null);
        if (gateway == null) return;
        List<SmsRetryEntry> pending = queueRepository.findByStatusAndNextAttemptAtBefore(
                SmsRetryStatus.PENDING, LocalDateTime.now());
        for (SmsRetryEntry entry : pending) {
            dispatchEntry(gateway, entry);
        }
    }

    private void dispatchEntry(NotificationGateway gateway, SmsRetryEntry entry) {
        NotificationGateway.DeliveryResult result;
        try {
            result = gateway.send(entry.getDestination(), entry.getSubject(), entry.getContent());
        } catch (RuntimeException e) {
            result = new NotificationGateway.DeliveryResult(false, e.getMessage());
        }
        if (result.delivered()) {
            entry.setStatus(SmsRetryStatus.SENT);
            log.info("SMS en file id={} renvoyé avec succès vers {} (tentative {})",
                    entry.getId(), entry.getDestination(), entry.getAttemptCount() + 1);
            syncRecipient(entry, DeliveryStatus.DELIVERED, result.providerMessageId());
        } else if (!result.retryable()) {
            // Échec définitif (ex. numéro structurellement invalide, 4xx fournisseur) — jamais retenté :
            // incident de production où des dizaines d'entrées de ce type, retentées indéfiniment toutes
            // les 30 min sans jamais pouvoir réussir, ont fini par saturer CPU/connexions DB.
            entry.setStatus(SmsRetryStatus.FAILED);
            entry.setAttemptCount(entry.getAttemptCount() + 1);
            entry.setLastError(result.detail());
            log.warn("SMS en file id={} vers {} : échec définitif, abandonné ({})",
                    entry.getId(), entry.getDestination(), result.detail());
            syncRecipient(entry, DeliveryStatus.FAILED, null);
        } else {
            scheduleRetry(entry, result.detail());
        }
        queueRepository.save(entry);
    }

    /** Reprogramme indéfiniment pour un échec RETRYABLE (pas d'abandon) — le destinataire reste PENDING
     * tant que la connexion au fournisseur n'a pas été rétablie ; seul un succès (ou une relance
     * manuelle réinitialisant l'entrée) en sort. Un échec définitif (numéro invalide...) ne passe jamais
     * ici — voir le branchement sur {@code result.retryable()} dans {@link #dispatchEntry}. */
    private void scheduleRetry(SmsRetryEntry entry, String error) {
        int attempts = entry.getAttemptCount() + 1;
        entry.setAttemptCount(attempts);
        entry.setLastError(error);
        // Bug de production (DateTimeException "Invalid value for EpochDay") : "retente indéfiniment"
        // signifie que `attempts` grandit sans borne (ex. solde SMS épuisé pendant plusieurs jours).
        // Math.pow(2, attempts) sature à Long.MAX_VALUE au-delà d'environ 2^63 (attempts ~64, atteint
        // en ~32h au rythme plafonné de 30 min/tentative), mais Long.MAX_VALUE * BACKOFF_BASE_SECONDS
        // déborde ensuite silencieusement (overflow long, pas de saturation sur *) vers une valeur
        // négative absurde — plusSeconds() sur cette valeur pousse la date des millions d'années dans
        // le passé, hors de la plage représentable par LocalDateTime, qui lève alors l'exception et
        // fait échouer toute la transaction (donc TOUS les SMS du lot, pas que celui-ci). On plafonne
        // l'exposant AVANT le calcul : 2^10 * 30s dépasse déjà largement MAX_BACKOFF_SECONDS (30 min),
        // donc le Math.min ci-dessous continue de s'appliquer normalement sans jamais approcher une
        // valeur capable de déborder un long.
        int cappedAttempts = Math.min(attempts, 10);
        long backoffSeconds = Math.min((long) Math.pow(2, cappedAttempts) * BACKOFF_BASE_SECONDS, MAX_BACKOFF_SECONDS);
        entry.setNextAttemptAt(LocalDateTime.now().plusSeconds(backoffSeconds));
        log.info("SMS en file id={} vers {} : nouvel essai dans {}s (tentative {})",
                entry.getId(), entry.getDestination(), backoffSeconds, attempts);
    }

    /** Répercute le succès sur le destinataire d'origine, si suivi individuellement — reste PENDING
     * pendant toutes les tentatives intermédiaires (toujours en cours, jamais abandonnées seules). */
    private void syncRecipient(SmsRetryEntry entry, DeliveryStatus status, String providerMessageId) {
        if (entry.getRecipientId() == null) return;
        recipientRepository.findById(entry.getRecipientId()).ifPresent(recipient -> {
            recipient.setDeliveryStatus(status);
            recipient.setDeliveredAt(status == DeliveryStatus.DELIVERED ? LocalDateTime.now() : null);
            if (providerMessageId != null) recipient.setProviderMessageId(providerMessageId);
            recipientRepository.save(recipient);
        });
    }
}
