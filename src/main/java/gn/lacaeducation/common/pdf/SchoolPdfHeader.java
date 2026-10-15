package gn.lacaeducation.common.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.common.TenantPublicInfo;
import lombok.extern.slf4j.Slf4j;

import java.awt.Color;

/**
 * En-tête officiel de l'école, partagé par tous les documents PDF (factures, reçus, bulletins,
 * fiches de paie, certificats…) — reproduit la structure des documents administratifs papier
 * guinéens (référence : "Fiche de note 7eme.docx") : ministère/circonscription scolaire à gauche,
 * République de Guinée/année/contact à droite, puis nom de l'établissement et titre du document
 * centrés. Centralise l'ancien code dupliqué dans billing/payments/payroll.
 */
@Slf4j
public final class SchoolPdfHeader {

    public static final Color DEFAULT_BRAND = new Color(13, 148, 136);
    public static final Color GREY = new Color(110, 110, 110);
    public static final Color DARK = new Color(30, 41, 59);

    private static final String MINISTRY = "MEPU-A";
    private static final String REPUBLIC = "RÉPUBLIQUE DE GUINÉE";
    private static final String MOTTO = "Travail — Justice — Solidarité";

    private SchoolPdfHeader() {}

    /** Renders the branded school header at the top of {@code doc} and returns the brand colour. */
    public static Color render(Document doc, TenantInfoProvider provider, Long tenantId, String documentTitle) {
        return render(doc, provider, tenantId, documentTitle, null);
    }

    /**
     * @param documentTitle nom du document affiché sous le nom de l'école (ex. "FICHE DE NOTE",
     *                       "REÇU DE PAIEMENT", "BULLETIN") — jamais nul en pratique, chaque
     *                       générateur doit préciser le sien.
     * @param academicYearLabel année scolaire affichée dans le bloc République (ex. "2025-2026") ;
     *                          omise si non fournie.
     */
    public static Color render(Document doc, TenantInfoProvider provider, Long tenantId,
                               String documentTitle, String academicYearLabel) {
        TenantPublicInfo info = tenantId == null ? null : provider.findTenantInfo(tenantId).orElse(null);
        Color brand = parseColor(info == null ? null : info.brandColor());
        if (info == null) return brand;
        try {
            renderAdministrativeBlock(doc, info, academicYearLabel);
            renderSchoolName(doc, info, brand);
            renderSeparator(doc, brand);
            if (documentTitle != null && !documentTitle.isBlank()) {
                PdfPTable titleBar = new PdfPTable(1);
                titleBar.setWidthPercentage(100);
                titleBar.setSpacingBefore(6f);
                titleBar.setSpacingAfter(8f);
                PdfPCell cell = new PdfPCell(new Phrase(documentTitle.toUpperCase(), font(FontFactory.HELVETICA_BOLD, 12, Color.WHITE)));
                cell.setBackgroundColor(brand);
                cell.setPadding(6f);
                cell.setBorder(0);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                titleBar.addCell(cell);
                doc.add(titleBar);
            }
            String legal = join("      ",
                    prefix("Agrément : ", info.accreditationNumber()),
                    prefix("Autorisation d'ouverture : ", info.openingAuthorization()),
                    prefix("Directeur : ", info.director()));
            if (!legal.isBlank()) {
                Paragraph p = new Paragraph(legal, font(FontFactory.HELVETICA_OBLIQUE, 7.5f, new Color(120, 120, 120)));
                p.setSpacingAfter(6f);
                doc.add(p);
            }
        } catch (RuntimeException e) {
            log.warn("En-tête école non rendu pour le tenant {} : {}", tenantId, e.getMessage());
        }
        return brand;
    }

    /** Bloc administratif trois colonnes, reproduisant l'en-tête officiel des documents pédagogiques
     * papier (référence : "Fiche de note 7eme.docx") : hiérarchie ministérielle à gauche, logo de
     * l'établissement centré, République/devise/année/contact à droite. Chaque ligne optionnelle
     * (IRE/DPE/sous-préfecture) n'apparaît que si renseignée dans les paramètres école.
     * Public : réutilisé par {@link AdminDocumentLayout#header} pour les documents sobres (reçus,
     * factures, fiches de paie) afin d'éviter deux implémentations divergentes. */
    public static void renderAdministrativeBlock(Document doc, TenantPublicInfo info, String academicYearLabel) {
        PdfPTable admin = new PdfPTable(new float[]{2f, 1.2f, 2f});
        admin.setWidthPercentage(100);
        admin.setSpacingAfter(2f);

        Font small = font(FontFactory.HELVETICA, 8f, DARK);
        Font smallBold = font(FontFactory.HELVETICA_BOLD, 8f, DARK);

        PdfPCell left = new PdfPCell();
        left.setBorder(0);
        left.setVerticalAlignment(Element.ALIGN_MIDDLE);
        left.addElement(new Paragraph(MINISTRY, smallBold));
        if (info.ire() != null && !info.ire().isBlank()) left.addElement(new Paragraph("IRE : " + info.ire(), small));
        if (info.dpeLabel() != null && !info.dpeLabel().isBlank()) left.addElement(new Paragraph("DPE/DCE : " + info.dpeLabel(), small));
        if (info.sousPrefectureLabel() != null && !info.sousPrefectureLabel().isBlank()) {
            left.addElement(new Paragraph("DSSEE : " + info.sousPrefectureLabel(), small));
        }
        admin.addCell(left);

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(0);
        logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Image logo = loadLogo(info.logo());
        if (logo != null) {
            // Retour comptable : logo trop petit sur le reçu — agrandi une 2e fois (66 → 90).
            logo.scaleToFit(90, 90);
            logo.setAlignment(Element.ALIGN_MIDDLE);
            logoCell.addElement(logo);
        }
        admin.addCell(logoCell);

        PdfPCell right = new PdfPCell();
        right.setBorder(0);
        right.setVerticalAlignment(Element.ALIGN_MIDDLE);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph republic = new Paragraph(REPUBLIC, smallBold);
        republic.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(republic);
        Paragraph motto = new Paragraph(MOTTO, font(FontFactory.HELVETICA_OBLIQUE, 7.5f, GREY));
        motto.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(motto);
        if (academicYearLabel != null && !academicYearLabel.isBlank()) {
            Paragraph year = new Paragraph(academicYearLabel, small);
            year.setAlignment(Element.ALIGN_RIGHT);
            right.addElement(year);
        }
        String phones = join(" / ", info.phone(), info.phone2());
        String contact = join("  /  ", prefix("Tél : ", phones.isBlank() ? null : phones), prefix("Email : ", info.email()));
        if (!contact.isBlank()) {
            Paragraph c = new Paragraph(contact, small);
            c.setAlignment(Element.ALIGN_RIGHT);
            right.addElement(c);
        }
        admin.addCell(right);
        doc.add(admin);
    }

    /** Nom de l'établissement centré (pleine largeur), avec adresse en dessous si renseignée — sous
     * le bloc administratif (ministère | logo | République). Public : réutilisé par
     * {@link AdminDocumentLayout#header} pour éviter une seconde implémentation divergente
     * (et un logo dupliqué, déjà affiché par {@link #renderAdministrativeBlock}). */
    public static void renderSchoolName(Document doc, TenantPublicInfo info, Color brand) {
        Paragraph name = new Paragraph(safe(info.schoolName(), "École"), font(FontFactory.HELVETICA_BOLD, 14, brand));
        name.setAlignment(Element.ALIGN_CENTER);
        name.setSpacingBefore(2f);
        doc.add(name);
        if (info.motto() != null && !info.motto().isBlank()) {
            Paragraph schoolMotto = new Paragraph("« " + info.motto() + " »",
                    font(FontFactory.HELVETICA_OBLIQUE, 8.5f, GREY));
            schoolMotto.setAlignment(Element.ALIGN_CENTER);
            doc.add(schoolMotto);
        }
        String address = join(" · ", info.address(), info.city(), info.country());
        if (!address.isBlank()) {
            Paragraph a = new Paragraph(address, font(FontFactory.HELVETICA, 8.5f, GREY));
            a.setAlignment(Element.ALIGN_CENTER);
            doc.add(a);
        }
    }

    /** Ligne de séparation fine à la couleur de l'établissement — sépare visuellement le bloc
     * d'identité (ministère/logo/République/école) du titre du document et de son contenu. */
    public static void renderSeparator(Document doc, Color color) {
        PdfPTable rule = new PdfPTable(1);
        rule.setWidthPercentage(100);
        rule.setSpacingBefore(4f);
        PdfPCell line = new PdfPCell();
        line.setBorder(Rectangle.TOP);
        line.setBorderColor(color);
        line.setBorderWidthTop(1.5f);
        line.setFixedHeight(2f);
        rule.addCell(line);
        doc.add(rule);
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

    /** Charge le logo de l'école depuis son URL (Cloudinary…) ; null si absent/illisible. */
    public static Image loadLogo(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            return Image.getInstance(new java.net.URI(url).toURL());
        } catch (Exception e) {
            log.warn("Logo école illisible ({}) : {}", url, e.getMessage());
            return null;
        }
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
