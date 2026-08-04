package gn.lacaeducation.communication.adapters.out.gateway;

import gn.lacaeducation.communication.application.port.out.NotificationGateway;
import gn.lacaeducation.communication.domain.CommunicationChannel;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * EMAIL gateway via SMTP (configured through {@code spring.mail.*}, e.g. Mailtrap in dev) using
 * {@link JavaMailSender}. Every message is rendered into the shared {@code email/notification.html}
 * template via {@link EmailTemplateRenderer}. Active only while Resend is not enabled
 * ({@code resend.enabled=false}, the default) — see {@link ResendEmailAdapter} for the real-world
 * sending path, which takes over as the sole EMAIL gateway once Resend is configured.
 */
@Component
@ConditionalOnProperty(prefix = "resend", name = "enabled", havingValue = "false", matchIfMissing = true)
@Slf4j
public class SmtpEmailGateway implements NotificationGateway {

    private final JavaMailSender mailSender;
    private final EmailTemplateRenderer renderer;

    @Value("${app.mail.from:CathClau Edu <no-reply@cathclau-edu.com>}")
    private String from;

    public SmtpEmailGateway(JavaMailSender mailSender, EmailTemplateRenderer renderer) {
        this.mailSender = mailSender;
        this.renderer = renderer;
    }

    @Override
    public CommunicationChannel getChannel() {
        return CommunicationChannel.EMAIL;
    }

    @Override
    public DeliveryResult send(String destination, String subject, String content) {
        if (destination == null || destination.isBlank()) {
            return new DeliveryResult(false, "no destination");
        }
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(destination);
            helper.setSubject(subject);
            helper.setText(renderer.render(subject, content), true);
            mailSender.send(msg);
            log.info("[EMAIL] sent to {} | {}", destination, subject);
            return new DeliveryResult(true, "sent");
        } catch (Exception e) {
            log.error("[EMAIL] failed to {} | {} : {}", destination, subject, e.getMessage());
            return new DeliveryResult(false, e.getMessage());
        }
    }
}
