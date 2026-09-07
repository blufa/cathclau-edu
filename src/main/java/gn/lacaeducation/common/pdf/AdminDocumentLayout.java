package gn.lacaeducation.common.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import gn.lacaeducation.common.TenantPublicInfo;

import java.awt.Color;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Mise en page sobre partagée par les documents administratifs officiels remis en main propre
 * (reçu de paiement, facture, fiche de paie…) : en-tête noir sur blanc (logo + établissement, sans
 * bandeau de couleur), titre encadré, tableau d'informations "libellé : valeur", tableau de détail à
 * en-tête sombre, ligne de total surlignée d'un filet, et cadre de signature/cachet. Centralise le
 * style d'origine du reçu de paiement pour que tous les documents remis aux familles et au personnel
 * se ressemblent.
 */
public final class AdminDocumentLayout {

    public static final Color GREY = new Color(90, 90, 90);
    public static final Color DARK = new Color(20, 20, 20);
    public static final Color LINE = new Color(60, 60, 60);

    private AdminDocumentLayout() {}

    /** En-tête sobre (logo + nom de l'établissement + adresse + téléphone), sans bandeau coloré. */
    public static void header(Document doc, TenantPublicInfo info) {
        header(doc, info, null);
    }

    /** Comme {@link #header(Document, TenantPublicInfo)}, en ajoutant le bloc administratif officiel
     * (MEPU-A / IRE / DPE-DCE / DSSEE / République de Guinée / année) au-dessus — reproduit l'en-tête
     * des documents pédagogiques papier (référence : "Fiche de note 7eme.docx"). Délègue à
     * {@link SchoolPdfHeader} (bloc administratif + logo centré + nom d'établissement) pour éviter
     * une seconde implémentation divergente et un logo dupliqué. */
    public static void header(Document doc, TenantPublicInfo info, String academicYearLabel) {
        if (info != null) {
            SchoolPdfHeader.renderAdministrativeBlock(doc, info, academicYearLabel);
            SchoolPdfHeader.renderSchoolName(doc, info, DARK);
        } else {
            Paragraph name = new Paragraph("Établissement", font(FontFactory.HELVETICA_BOLD, 13, DARK));
            name.setAlignment(Element.ALIGN_CENTER);
            doc.add(name);
        }
        SchoolPdfHeader.renderSeparator(doc, LINE);
    }

    /** Titre encadré (boîte à bordure fine, sans remplissage coloré). */
    public static void titleBox(Document doc, String title) {
        PdfPCell c = new PdfPCell(new Phrase(title, font(FontFactory.HELVETICA_BOLD, 13, DARK)));
        c.setBorder(Rectangle.BOX);
        c.setBorderColor(LINE);
        c.setBorderWidth(1f);
        c.setPadding(8f);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingAfter(12f);
        t.addCell(c);
        doc.add(t);
    }

    /** Tableau de lignes "libellé : valeur" sans bordures visibles. */
    public static PdfPTable infoTable() {
        PdfPTable t = new PdfPTable(new float[]{1.3f, 2f});
        t.setWidthPercentage(100);
        return t;
    }

    public static void row(PdfPTable t, String label, String value) {
        PdfPCell l = new PdfPCell(new Phrase(label, font(FontFactory.HELVETICA_BOLD, 9.5f, DARK)));
        l.setBorder(0);
        l.setPaddingBottom(4f);
        PdfPCell v = new PdfPCell(new Phrase(": " + (value == null ? "-" : value), font(FontFactory.HELVETICA, 9.5f, DARK)));
        v.setBorder(0);
        v.setPaddingBottom(4f);
        t.addCell(l);
        t.addCell(v);
    }

    /** Tableau de détail à largeurs données, à remplir avec {@link #headerCell} puis {@link #cell}. */
    public static PdfPTable dataTable(float[] widths) {
        PdfPTable t = new PdfPTable(widths);
        t.setWidthPercentage(100);
        t.setSpacingBefore(12f);
        return t;
    }

    /** En-tête de colonne à fond sombre, texte blanc — même traitement que le reçu de paiement. */
    public static void headerCell(PdfPTable t, String label) {
        PdfPCell c = new PdfPCell(new Phrase(label, font(FontFactory.HELVETICA_BOLD, 9, Color.WHITE)));
        c.setBackgroundColor(DARK);
        c.setPadding(6f);
        c.setBorderColor(LINE);
        t.addCell(c);
    }

    public static void cell(PdfPTable t, String value, int align) {
        PdfPCell c = new PdfPCell(new Phrase(value == null ? "-" : value, font(FontFactory.HELVETICA, 9, DARK)));
        c.setPadding(6f);
        c.setBorderColor(LINE);
        c.setHorizontalAlignment(align);
        t.addCell(c);
    }

    /** Ligne de total : filet supérieur, libellé à gauche en gras, montant à droite en gras. */
    public static void totalRow(Document doc, String label, String amount) {
        PdfPTable t = new PdfPTable(new float[]{2f, 2f});
        t.setWidthPercentage(100);
        PdfPCell l = new PdfPCell(new Phrase(label, font(FontFactory.HELVETICA_BOLD, 10, DARK)));
        l.setBorder(Rectangle.TOP);
        l.setBorderColor(LINE);
        l.setPadding(7f);
        PdfPCell v = new PdfPCell(new Phrase(amount, font(FontFactory.HELVETICA_BOLD, 11, DARK)));
        v.setBorder(Rectangle.TOP);
        v.setBorderColor(LINE);
        v.setPadding(7f);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(l);
        t.addCell(v);
        doc.add(t);
    }

    /** Cadre de signature / cachet, aligné à droite (ex. "Signature et Cachet du Caissier"). */
    public static void signatureBox(Document doc, String label) {
        PdfPCell c = new PdfPCell(new Phrase(label, font(FontFactory.HELVETICA_BOLD, 9.5f, DARK)));
        c.setBorder(Rectangle.BOX);
        c.setBorderColor(LINE);
        c.setFixedHeight(70f);
        c.setPadding(8f);
        c.setVerticalAlignment(Element.ALIGN_TOP);
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(60);
        t.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.setSpacingBefore(22f);
        t.addCell(c);
        doc.add(t);
    }

    public static Font font(String base, float size, Color color) {
        Font f = FontFactory.getFont(base, size);
        f.setColor(color);
        return f;
    }

    public static String money(BigDecimal a) {
        if (a == null) return "0";
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.FRANCE);
        sym.setGroupingSeparator(' ');
        return new DecimalFormat("#,##0", sym).format(a);
    }

    public static String join(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isBlank()) continue;
            if (sb.length() > 0) sb.append(sep);
            sb.append(p);
        }
        return sb.toString();
    }

    public static String safe(String s) { return (s == null || s.isBlank()) ? "-" : s; }
}
