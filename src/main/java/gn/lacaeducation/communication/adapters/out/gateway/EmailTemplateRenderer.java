package gn.lacaeducation.communication.adapters.out.gateway;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rendu HTML partagé des e-mails (branding, gabarit {@code email/notification.html}) — utilisé par
 * tous les gateways EMAIL (SMTP, Resend) pour éviter de dupliquer la mise en forme.
 */
@Component
@RequiredArgsConstructor
public class EmailTemplateRenderer {

    private static final Pattern URL = Pattern.compile("https?://\\S+");

    private final TemplateEngine templateEngine;

    @Value("${app.mail.brand-name:CathClau Edu}")
    private String brandName;

    /** Renders the content into the branded HTML template, extracting the first URL as a CTA button. */
    public String render(String subject, String content) {
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
