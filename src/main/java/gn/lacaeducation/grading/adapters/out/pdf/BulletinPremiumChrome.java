package gn.lacaeducation.grading.adapters.out.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import gn.lacaeducation.common.TenantPublicInfo;
import gn.lacaeducation.common.pdf.DesignFonts;
import gn.lacaeducation.common.pdf.RoundedCellBorder;
import gn.lacaeducation.common.pdf.SchoolPdfHeader;

import java.awt.Color;
import java.math.BigDecimal;
import java.util.Locale;

/**
 * Habillage visuel premium du bulletin officiel ({@code OpenPdfReportCardGenerator}) — en-tête
 * ministère/logo/école, titre encadré de filets or, cartes BILAN (moyenne + RANG) — extrait ici
 * pour que le bulletin mensuel (§censeur, "ça doit être le même bulletin que le bulletin normal")
 * puisse réutiliser exactement le même habillage sans dupliquer la palette/les polices. Volontairement
 * un fichier à part (pas une modification de {@code OpenPdfReportCardGenerator}, document officiel
 * déjà en production) : aucun risque de régression sur le bulletin officiel.
 */
public final class BulletinPremiumChrome {

    public static final Color DARK_GREEN = new Color(14, 77, 58);
    public static final Color GOLD = new Color(184, 137, 43);
    public static final Color BORDER = new Color(217, 210, 192);
    public static final Color LABEL_GREY = new Color(138, 122, 85);
    public static final Color TEXT_GREY = new Color(94, 107, 101);
    public static final Color DARK_TEXT = new Color(19, 38, 31);
    public static final Color GOLD_LIGHT_TEXT = new Color(233, 215, 158);
    public static final Color WHITE = Color.WHITE;
    public static final Color FOOTER_BORDER = new Color(226, 220, 205);

    private static final BaseFont SANS = DesignFonts.SANS;
    private static final BaseFont SANS_BOLD = DesignFonts.SANS_SEMIBOLD;
    private static final BaseFont SANS_EXTRABOLD = DesignFonts.SANS_EXTRABOLD;
    private static final BaseFont SERIF_BOLD = DesignFonts.SERIF_BOLD;
    private static final BaseFont SERIF_ITALIC = DesignFonts.SERIF_SEMIBOLD_ITALIC;

    private BulletinPremiumChrome() {}

    // ── En-tête : ministère/logo/école (gauche) + République/devise/admin (droite) ──
    public static void header(Document doc, TenantPublicInfo info, String schoolYear) {
        PdfPTable t = new PdfPTable(new float[]{1.3f, 1f});
        t.setWidthPercentage(100);
        t.setSpacingAfter(mm(3.8f));

        PdfPCell left = new PdfPCell();
        left.setBorder(0);
        left.setVerticalAlignment(Element.ALIGN_TOP);
        PdfPTable leftInner = new PdfPTable(new float[]{1f, 3f});
        leftInner.setWidthPercentage(100);
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(0);
        logoCell.setFixedHeight(mm(21));
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logoCell.setCellEvent(RoundedCellBorder.fillAndBorder(mm(2), WHITE, BORDER, 0.8f));
        Image logo = info != null ? SchoolPdfHeader.loadLogo(info.logo()) : null;
        if (logo != null) {
            logo.scaleToFit(mm(18), mm(18));
            logo.setAlignment(Element.ALIGN_MIDDLE);
            logoCell.addElement(logo);
        }
        leftInner.addCell(logoCell);
        PdfPCell nameCell = new PdfPCell();
        nameCell.setBorder(0);
        nameCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        nameCell.setPaddingLeft(mm(4));
        Paragraph name = new Paragraph(safe(info != null ? info.schoolName() : null, "École"), font(SERIF_BOLD, FontFactory.TIMES_BOLD, 15, DARK_GREEN));
        name.setLeading(13f);
        nameCell.addElement(name);
        if (schoolYear != null && !schoolYear.isBlank()) {
            Paragraph year = new Paragraph("Année Scolaire : " + schoolYear, font(SANS, FontFactory.HELVETICA, 8, TEXT_GREY));
            year.setSpacingBefore(2f);
            nameCell.addElement(year);
        }
        leftInner.addCell(nameCell);
        left.addElement(leftInner);
        t.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(0);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        right.setVerticalAlignment(Element.ALIGN_TOP);
        Paragraph republic = new Paragraph("RÉPUBLIQUE DE GUINÉE", font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 9, DARK_TEXT));
        republic.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(republic);
        Paragraph motto = new Paragraph("Travail — Justice — Solidarité", font(SERIF_ITALIC, FontFactory.TIMES_BOLDITALIC, 10.5f, TEXT_GREY));
        motto.setAlignment(Element.ALIGN_RIGHT);
        motto.setSpacingBefore(1f);
        right.addElement(motto);
        right.addElement(flagBars());
        String ireDpe = join(" · ", prefix("IRE : ", info != null ? info.ire() : null), prefix("DPE/DCE : ", info != null ? info.dpeLabel() : null));
        String sousPref = prefix("DSSEE : ", info != null ? info.sousPrefectureLabel() : null);
        Paragraph adminP = new Paragraph("MEPU-A", font(SANS, FontFactory.HELVETICA, 7.5f, TEXT_GREY));
        adminP.setAlignment(Element.ALIGN_RIGHT);
        adminP.setLeading(10.5f);
        adminP.setSpacingBefore(2f);
        if (!ireDpe.isBlank()) adminP.add("\n" + ireDpe);
        if (!sousPref.isBlank()) adminP.add("\n" + sousPref);
        right.addElement(adminP);
        t.addCell(right);

        doc.add(t);
    }

    private static PdfPTable flagBars() {
        PdfPTable bars = new PdfPTable(new float[]{1f, 1f, 1f});
        bars.setWidthPercentage(23f);
        bars.setHorizontalAlignment(Element.ALIGN_RIGHT);
        bars.getDefaultCell().setBorder(0);
        bars.setSpacingBefore(2f);
        bars.setSpacingAfter(2f);
        for (Color c : new Color[]{new Color(206, 17, 38), new Color(252, 209, 22), new Color(0, 148, 96)}) {
            PdfPCell cell = new PdfPCell();
            cell.setBorder(0);
            cell.setFixedHeight(mm(1));
            cell.setCellEvent(RoundedCellBorder.fillOnly(0, c));
            bars.addCell(cell);
        }
        return bars;
    }

    // ── Titre centré encadré de filets or ──────────────────────────────────────────
    public static void titleBar(Document doc, String title) {
        PdfPTable t = new PdfPTable(new float[]{1f, 0f, 1f});
        t.setWidthPercentage(100);
        t.setSpacingAfter(mm(3.8f));
        t.getDefaultCell().setBorder(0);

        PdfPCell line1 = new PdfPCell();
        line1.setBorder(Rectangle.BOTTOM);
        line1.setBorderColorBottom(GOLD);
        line1.setBorderWidthBottom(0.8f);
        line1.setFixedHeight(mm(10));
        t.addCell(line1);

        PdfPCell titleCell = new PdfPCell(new Phrase(title, font(SERIF_BOLD, FontFactory.TIMES_BOLD, 17, DARK_GREEN)));
        titleCell.setBorder(0);
        titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        titleCell.setPadding(0);
        titleCell.setPaddingLeft(mm(3));
        titleCell.setPaddingRight(mm(3));
        titleCell.setNoWrap(true);
        t.addCell(titleCell);

        PdfPCell line2 = new PdfPCell();
        line2.setBorder(Rectangle.BOTTOM);
        line2.setBorderColorBottom(GOLD);
        line2.setBorderWidthBottom(0.8f);
        line2.setFixedHeight(mm(10));
        t.addCell(line2);

        doc.add(t);
    }

    public static void sectionLabel(Document doc, String text) {
        Paragraph p = new Paragraph(text, font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 7.5f, DARK_GREEN));
        p.setSpacingAfter(mm(1.8f));
        doc.add(p);
    }

    public static PdfPTable cardRow(int cardCount) {
        float[] widths = new float[cardCount * 2 - 1];
        for (int i = 0; i < widths.length; i++) widths[i] = (i % 2 == 0) ? 1f : 0.08f;
        PdfPTable t = new PdfPTable(widths);
        t.setWidthPercentage(100);
        t.setSpacingAfter(mm(3));
        return t;
    }

    public static void spacerCell(PdfPTable row) {
        PdfPCell spacer = new PdfPCell();
        spacer.setBorder(0);
        row.addCell(spacer);
    }

    public static void addCard(PdfPTable row, String label, String value, String unit, String mention, boolean highlighted, boolean first) {
        if (!first) spacerCell(row);
        PdfPCell c = new PdfPCell();
        c.setPadding(mm(2.5f));
        c.setPaddingLeft(mm(3.2f));
        c.setBorder(0);
        c.setCellEvent(highlighted
                ? RoundedCellBorder.fillOnly(mm(2.5f), DARK_GREEN)
                : RoundedCellBorder.fillAndBorder(mm(2.5f), WHITE, BORDER, 0.8f));
        Color labelColor = highlighted ? GOLD_LIGHT_TEXT : LABEL_GREY;
        Color valueColor = highlighted ? WHITE : DARK_TEXT;
        Color unitColor = highlighted ? GOLD_LIGHT_TEXT : TEXT_GREY;
        Color mentionColor = highlighted ? GOLD_LIGHT_TEXT : DARK_GREEN;
        Paragraph l = new Paragraph(label.toUpperCase(Locale.FRENCH), font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 6.5f, labelColor));
        c.addElement(l);
        PdfPTable valueRow = new PdfPTable(new float[]{2.1f, 1.2f});
        valueRow.setWidthPercentage(100);
        valueRow.setSpacingBefore(2f);
        PdfPCell valCell = new PdfPCell();
        valCell.setBorder(0);
        valCell.setPadding(0);
        valCell.setNoWrap(true);
        Phrase valPhrase = new Phrase();
        valPhrase.add(new com.lowagie.text.Chunk(value, font(SANS_EXTRABOLD, FontFactory.HELVETICA_BOLD, 14, valueColor)));
        valPhrase.add(new com.lowagie.text.Chunk("  " + unit, font(SANS, FontFactory.HELVETICA, 8, unitColor)));
        valCell.addElement(new Paragraph(valPhrase));
        valueRow.addCell(valCell);
        PdfPCell mentionCell = new PdfPCell(new Phrase(mention, font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 8, mentionColor)));
        mentionCell.setBorder(0);
        mentionCell.setPadding(0);
        mentionCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        mentionCell.setVerticalAlignment(Element.ALIGN_BOTTOM);
        valueRow.addCell(mentionCell);
        c.addElement(valueRow);
        row.addCell(c);
    }

    public static void addRankCard(PdfPTable row, Integer rank, Integer classSize, boolean first) {
        if (!first) spacerCell(row);
        PdfPCell c = new PdfPCell();
        c.setPadding(mm(2.5f));
        c.setPaddingLeft(mm(3.2f));
        c.setBorder(0);
        c.setCellEvent(RoundedCellBorder.fillAndBorder(mm(2.5f), WHITE, BORDER, 0.8f));
        c.addElement(new Paragraph("RANG", font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 6.5f, LABEL_GREY)));
        Phrase p = new Phrase();
        p.add(new com.lowagie.text.Chunk(rank != null ? rank + "e" : "—", font(SANS_EXTRABOLD, FontFactory.HELVETICA_BOLD, 14, DARK_TEXT)));
        p.add(new com.lowagie.text.Chunk(" / " + (classSize != null ? classSize : "—") + " él.", font(SANS, FontFactory.HELVETICA, 8, TEXT_GREY)));
        Paragraph pp = new Paragraph(p);
        pp.setSpacingBefore(2f);
        c.addElement(pp);
        row.addCell(c);
    }

    public static com.lowagie.text.Font font(BaseFont base, String fallbackFamily, float size, Color color) {
        if (base != null) {
            com.lowagie.text.Font f = new com.lowagie.text.Font(base, size);
            f.setColor(color);
            return f;
        }
        return FontFactory.getFont(fallbackFamily, size, color);
    }

    /** mm → points (72pt/pouce, 25.4mm/pouce). */
    public static float mm(float v) {
        return v * 72f / 25.4f;
    }

    public static String dec(BigDecimal value) {
        return value == null ? "—" : value.toPlainString();
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

    public static String prefix(String prefix, String value) {
        return value == null || value.isBlank() ? "" : prefix + value;
    }

    public static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
