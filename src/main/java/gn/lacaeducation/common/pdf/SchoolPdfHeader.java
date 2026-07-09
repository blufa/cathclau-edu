package gn.lacaeducation.common.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.common.TenantPublicInfo;
import lombok.extern.slf4j.Slf4j;

import java.awt.Color;

/**
 * En-tête officiel de l'école, partagé par tous les documents PDF (factures, reçus, bulletins,
 * fiches de paie, certificats…). Rend un bandeau aux couleurs de l'école (logo + nom + contacts)
 * suivi des mentions légales, puis renvoie la couleur de marque résolue pour harmoniser le reste
 * du document. Centralise l'ancien code dupliqué dans billing/payments/payroll.
 */
@Slf4j
public final class SchoolPdfHeader {

    public static final Color DEFAULT_BRAND = new Color(13, 148, 136);
    public static final Color GREY = new Color(110, 110, 110);
    public static final Color DARK = new Color(30, 41, 59);

    private SchoolPdfHeader() {}

    /** Renders the branded school header at the top of {@code doc} and returns the brand colour. */
    public static Color render(Document doc, TenantInfoProvider provider, Long tenantId) {
        TenantPublicInfo info = tenantId == null ? null : provider.findTenantInfo(tenantId).orElse(null);
        Color brand = parseColor(info == null ? null : info.brandColor());
        if (info == null) return brand;
        try {
            PdfPTable band = new PdfPTable(new float[]{1.1f, 4f});
            band.setWidthPercentage(100);
            band.setSpacingAfter(2f);

            PdfPCell logoCell = new PdfPCell();
            logoCell.setBackgroundColor(Color.WHITE);
            logoCell.setBorder(0);
            logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            logoCell.setPadding(8f);
            Image logo = loadLogo(info.logo());
            if (logo != null) {
                logo.scaleToFit(56, 56);
                logo.setAlignment(Element.ALIGN_MIDDLE);
                logoCell.addElement(logo);
            } else {
                Paragraph initials = new Paragraph(initials(info.schoolName()),
                        font(FontFactory.HELVETICA_BOLD, 20, brand));
                initials.setAlignment(Element.ALIGN_CENTER);
                logoCell.addElement(initials);
            }
            band.addCell(logoCell);

            PdfPCell nameCell = new PdfPCell();
            nameCell.setBackgroundColor(brand);
            nameCell.setBorder(0);
            nameCell.setPadding(11f);
            nameCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            nameCell.addElement(new Paragraph(safe(info.schoolName(), "École"),
                    font(FontFactory.HELVETICA_BOLD, 15, Color.WHITE)));
            String address = join(" · ", info.address(), info.city(), info.country());
            if (!address.isBlank()) {
                Paragraph a = new Paragraph(address, font(FontFactory.HELVETICA, 8.5f, Color.WHITE));
                a.setSpacingBefore(2f);
                nameCell.addElement(a);
            }
            String contact = join("   ", prefix("Tél : ", info.phone()), prefix("Email : ", info.email()), info.website());
            if (!contact.isBlank()) {
                nameCell.addElement(new Paragraph(contact, font(FontFactory.HELVETICA, 8.5f, Color.WHITE)));
            }
            band.addCell(nameCell);
            doc.add(band);

            String legal = join("      ",
                    prefix("Agrément : ", info.accreditationNumber()),
                    prefix("Autorisation d'ouverture : ", info.openingAuthorization()),
                    prefix("Directeur : ", info.director()));
            if (!legal.isBlank()) {
                Paragraph p = new Paragraph(legal, font(FontFactory.HELVETICA_OBLIQUE, 7.5f, new Color(120, 120, 120)));
                p.setSpacingBefore(3f);
                p.setSpacingAfter(6f);
                doc.add(p);
            }
        } catch (RuntimeException e) {
            log.warn("En-tête école non rendu pour le tenant {} : {}", tenantId, e.getMessage());
        }
        return brand;
    }

    public static Color parseColor(String hex) {
        try {
            if (hex == null || hex.isBlank()) return DEFAULT_BRAND;
            String h = hex.trim();
            if (h.startsWith("#")) h = h.substring(1);
            if (h.length() == 3) h = "" + h.charAt(0) + h.charAt(0) + h.charAt(1) + h.charAt(1) + h.charAt(2) + h.charAt(2);
            if (h.length() != 6) return DEFAULT_BRAND;
            return new Color(Integer.parseInt(h.substring(0, 2), 16),
                    Integer.parseInt(h.substring(2, 4), 16),
                    Integer.parseInt(h.substring(4, 6), 16));
        } catch (RuntimeException e) {
            return DEFAULT_BRAND;
        }
    }

    /** Lightens {@code c} toward white by factor {@code f} (0 = white, 1 = original). */
    public static Color tint(Color c, double f) {
        return new Color(
                (int) Math.round(255 + (c.getRed() - 255) * f),
                (int) Math.round(255 + (c.getGreen() - 255) * f),
                (int) Math.round(255 + (c.getBlue() - 255) * f));
    }

    public static Font font(String base, float size, Color color) {
        Font f = FontFactory.getFont(base, size);
        f.setColor(color);
        return f;
    }

    private static Image loadLogo(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            return Image.getInstance(new java.net.URI(url).toURL());
        } catch (Exception e) {
            log.warn("Logo école illisible ({}) : {}", url, e.getMessage());
            return null;
        }
    }

    private static String initials(String name) {
        if (name == null || name.isBlank()) return "EC";
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) { if (!p.isBlank()) sb.append(Character.toUpperCase(p.charAt(0))); if (sb.length() == 2) break; }
        return sb.length() == 0 ? "EC" : sb.toString();
    }

    private static String join(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isBlank()) continue;
            if (sb.length() > 0) sb.append(sep);
            sb.append(p);
        }
        return sb.toString();
    }

    private static String prefix(String prefix, String value) {
        return (value == null || value.isBlank()) ? "" : prefix + value;
    }

    private static String safe(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
