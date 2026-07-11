package gn.lacaeducation.communication.adapters.out.gateway;
import gn.lacaeducation.communication.application.port.out.NotificationGateway;
import gn.lacaeducation.communication.domain.CommunicationChannel;
import org.springframework.stereotype.Component;

/**
 * Default no-op gateways: they log the dispatch and report success when a destination is present.
 * Real providers (Twilio, SMTP, WhatsApp Business API) replace these behind the same port.
 * One bean per channel so {@code CommunicationService} can resolve the right adapter.
 */
public class LoggingNotificationGateway implements NotificationGateway {
    private final CommunicationChannel channel;
    private final org.slf4j.Logger log;

    private LoggingNotificationGateway(CommunicationChannel channel) {
        this.channel = channel;
        this.log = org.slf4j.LoggerFactory.getLogger("communication.gateway." + channel);
    }

    @Override public CommunicationChannel getChannel() { return channel; }

    @Override public DeliveryResult send(String destination, String subject, String content) {
        if (destination == null || destination.isBlank()) {
            return new DeliveryResult(false, "no destination");
        }
        log.info("[{}] -> {} | {} | {}", channel, destination, subject, content);
        return new DeliveryResult(true, "logged");
    }

    // EMAIL → SmtpEmailGateway, SMS → OrangeSmsGateway (réels). Seul WhatsApp reste un stub de journalisation.
    @Component static class WhatsAppGateway extends LoggingNotificationGateway { WhatsAppGateway() { super(CommunicationChannel.WHATSAPP); } }
}
