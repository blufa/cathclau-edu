package gn.lacaeducation.grading.adapters.out.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import gn.lacaeducation.common.TenantPublicInfo;
import gn.lacaeducation.common.pdf.SchoolPdfHeader;

import java.awt.Color;
import java.util.Locale;

/**
 * En-tête spécifique au bulletin (§retour fondateur, fidèle à l'entête du bulletin témoin) — reprend
 * le bloc ministère/logo/République du reçu ({@link SchoolPdfHeader#renderAdministrativeBlock}) mais,
 * contrairement à ce dernier, SANS année scolaire ni téléphone dans ce bloc, nom de l'établissement en
 * MAJUSCULES, et la devise de l'école à la place de l'adresse en dessous — demandes explicites qui ne
 * doivent pas changer l'en-tête des autres documents (reçus, factures…), d'où une implémentation
 * séparée plutôt qu'une modification de {@link SchoolPdfHeader}.
 */
final class BulletinHeader {

    private static final Color GREY = new Color(110, 110, 110);
    private static final Color DARK = new Color(30, 41, 59);
    private static final String MINISTRY = "MEPU-A";
    private static final String REPUBLIC = "RÉPUBLIQUE DE GUINÉE";
    private static final String MOTTO = "Travail — Justice — Solidarité";

    private BulletinHeader() {}

    static void render(Document doc, TenantPublicInfo info, Color accent) {
        if (info == null) return;

        PdfPTable admin = new PdfPTable(new float[]{2f, 1.2f, 2f});
        admin.setWidthPercentage(100);
        admin.setSpacingAfter(4f);

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
        Image logo = SchoolPdfHeader.loadLogo(info.logo());
        if (logo != null) {
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
        // Pas d'année scolaire ni de téléphone ici (demande explicite) — l'année figure déjà dans le
        // bandeau "Année Scolaire : ..." sous le titre du bulletin, le téléphone n'a pas sa place ici.
        admin.addCell(right);
        doc.add(admin);

        Paragraph name = new Paragraph(
                safe(info.schoolName(), "École").toUpperCase(Locale.FRENCH),
                font(FontFactory.HELVETICA_BOLD, 15, accent));
        name.setAlignment(Element.ALIGN_CENTER);
        name.setSpacingBefore(2f);
        doc.add(name);
        if (info.motto() != null && !info.motto().isBlank()) {
            Paragraph schoolMotto = new Paragraph("« " + info.motto() + " »",
                    font(FontFactory.HELVETICA_OBLIQUE, 9, GREY));
            schoolMotto.setAlignment(Element.ALIGN_CENTER);
            doc.add(schoolMotto);
        }

        SchoolPdfHeader.renderSeparator(doc, accent);
    }

    private static Font font(String base, float size, Color color) {
        Font f = FontFactory.getFont(base, size);
        f.setColor(color);
        return f;
    }

    private static String safe(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
