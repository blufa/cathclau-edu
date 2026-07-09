package gn.lacaeducation.communication.application.port.out;
import gn.lacaeducation.communication.domain.CommunicationChannel;

/**
 * Outbound port to the messaging providers (§11.1). A dedicated adapter implements one channel
 * (SMS/Twilio, SMTP, WhatsApp Business API); the service selects the adapter via {@link #getChannel()}.
 */
public interface NotificationGateway {
    record DeliveryResult(boolean delivered, String detail) {}

    /** The channel this gateway dispatches to. */
    CommunicationChannel getChannel();

    /** Dispatches a single message to {@code destination} (phone or email depending on the channel). */
    DeliveryResult send(String destination, String subject, String content);
}
