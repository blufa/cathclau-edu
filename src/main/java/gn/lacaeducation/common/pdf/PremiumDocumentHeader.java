package gn.lacaeducation.common.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import gn.lacaeducation.common.TenantPublicInfo;

import java.awt.Color;

/**
 * En-tête commun aux documents "design premium" en couleur de marque personnalisable (fiche de paie,
 * reçu de paiement — §retour fondateur, /features/document-school/*.dc.html) : logo/nom d'établissement/
 * adresse/téléphone à gauche, République de Guinée/devise/ministère à droite. Byte pour byte identique
 * entre ces deux documents dans la référence HTML, d'où ce partage (contrairement au bulletin/tableau
 * d'honneur, §grading, qui ont leur propre en-tête à palette fixe — voir {@code BulletinHeader}).
 */
public final class PremiumDocumentHeader {

    public static final Color DARK_TEXT = new Color(19, 38, 31);
    public static final Color TEXT_GREY = new Color(94, 107, 101);

    private static final com.lowagie.text.pdf.BaseFont SANS_BOLD = DesignFonts.SANS_SEMIBOLD;
    private static final com.lowagie.text.pdf.BaseFont SERIF_BOLD = DesignFonts.SERIF_BOLD;
    private static final com.lowagie.text.pdf.BaseFont SERIF_ITALIC = DesignFonts.SERIF_SEMIBOLD_ITALIC;
    private static final com.lowagie.text.pdf.BaseFont SANS = DesignFonts.SANS;

    private PremiumDocumentHeader() {}

    public static void render(Document doc, TenantPublicInfo info) {
        PdfPTable t = new PdfPTable(new float[]{1.3f, 1f});
        t.setWidthPercentage(100);
        t.setSpacingAfter(mm(3));

        PdfPCell left = new PdfPCell();
        left.setBorder(0);
        left.setVerticalAlignment(Element.ALIGN_TOP);
        PdfPTable leftInner = new PdfPTable(new float[]{1f, 3f});
        leftInner.setWidthPercentage(100);
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(0);
        logoCell.setFixedHeight(mm(19));
        logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        // Pas de fond/bordure derrière le logo (demande utilisateur) — le logo flotte directement sur
        // le fond crème de la page, sans boîte blanche qui tranche visuellement une fois un vrai logo
        // (souvent déjà sur fond blanc/transparent) en place.
        Image logo = info != null ? SchoolPdfHeader.loadLogo(info.logo()) : null;
        if (logo != null) {
            logo.scaleToFit(mm(16), mm(16));
            logo.setAlignment(Element.ALIGN_MIDDLE);
            logoCell.addElement(logo);
        }
        leftInner.addCell(logoCell);
        PdfPCell nameCell = new PdfPCell();
        nameCell.setBorder(0);
        nameCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        nameCell.setPaddingLeft(mm(4));
        Paragraph name = new Paragraph(safe(info != null ? info.schoolName() : null, "École"),
                font(SERIF_BOLD, FontFactory.TIMES_BOLD, 18, DARK_TEXT));
        name.setLeading(16f);
        nameCell.addElement(name);
        String addr = info != null ? join(" · ", info.address(), info.city(), "Guinée") : "";
        if (!addr.isBlank()) {
            Paragraph a = new Paragraph(addr, font(SANS, FontFactory.HELVETICA, 8, TEXT_GREY));
            a.setSpacingBefore(1.5f);
            nameCell.addElement(a);
        }
        String phones = info != null ? join(" / ", info.phone(), info.phone2()) : "";
        if (!phones.isBlank()) {
            Paragraph ph = new Paragraph("Tél : " + phones, font(SANS, FontFactory.HELVETICA, 8, TEXT_GREY));
            ph.setSpacingBefore(0.5f);
            nameCell.addElement(ph);
        }
        leftInner.addCell(nameCell);
        left.addElement(leftInner);
        t.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(0);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        right.setVerticalAlignment(Element.ALIGN_TOP);
        Paragraph republic = new Paragraph("RÉPUBLIQUE DE GUINÉE", font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 8.5f, DARK_TEXT));
        republic.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(republic);
        Paragraph motto = new Paragraph("Travail — Justice — Solidarité", font(SERIF_ITALIC, FontFactory.TIMES_BOLDITALIC, 10, TEXT_GREY));
        motto.setAlignment(Element.ALIGN_RIGHT);
        motto.setSpacingBefore(1f);
        right.addElement(motto);
        String ireDpe = join(" · ", prefix("IRE : ", info != null ? info.ire() : null), prefix("DPE/DCE : ", info != null ? info.dpeLabel() : null));
        String sousPref = prefix("DSSEE : ", info != null ? info.sousPrefectureLabel() : null);
        Paragraph adminP = new Paragraph("MEPU-A", font(SANS, FontFactory.HELVETICA, 7, TEXT_GREY));
        adminP.setAlignment(Element.ALIGN_RIGHT);
        adminP.setLeading(9.5f);
        adminP.setSpacingBefore(2f);
        if (!ireDpe.isBlank()) adminP.add("\n" + ireDpe);
        if (!sousPref.isBlank()) adminP.add("\n" + sousPref);
        right.addElement(adminP);
        t.addCell(right);

        doc.add(t);
    }

    private static com.lowagie.text.Font font(com.lowagie.text.pdf.BaseFont base, String fallbackFamily, float size, Color color) {
        if (base != null) {
            com.lowagie.text.Font f = new com.lowagie.text.Font(base, size);
            f.setColor(color);
            return f;
        }
        return FontFactory.getFont(fallbackFamily, size, color);
    }

    static float mm(float v) { return v * 72f / 25.4f; }

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
        return value == null || value.isBlank() ? "" : prefix + value;
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
