package gn.lacaeducation.documents.application.service;

import gn.lacaeducation.documents.domain.DocumentTemplate;
import gn.lacaeducation.documents.domain.PageFormat;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Moteur de modèles : substitue les variables {{cle}} dans l'en-tête / le corps / le pied, puis
 * compose un document HTML autoportant (branding, format A4/A5, marges, filigrane, logo/signature/
 * cachet). Le HTML produit est prévisualisé / imprimé / exporté (PDF, Word) côté navigateur.
 */
@Service
public class DocumentRenderService {

    private static final Pattern VAR = Pattern.compile("\\{\\{\\s*([\\w.]+)\\s*}}");
    private static final DateTimeFormatter FR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String render(DocumentTemplate t, Map<String, String> context) {
        String color = orDefault(t.getPrimaryColor(), "#1d4ed8");
        String font = orDefault(t.getFontFamily(), "system-ui, 'Segoe UI', Arial, sans-serif");

        Map<String, String> c = new HashMap<>();
        if (context != null) c.putAll(context);
        c.putIfAbsent("date", LocalDate.now().format(FR));
        c.putIfAbsent("couleur", color);
        // Logo : celui du modèle, sinon le logo de l'école (pour que tout document en porte un).
        c.putIfAbsent("logo", img(orDefault(t.getLogoUrl(), c.get("ecole.logo")), "max-height:64px"));
        c.putIfAbsent("signature", img(t.getSignatureUrl(), "max-height:80px"));
        c.putIfAbsent("cachet", img(t.getStampUrl(), "max-height:110px"));
        c.putIfAbsent("watermark", t.getWatermarkText() != null ? t.getWatermarkText() : "");

        String header = fill(nz(t.getHeaderHtml()), c);
        String body = fill(nz(t.getBodyHtml()), c);
        String footer = fill(nz(t.getFooterHtml()), c);

        String size = t.getPageFormat() == PageFormat.A5 ? "A5" : "A4";
        int mt = nz(t.getMarginTop(), 18), mr = nz(t.getMarginRight(), 16), mb = nz(t.getMarginBottom(), 18), ml = nz(t.getMarginLeft(), 16);
        String watermark = t.getWatermarkText() != null && !t.getWatermarkText().isBlank()
                ? "<div class=\"wm\">" + esc(t.getWatermarkText()) + "</div>" : "";

        return "<!doctype html><html lang=\"fr\"><head><meta charset=\"utf-8\">"
                + "<style>"
                + "@page{size:" + size + ";margin:" + mt + "mm " + mr + "mm " + mb + "mm " + ml + "mm}"
                + "*{box-sizing:border-box}body{font-family:" + font + ";color:#0f172a;margin:0;position:relative}"
                + ".accent,h1,h2{color:" + color + "}a{color:" + color + "}"
                + ".doc-header{margin-bottom:18px}"
                + ".doc-footer{border-top:1px solid #cbd5e1;margin-top:24px;padding-top:8px;font-size:11px;color:#64748b}"
                + "table{width:100%;border-collapse:collapse}th,td{border:1px solid #cbd5e1;padding:6px;font-size:12px}"
                + ".wm{position:fixed;inset:0;display:flex;align-items:center;justify-content:center;"
                + "font-size:90px;color:" + color + ";opacity:.07;transform:rotate(-30deg);pointer-events:none;font-weight:800}"
                + "@media print{.no-print{display:none}}"
                + "</style></head><body>" + watermark
                + "<div class=\"doc-header\">" + header + "</div>"
                + "<div class=\"doc-body\">" + body + "</div>"
                + "<div class=\"doc-footer\">" + footer + "</div>"
                + "</body></html>";
    }

    private String fill(String tpl, Map<String, String> c) {
        Matcher m = VAR.matcher(tpl);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String key = m.group(1);
            m.appendReplacement(sb, Matcher.quoteReplacement(c.getOrDefault(key, "")));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String img(String url, String style) {
        return url != null && !url.isBlank() ? "<img src=\"" + url + "\" style=\"" + style + "\"/>" : "";
    }
    private static String esc(String v) { return v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }
    private static String nz(String v) { return v != null ? v : ""; }
    private static int nz(Integer v, int d) { return v != null ? v : d; }
    private static String orDefault(String v, String d) { return v != null && !v.isBlank() ? v : d; }
}
