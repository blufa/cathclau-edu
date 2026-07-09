package gn.lacaeducation.notification.adapters.out.email;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

@Component @RequiredArgsConstructor @Slf4j
public class EmailService {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    /** Espacement minimal entre deux envois (anti rate-limit fournisseur, ex. Mailtrap « too many emails/sec »). */
    @Value("${app.mail.min-interval-ms:1100}")
    private long minIntervalMs;

    /** Expéditeur des emails (obligatoire pour la plupart des SMTP réels). Configurable par env APP_MAIL_FROM. */
    @Value("${app.mail.from:CathClau Edu <no-reply@cathclau-edu.com>}")
    private String from;

    private static final Object SEND_LOCK = new Object();
    private static long lastSentAt = 0L;

    /**
     * Envoi asynchrone (ne bloque pas la requête) et étalé dans le temps pour rester sous la limite
     * de débit du fournisseur SMTP ; un retry unique couvre le throttling transitoire (550).
     */
    @Async
    public void send(String to, String subject, String template, Map<String, Object> variables) {
        final String html;
        try {
            Context ctx = new Context(Locale.FRENCH);
            ctx.setVariables(variables);
            html = templateEngine.process(template, ctx);
        } catch (Exception e) {
            log.error("Email template error ({}) for {}: {}", template, to, e.getMessage());
            return;
        }
        if (!trySend(to, subject, html)) {
            // Retry unique après une pause (cas « too many emails per second »).
            sleep(2000);
            if (!trySend(to, subject, html)) {
                log.error("Email abandonné après retry pour {} – {}", to, subject);
            }
        }
    }

    private boolean trySend(String to, String subject, String html) {
        throttle();
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(msg);
            log.info("Email sent to {} – {}", to, subject);
            return true;
        } catch (Exception e) {
            log.warn("Failed to send email to {} – {}: {}", to, subject, e.getMessage());
            return false;
        }
    }

    /** Sérialise les envois et garantit un délai minimal entre eux (rate limiting global). */
    private void throttle() {
        synchronized (SEND_LOCK) {
            long wait = lastSentAt + minIntervalMs - System.currentTimeMillis();
            if (wait > 0) sleep(wait);
            lastSentAt = System.currentTimeMillis();
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
