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
    public static final Color BORDER_LIGHT = new Color(234, 228, 214);
    public static final Color BORDER_LIGHTER = new Color(239, 234, 223);
    public static final Color ROW_STRIPE = new Color(252, 250, 244);
    public static final Color TOTAL_BG = new Color(241, 236, 223);
    public static final Color RESULT_TINT = new Color(251, 244, 226);
    public static final Color RESULT_TOTAL_TINT = new Color(243, 230, 196);

    private static final BaseFont SANS = DesignFonts.SANS;
    private static final BaseFont SANS_BOLD = DesignFonts.SANS_SEMIBOLD;
    private static final BaseFont SANS_EXTRABOLD = DesignFonts.SANS_EXTRABOLD;
    private static final BaseFont SERIF_BOLD = DesignFonts.SERIF_BOLD;
    private static final BaseFont SERIF_ITALIC = DesignFonts.SERIF_SEMIBOLD_ITALIC;
    private static final BaseFont MONO_BOLD = DesignFonts.MONO_SEMIBOLD;

    private BulletinPremiumChrome() {}

    // ── En-tête : ministère/logo/école (gauche) + République/devise/admin (droite) ──
    public static void header(Document doc, TenantPublicInfo info, String schoolYear) {
        PdfPTable t = new PdfPTable(new float[]{1.3f, 1f});
        t.setWidthPercentage(100);
        t.setSpacingAfter(mm(2.5f));

        PdfPCell left = new PdfPCell();
        left.setBorder(0);
        left.setVerticalAlignment(Element.ALIGN_TOP);
        PdfPTable leftInner = new PdfPTable(new float[]{1f, 3f});
        leftInner.setWidthPercentage(100);
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(0);
        logoCell.setFixedHeight(mm(16));
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
        t.setSpacingAfter(mm(2.2f));
        t.getDefaultCell().setBorder(0);

        PdfPCell line1 = new PdfPCell();
        line1.setBorder(Rectangle.BOTTOM);
        line1.setBorderColorBottom(GOLD);
        line1.setBorderWidthBottom(0.8f);
        line1.setFixedHeight(mm(6.5f));
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
        line2.setFixedHeight(mm(6.5f));
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
        t.setSpacingAfter(mm(1.8f));
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
        c.setPadding(mm(2f));
        c.setPaddingLeft(mm(2.6f));
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
        c.setPadding(mm(2f));
        c.setPaddingLeft(mm(2.6f));
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

    // ── Cellules de tableau de notes (fidèles à OpenPdfReportCardGenerator — copiées, pas partagées
    // par appel direct, pour ne jamais risquer de régression sur le bulletin officiel déjà en
    // production en touchant son fichier). ───────────────────────────────────────────────────────
    public static void headerCell(PdfPTable table, String text) {
        headerCell(table, text, mm(2.2f));
    }

    /** Variante avec un padding réglable — {@code OpenPdfMonthlyReportCardGenerator} utilise un
     * padding plus compact (§retour utilisateur : "il faut le contenir sur une seule page") que le
     * bulletin officiel, dont le padding par défaut (2.2mm) reste inchangé via {@link #headerCell}. */
    public static void headerCell(PdfPTable table, String text, float padding) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 8.5f, WHITE)));
        cell.setBackgroundColor(DARK_GREEN);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(padding);
        cell.setBorderColor(DARK_GREEN);
        table.addCell(cell);
    }

    public static void headerCellTint(PdfPTable table, String text, Color bg) {
        headerCellTint(table, text, bg, mm(2.2f));
    }

    public static void headerCellTint(PdfPTable table, String text, Color bg, float padding) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 8.5f, WHITE)));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(padding);
        cell.setBorderColor(bg);
        table.addCell(cell);
    }

    public static void bodyCell(PdfPTable table, String text, int alignment, Color bg, Color color, boolean bold) {
        bodyCell(table, text, alignment, bg, color, bold, mm(1.5f));
    }

    public static void bodyCell(PdfPTable table, String text, int alignment, Color bg, Color color, boolean bold, float padding) {
        PdfPCell cell = new PdfPCell(new Phrase(text, bold
                ? font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 9f, color)
                : font(SANS, FontFactory.HELVETICA, 9f, color)));
        cell.setHorizontalAlignment(alignment);
        cell.setBackgroundColor(bg);
        cell.setPadding(padding);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColorBottom(BORDER_LIGHTER);
        cell.setBorderWidthBottom(0.4f);
        table.addCell(cell);
    }

    public static void bodyCellMonoTint(PdfPTable table, String text, Color bg) {
        bodyCellMonoTint(table, text, bg, mm(1.5f));
    }

    public static void bodyCellMonoTint(PdfPTable table, String text, Color bg, float padding) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(MONO_BOLD, FontFactory.COURIER_BOLD, 9f, DARK_TEXT)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(bg);
        cell.setPadding(padding);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColorBottom(BORDER_LIGHTER);
        cell.setBorderWidthBottom(0.4f);
        table.addCell(cell);
    }

    public static void totalCell(PdfPTable table, String text, int alignment, int colspan, Color bg, Color color) {
        totalCell(table, text, alignment, colspan, bg, color, mm(2.2f));
    }

    public static void totalCell(PdfPTable table, String text, int alignment, int colspan, Color bg, Color color, float padding) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 9f, color)));
        cell.setColspan(colspan);
        cell.setHorizontalAlignment(alignment);
        cell.setBackgroundColor(bg);
        cell.setPadding(padding);
        cell.setBorderColor(bg);
        table.addCell(cell);
    }

    public static void totalCellMono(PdfPTable table, String text, Color bg, Color color) {
        totalCellMono(table, text, bg, color, mm(2.2f));
    }

    public static void totalCellMono(PdfPTable table, String text, Color bg, Color color, float padding) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(MONO_BOLD, FontFactory.COURIER_BOLD, 9f, color)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(bg);
        cell.setPadding(padding);
        cell.setBorderColor(bg);
        table.addCell(cell);
    }

    /** Enveloppe une table à bords droits dans une cellule à coins arrondis (OpenPDF ne permet pas de
     * clipper les coins d'une PdfPTable elle-même). */
    public static void roundedTable(Document doc, PdfPTable inner, float spacingAfter) {
        PdfPCell outer = new PdfPCell();
        outer.setBorder(0);
        outer.setPadding(0);
        outer.setCellEvent(RoundedCellBorder.borderOnly(mm(2.5f), BORDER, 0.8f));
        outer.addElement(inner);
        PdfPTable wrapper = new PdfPTable(1);
        wrapper.setWidthPercentage(100);
        wrapper.setSpacingAfter(spacingAfter);
        wrapper.addCell(outer);
        doc.add(wrapper);
    }

    /** Appréciation (gauche, barre d'accent or) + signature (droite), côte à côte sur une seule
     * ligne — même disposition que le bulletin officiel (économise la hauteur d'un bloc empilé en
     * plus, important pour les documents qui ajoutent du contenu au-dessus comme le diagramme en
     * bâtonnets du bulletin mensuel). {@code appreciationLines} : une ou deux lignes de texte
     * (ex. appréciation automatique puis commentaire manuel), jamais vide — l'appelant filtre avant.
     */
    public static void appreciationAndSignature(Document doc, String appreciationLabel,
                                                 java.util.List<Paragraph> appreciationLines,
                                                 String roleLabel, TenantPublicInfo info) {
        PdfPTable t = new PdfPTable(new float[]{1.6f, 1f});
        t.setWidthPercentage(100);
        t.setSpacingBefore(mm(4));

        PdfPTable appreciationInner = new PdfPTable(new float[]{0.08f, 1f});
        appreciationInner.setWidthPercentage(100);
        PdfPCell bar = new PdfPCell();
        bar.setBorder(0);
        bar.setBackgroundColor(GOLD);
        appreciationInner.addCell(bar);
        PdfPCell content = new PdfPCell();
        content.setBorder(0);
        content.setPadding(mm(3));
        content.addElement(new Paragraph(appreciationLabel.toUpperCase(Locale.FRENCH), font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 6.5f, LABEL_GREY)));
        boolean first = true;
        for (Paragraph line : appreciationLines) {
            line.setSpacingBefore(first ? 3f : 4f);
            first = false;
            content.addElement(line);
        }
        appreciationInner.addCell(content);

        // 20mm (contre 26mm côté bulletin officiel, méthode séparée, non partagée — voir
        // OpenPdfReportCardGenerator) : le mensuel ajoute un diagramme en bâtonnets au-dessus, il a
        // besoin de chaque mm pour tenir sur une seule page (§retour utilisateur).
        float blockHeight = mm(20);
        PdfPCell left = new PdfPCell();
        left.setBorder(0);
        left.setPadding(0);
        left.setMinimumHeight(blockHeight);
        left.setCellEvent(RoundedCellBorder.fillAndBorder(mm(2.5f), WHITE, BORDER, 0.8f));
        left.addElement(appreciationInner);
        t.addCell(left);

        String city = info != null ? info.city() : null;
        String date = java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH).format(java.time.LocalDate.now());
        PdfPTable sign = new PdfPTable(1);
        sign.setWidthPercentage(100);

        Paragraph faitA = new Paragraph((city != null && !city.isBlank() ? "Fait à " + city + ", " : "Fait ") + "le " + date,
                font(SANS, FontFactory.HELVETICA, 8.5f, TEXT_GREY));
        faitA.setAlignment(Element.ALIGN_CENTER);
        sign.addCell(noBorderCell(faitA));

        Paragraph role = new Paragraph(roleLabel, font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 9, DARK_TEXT));
        role.setAlignment(Element.ALIGN_CENTER);
        role.setSpacingBefore(2f);
        sign.addCell(noBorderCell(role));

        PdfPCell blank = new PdfPCell(new Phrase(" ", font(SANS, FontFactory.HELVETICA, 11, WHITE)));
        blank.setBorder(0);
        sign.addCell(blank);

        PdfPCell lineHolder = new PdfPCell();
        lineHolder.setBorder(Rectangle.TOP);
        lineHolder.setBorderColorTop(new Color(184, 176, 156));
        lineHolder.setBorderWidthTop(0.6f);
        lineHolder.setPadding(0);
        if (info != null && info.director() != null && !info.director().isBlank()) {
            Paragraph director = new Paragraph(info.director(), font(SERIF_BOLD, FontFactory.TIMES_BOLD, 13, DARK_TEXT));
            director.setAlignment(Element.ALIGN_CENTER);
            director.setSpacingBefore(3f);
            lineHolder.addElement(director);
        } else {
            lineHolder.addElement(new Paragraph(" "));
        }
        sign.addCell(lineHolder);

        PdfPCell right = new PdfPCell();
        right.setBorder(0);
        right.setMinimumHeight(blockHeight);
        right.setVerticalAlignment(Element.ALIGN_BOTTOM);
        right.addElement(sign);
        t.addCell(right);

        PdfPTable wrapper = new PdfPTable(1);
        wrapper.setWidthPercentage(100);
        wrapper.setKeepTogether(true);
        PdfPCell outerCell = new PdfPCell(t);
        outerCell.setBorder(0);
        outerCell.setPadding(0);
        wrapper.addCell(outerCell);
        doc.add(wrapper);
    }

    private static PdfPCell noBorderCell(Paragraph p) {
        PdfPCell c = new PdfPCell();
        c.setBorder(0);
        c.setPadding(0);
        c.addElement(p);
        return c;
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
