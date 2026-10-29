package gn.lacaeducation.grading.adapters.out.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.common.TenantPublicInfo;
import gn.lacaeducation.common.pdf.PremiumPageBackground;
import gn.lacaeducation.common.pdf.RoundedCellBorder;
import gn.lacaeducation.common.pdf.SchoolPdfPageFooter;
import gn.lacaeducation.grading.application.port.out.ClassCentralizerPdfGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Centralisateur de classe — reproduit fidèlement la maquette de référence (§retour utilisateur,
 * {@code features/Centralisateur-bon-design.pdf}) : fond crème, liseré tricolore, en-tête logo/école,
 * une ligne par élève avec les moyennes générales mensuelles (toutes matières confondues) de chaque
 * semestre, puis le bilan annuel (moyenne, rang, mention, décision) — jamais une colonne par matière
 * (ancien design, remplacé). Paysage vu le nombre de colonnes.
 */
@Component
@RequiredArgsConstructor
class OpenPdfClassCentralizerGenerator implements ClassCentralizerPdfGenerator {

    private static final Color DARK_GREEN = new Color(14, 77, 58);
    private static final Color GOLD = new Color(184, 137, 43);
    private static final Color BORDER = new Color(217, 210, 192);
    private static final Color BORDER_LIGHT = new Color(234, 228, 214);
    private static final Color LABEL_GREY = new Color(138, 122, 85);
    private static final Color TEXT_GREY = new Color(94, 107, 101);
    private static final Color DARK_TEXT = new Color(19, 38, 31);
    private static final Color ROW_STRIPE = new Color(252, 250, 244);
    private static final Color COMPO_TINT = new Color(251, 244, 226);
    private static final Color FAIL_RED = new Color(179, 38, 30);
    private static final Color GREEN_PILL = new Color(224, 236, 213);
    private static final Color WHITE = Color.WHITE;

    private static final String[] MONTHS_FR = {"Jan.", "Fév.", "Mars", "Avril", "Mai", "Juin",
            "Juil.", "Août", "Sept.", "Oct.", "Nov.", "Déc."};

    private static final com.lowagie.text.pdf.BaseFont SANS = gn.lacaeducation.common.pdf.DesignFonts.SANS;
    private static final com.lowagie.text.pdf.BaseFont SANS_BOLD = gn.lacaeducation.common.pdf.DesignFonts.SANS_SEMIBOLD;
    private static final com.lowagie.text.pdf.BaseFont SERIF_BOLD = gn.lacaeducation.common.pdf.DesignFonts.SERIF_BOLD;
    private static final com.lowagie.text.pdf.BaseFont MONO = gn.lacaeducation.common.pdf.DesignFonts.MONO;

    private final TenantInfoProvider tenantInfoProvider;

    @Override
    public byte[] generate(Centralizer data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), mm(11), mm(9), mm(8), mm(14));
        try {
            TenantPublicInfo info = data.tenantId() == null ? null
                    : tenantInfoProvider.findTenantInfo(data.tenantId()).orElse(null);

            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new PremiumPageBackground());
            writer.setPageEvent(new SchoolPdfPageFooter(info, BORDER_LIGHT));
            doc.open();

            header(doc, info, data);
            table(doc, data);
            statCards(doc, data);
            legendAndSignature(doc, info, data);

            doc.close();
            return out.toByteArray();
        } catch (RuntimeException e) {
            if (doc.isOpen()) doc.close();
            throw e;
        }
    }

    private static void header(Document doc, TenantPublicInfo info, Centralizer data) {
        PdfPTable t = new PdfPTable(new float[]{1f, 1f});
        t.setWidthPercentage(100);
        t.setSpacingAfter(mm(3));
        PdfPCell left = new PdfPCell();
        left.setBorder(0);
        PdfPTable inner = new PdfPTable(new float[]{1f, 3f});
        inner.setWidthPercentage(100);
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(0);
        logoCell.setFixedHeight(mm(16));
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logoCell.setCellEvent(RoundedCellBorder.fillAndBorder(mm(2), WHITE, BORDER, 0.8f));
        com.lowagie.text.Image logo = info != null ? gn.lacaeducation.common.pdf.SchoolPdfHeader.loadLogo(info.logo()) : null;
        if (logo != null) {
            logo.scaleToFit(mm(13), mm(13));
            logo.setAlignment(Element.ALIGN_MIDDLE);
            logoCell.addElement(logo);
        }
        inner.addCell(logoCell);
        PdfPCell nameCell = new PdfPCell();
        nameCell.setBorder(0);
        nameCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        nameCell.setPaddingLeft(mm(4));
        nameCell.addElement(new Paragraph(safe(info != null ? info.schoolName() : null, "École"),
                font(SERIF_BOLD, FontFactory.TIMES_BOLD, 13, DARK_GREEN)));
        String contact = join(" · ", info != null ? info.address() : null, info != null ? info.phone() : null);
        if (!contact.isBlank()) {
            Paragraph c = new Paragraph(contact, font(SANS, FontFactory.HELVETICA, 7.5f, TEXT_GREY));
            c.setSpacingBefore(1f);
            nameCell.addElement(c);
        }
        inner.addCell(nameCell);
        left.addElement(inner);
        t.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(0);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        right.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph title = new Paragraph("Centralisateur de notes", font(SERIF_BOLD, FontFactory.TIMES_BOLD, 18, DARK_GREEN));
        title.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(title);
        String sub = join(" · ",
                data.className() != null ? "CLASSE DE " + data.className().toUpperCase(Locale.FRENCH) : null,
                data.schoolYear() != null ? "ANNÉE SCOLAIRE " + data.schoolYear() : null,
                data.studentCount() + (data.studentCount() > 1 ? " ÉLÈVES" : " ÉLÈVE"));
        Paragraph subP = new Paragraph(sub, font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 7.5f, GOLD));
        subP.setAlignment(Element.ALIGN_RIGHT);
        subP.setSpacingBefore(1.5f);
        right.addElement(subP);
        t.addCell(right);
        doc.add(t);
    }

    private static void table(Document doc, Centralizer data) {
        List<YearMonth> s1 = data.s1Months();
        List<YearMonth> s2 = data.s2Months();
        int nCols = 2 + s1.size() + 2 + s2.size() + 2 + 4;
        float[] widths = new float[nCols];
        widths[0] = 0.4f;
        widths[1] = 2.3f;
        int i = 2;
        for (int k = 0; k < s1.size(); k++) widths[i++] = 0.8f;
        widths[i++] = 0.85f; // S1 Compo.
        widths[i++] = 0.95f; // Moy. S1
        for (int k = 0; k < s2.size(); k++) widths[i++] = 0.8f;
        widths[i++] = 0.85f; // S2 Compo.
        widths[i++] = 0.95f; // Moy. S2
        widths[i++] = 0.95f; // Moy. an.
        widths[i++] = 0.6f;  // Rang
        widths[i++] = 1.15f; // Mention
        widths[i] = 1.0f;    // Décision

        PdfPTable t = new PdfPTable(widths);
        t.setWidthPercentage(100);
        t.setHeaderRows(2);

        // Ligne 1 : groupes fusionnés.
        groupHeaderCell(t, "N°", DARK_GREEN, 1, 2);
        groupHeaderCell(t, "Nom & prénom", DARK_GREEN, 1, 2);
        groupHeaderCell(t, "1ER SEMESTRE", DARK_GREEN, s1.size() + 2, 1);
        groupHeaderCell(t, "2ÈME SEMESTRE", DARK_GREEN, s2.size() + 2, 1);
        groupHeaderCell(t, "BILAN ANNUEL", GOLD, 4, 1);

        // Ligne 2 : sous-colonnes.
        for (YearMonth ym : s1) subHeaderCell(t, MONTHS_FR[ym.getMonthValue() - 1], DARK_GREEN);
        subHeaderCell(t, "Compo.", DARK_GREEN);
        subHeaderCell(t, "Moy. S1", DARK_GREEN);
        for (YearMonth ym : s2) subHeaderCell(t, MONTHS_FR[ym.getMonthValue() - 1], DARK_GREEN);
        subHeaderCell(t, "Compo.", DARK_GREEN);
        subHeaderCell(t, "Moy. S2", DARK_GREEN);
        subHeaderCell(t, "Moy. an.", GOLD);
        subHeaderCell(t, "Rang", GOLD);
        subHeaderCell(t, "Mention", GOLD);
        subHeaderCell(t, "Décision", GOLD);

        boolean zebra = false;
        int n = 1;
        for (StudentRow row : data.rows()) {
            Color bg = zebra ? ROW_STRIPE : WHITE;
            zebra = !zebra;
            boolean failing = "REDOUBLE".equals(row.decision());
            Color numColor = failing ? FAIL_RED : DARK_TEXT;

            bodyCell(t, String.valueOf(n++), Element.ALIGN_CENTER, bg, numColor, false);
            bodyCell(t, safe(row.studentName()), Element.ALIGN_LEFT, bg, numColor, true);
            for (YearMonth ym : s1) bodyCellMono(t, dec(row.s1Monthly() == null ? null : row.s1Monthly().get(ym)), bg, numColor);
            bodyCellMono(t, dec(row.s1Compo()), bg, numColor);
            bodyCellMonoTint(t, dec(row.s1Average()), COMPO_TINT, failing ? FAIL_RED : DARK_GREEN);
            for (YearMonth ym : s2) bodyCellMono(t, dec(row.s2Monthly() == null ? null : row.s2Monthly().get(ym)), bg, numColor);
            bodyCellMono(t, dec(row.s2Compo()), bg, numColor);
            bodyCellMonoTint(t, dec(row.s2Average()), COMPO_TINT, failing ? FAIL_RED : DARK_GREEN);
            bodyCellMonoTint(t, dec(row.annualAverage()), new Color(250, 240, 220), failing ? FAIL_RED : GOLD.darker());
            bodyCell(t, row.rank() == null ? "—" : rankLabel(row.rank()), Element.ALIGN_CENTER, bg, numColor, false);
            bodyCell(t, safe(row.honor()), Element.ALIGN_CENTER, bg, numColor, false);
            bodyCell(t, safe(row.decision()), Element.ALIGN_CENTER, failing ? bg : GREEN_PILL, failing ? FAIL_RED : DARK_GREEN, true);
        }
        if (data.rows().isEmpty()) {
            PdfPCell empty = new PdfPCell(new Phrase("Aucun bulletin semestriel généré pour cette classe et cette année scolaire.",
                    font(SANS, FontFactory.HELVETICA_OBLIQUE, 9.5f, TEXT_GREY)));
            empty.setColspan(nCols);
            empty.setHorizontalAlignment(Element.ALIGN_CENTER);
            empty.setPadding(12f);
            t.addCell(empty);
        }
        roundedTable(doc, t, mm(2.6f));
    }

    private static void statCards(Document doc, Centralizer data) {
        PdfPTable grid = new PdfPTable(new float[]{1f, 1f, 1f, 1f, 1.3f});
        grid.setWidthPercentage(100);
        statCell(grid, "Moyenne classe S1", dec(data.s1ClassAverage()), true);
        statCell(grid, "Moyenne classe S2", dec(data.s2ClassAverage()), true);
        statCell(grid, "Moyenne annuelle", dec(data.annualClassAverage()), true);
        long total = data.rows() == null ? 0 : data.rows().size();
        statCell(grid, "Admis", data.admittedCount() + " / " + total, true);
        statCell(grid, "Major de classe", safe(data.topStudentName(), "—"), false);

        PdfPCell outer = new PdfPCell();
        outer.setBorder(0);
        outer.setPadding(0);
        outer.setCellEvent(RoundedCellBorder.fillAndBorder(mm(2.5f), WHITE, BORDER, 0.8f));
        outer.addElement(grid);
        PdfPTable wrapper = new PdfPTable(1);
        wrapper.setWidthPercentage(100);
        wrapper.setSpacingBefore(mm(3));
        wrapper.setSpacingAfter(mm(3));
        wrapper.addCell(outer);
        doc.add(wrapper);
    }

    private static void statCell(PdfPTable grid, String label, String value, boolean borderRight) {
        PdfPCell c = new PdfPCell();
        c.setPadding(mm(2.2f));
        c.setPaddingLeft(mm(3.5f));
        c.setBorder(0);
        if (borderRight) { c.setBorder(Rectangle.RIGHT); c.setBorderColorRight(BORDER_LIGHT); c.setBorderWidthRight(0.6f); }
        c.addElement(new Paragraph(label.toUpperCase(Locale.FRENCH), font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 6.5f, LABEL_GREY)));
        Paragraph v = new Paragraph(value, font(SERIF_BOLD, FontFactory.TIMES_BOLD, 13, DARK_TEXT));
        v.setSpacingBefore(1.5f);
        c.addElement(v);
        grid.addCell(c);
    }

    private static void legendAndSignature(Document doc, TenantPublicInfo info, Centralizer data) {
        String city = info != null ? info.city() : null;
        String date = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH).format(java.time.LocalDate.now());
        String note = "Moyennes mensuelles sur 20 (total coefficié ÷ 12) · "
                + (city != null && !city.isBlank() ? "Fait à " + city + ", " : "Fait ") + "le " + date;

        PdfPTable t = new PdfPTable(new float[]{2f, 1f});
        t.setWidthPercentage(100);
        PdfPCell left = new PdfPCell(new Phrase(note, font(SANS, FontFactory.HELVETICA_OBLIQUE, 7.5f, TEXT_GREY)));
        left.setBorder(0);
        left.setVerticalAlignment(Element.ALIGN_BOTTOM);
        t.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(0);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph role = new Paragraph("Le Principal", font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 9.5f, DARK_TEXT));
        role.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(role);
        if (info != null && info.director() != null && !info.director().isBlank()) {
            Paragraph director = new Paragraph(info.director(), font(SERIF_BOLD, FontFactory.TIMES_BOLD, 10.5f, DARK_TEXT));
            director.setAlignment(Element.ALIGN_RIGHT);
            director.setSpacingBefore(14f);
            right.addElement(director);
        }
        t.addCell(right);
        doc.add(t);
    }

    private static String rankLabel(int rank) {
        return switch (rank) {
            case 1 -> "1er";
            default -> rank + "e";
        };
    }

    private static void groupHeaderCell(PdfPTable table, String text, Color bg, int colspan, int rowspan) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 7.5f, WHITE)));
        cell.setColspan(colspan);
        cell.setRowspan(rowspan);
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4f);
        cell.setBorderColor(bg);
        table.addCell(cell);
    }

    private static void subHeaderCell(PdfPTable table, String text, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 6.8f, WHITE)));
        cell.setBackgroundColor(darken(bg));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(3.2f);
        cell.setBorderColor(bg);
        table.addCell(cell);
    }

    private static Color darken(Color c) {
        return new Color(Math.max(0, c.getRed() - 18), Math.max(0, c.getGreen() - 18), Math.max(0, c.getBlue() - 18));
    }

    private static void bodyCell(PdfPTable table, String text, int alignment, Color bg, Color color, boolean bold) {
        PdfPCell cell = new PdfPCell(new Phrase(text, bold
                ? font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 7.5f, color)
                : font(SANS, FontFactory.HELVETICA, 7.5f, color)));
        cell.setHorizontalAlignment(alignment);
        cell.setBackgroundColor(bg);
        cell.setPadding(2.6f);
        cell.setBorderColor(new Color(232, 234, 237));
        table.addCell(cell);
    }

    private static void bodyCellMono(PdfPTable table, String text, Color bg, Color color) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(MONO, FontFactory.COURIER, 7.5f, color)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(bg);
        cell.setPadding(2.6f);
        cell.setBorderColor(new Color(232, 234, 237));
        table.addCell(cell);
    }

    private static void bodyCellMonoTint(PdfPTable table, String text, Color bg, Color textColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(MONO, FontFactory.COURIER_BOLD, 8f, textColor)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(bg);
        cell.setPadding(2.6f);
        cell.setBorderColor(new Color(232, 234, 237));
        table.addCell(cell);
    }

    private static void roundedTable(Document doc, PdfPTable inner, float spacingAfter) {
        PdfPCell outer = new PdfPCell(inner);
        outer.setBorder(0);
        outer.setPadding(0);
        outer.setCellEvent(RoundedCellBorder.borderOnly(mm(2.5f), BORDER, 0.8f));
        PdfPTable wrapper = new PdfPTable(1);
        wrapper.setWidthPercentage(100);
        wrapper.setSpacingAfter(spacingAfter);
        wrapper.addCell(outer);
        doc.add(wrapper);
    }

    private static com.lowagie.text.Font font(com.lowagie.text.pdf.BaseFont base, String fallbackFamily, float size, Color color) {
        if (base != null) {
            com.lowagie.text.Font f = new com.lowagie.text.Font(base, size);
            f.setColor(color);
            return f;
        }
        return FontFactory.getFont(fallbackFamily, size, color);
    }

    private static float mm(float v) { return v * 72f / 25.4f; }

    private static String safe(String s) { return s == null || s.isBlank() ? "—" : s; }

    private static String safe(String s, String fallback) { return s == null || s.isBlank() ? fallback : s; }

    private static String dec(BigDecimal v) { return v == null ? "—" : v.stripTrailingZeros().toPlainString(); }

    private static String join(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isBlank()) continue;
            if (sb.length() > 0) sb.append(sep);
            sb.append(p);
        }
        return sb.toString();
    }
}
