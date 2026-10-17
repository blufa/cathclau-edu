package gn.lacaeducation.communication.application.port.out;
import gn.lacaeducation.communication.domain.CommunicationChannel;

/**
 * Outbound port to the messaging providers (§11.1). A dedicated adapter implements one channel
 * (SMS/Twilio, SMTP, WhatsApp Business API); the service selects the adapter via {@link #getChannel()}.
 */
public interface NotificationGateway {
    /**
     * @param retryable Faux pour un échec DÉFINITIF (ex. 4xx "numéro invalide" côté fournisseur) qui ne
     *                   deviendra jamais un succès en réessayant — {@code true} par défaut (comportement
     *                   historique) pour une vraie panne transitoire (réseau, 5xx fournisseur). Incident
     *                   de production : {@code SmsRetryDispatchScheduler} retentait indéfiniment des
     *                   numéros structurellement invalides, toutes les 30 min, pour toujours — avec des
     *                   dizaines d'entrées accumulées, ça a fini par saturer CPU/connexions DB.
     */
    record DeliveryResult(boolean delivered, String detail, String providerMessageId, boolean retryable) {
        public DeliveryResult(boolean delivered, String detail, String providerMessageId) {
            this(delivered, detail, providerMessageId, true);
        }
        /** Identifiant fournisseur absent (canal sans confirmation de livraison par webhook). */
        public DeliveryResult(boolean delivered, String detail) {
            this(delivered, detail, null, true);
        }
    }

    /** The channel this gateway dispatches to. */
    CommunicationChannel getChannel();

    /** Dispatches a single message to {@code destination} (phone or email depending on the channel). */
    DeliveryResult send(String destination, String subject, String content);
}
