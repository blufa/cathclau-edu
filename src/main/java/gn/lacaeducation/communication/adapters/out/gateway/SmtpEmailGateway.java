package gn.lacaeducation.communication.adapters.out.gateway;

import gn.lacaeducation.communication.application.port.out.NotificationGateway;
import gn.lacaeducation.communication.domain.CommunicationChannel;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Real EMAIL gateway: sends branded HTML e-mails through SMTP (configured via {@code spring.mail.*},
 * e.g. Mailtrap in dev) using {@link JavaMailSender}. Every message is rendered into the shared
 * {@code email/notification.html} template; any link found in the content is surfaced as a CTA button.
 * Replaces the logging stub for the EMAIL channel. SMS/WhatsApp remain logging stubs for now.
 */
@Component
@Slf4j
public class SmtpEmailGateway implements NotificationGateway {

    private static final Pattern URL = Pattern.compile("https?://\\S+");

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from:CathClau Edu <no-reply@cathclau.edu>}")
    private String from;

    @Value("${app.mail.brand-name:CathClau Edu}")
    private String brandName;

    public SmtpEmailGateway(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
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
            helper.setText(render(subject, content), true);
            mailSender.send(msg);
            log.info("[EMAIL] sent to {} | {}", destination, subject);
            return new DeliveryResult(true, "sent");
        } catch (Exception e) {
            log.error("[EMAIL] failed to {} | {} : {}", destination, subject, e.getMessage());
            return new DeliveryResult(false, e.getMessage());
        }
    }

    /** Renders the content into the branded HTML template, extracting the first URL as a CTA button. */
    private String render(String subject, String content) {
        String safe = content == null ? "" : content;
        Matcher m = URL.matcher(safe);
        String actionUrl = m.find() ? m.group() : null;

        String text = actionUrl != null ? safe.replace(actionUrl, "").trim() : safe;
        String bodyHtml = HtmlUtils.htmlEscape(text).replace("\n", "<br/>");

        Context ctx = new Context();
        ctx.setVariable("schoolName", brandName);
        ctx.setVariable("subject", subject);
        ctx.setVariable("bodyHtml", bodyHtml);
        ctx.setVariable("actionUrl", actionUrl);
        ctx.setVariable("actionLabel",
                actionUrl != null && actionUrl.contains("verify-email") ? "Activer mon compte" : "Ouvrir le lien");
        return templateEngine.process("email/notification", ctx);
    }
}
