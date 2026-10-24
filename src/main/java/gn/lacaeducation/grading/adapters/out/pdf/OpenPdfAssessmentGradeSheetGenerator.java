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
import gn.lacaeducation.grading.application.port.out.AssessmentGradeSheetPdfGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Fiche de notes d'UNE évaluation précise (ex. « Anglais — Oral — Octobre ») — reproduit fidèlement le
 * design premium de référence (§document-school, "Fiche de notes.dc.html") : fond crème, liseré
 * tricolore, en-tête logo/école, titre encadré de filets or, carte Matière/Type/Date/Classe, tableau
 * Matricule/Nom/Note à coins arrondis, cartes de statistiques (moyenne de classe, plus haute/basse
 * note, effectif ≥ 10/20), signature chargé de cours + principal.
 */
@Component
@RequiredArgsConstructor
class OpenPdfAssessmentGradeSheetGenerator implements AssessmentGradeSheetPdfGenerator {

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
    private static final Color WHITE = Color.WHITE;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final com.lowagie.text.pdf.BaseFont SANS = gn.lacaeducation.common.pdf.DesignFonts.SANS;
    private static final com.lowagie.text.pdf.BaseFont SANS_BOLD = gn.lacaeducation.common.pdf.DesignFonts.SANS_SEMIBOLD;
    private static final com.lowagie.text.pdf.BaseFont SANS_EXTRABOLD = gn.lacaeducation.common.pdf.DesignFonts.SANS_EXTRABOLD;
    private static final com.lowagie.text.pdf.BaseFont SERIF_BOLD = gn.lacaeducation.common.pdf.DesignFonts.SERIF_BOLD;
    private static final com.lowagie.text.pdf.BaseFont MONO = gn.lacaeducation.common.pdf.DesignFonts.MONO;

    private final TenantInfoProvider tenantInfoProvider;

    @Override
    public byte[] generate(AssessmentGradeSheet sheet) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, mm(15), mm(12), mm(10), mm(16));
        try {
            TenantPublicInfo info = sheet.tenantId() == null ? null
                    : tenantInfoProvider.findTenantInfo(sheet.tenantId()).orElse(null);

            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new PremiumPageBackground());
            writer.setPageEvent(new gn.lacaeducation.common.pdf.SchoolPdfPageFooter(info, BORDER_LIGHT));
            doc.open();

            header(doc, info);
            titleBar(doc, "Fiche de notes");
            metaCard(doc, sheet);

            List<StudentGradeRow> rows = sheet.rows() != null ? sheet.rows() : List.of();
            table(doc, sheet, rows);
            stats(doc, rows, sheet.maxScore());
            signature(doc, info);

            doc.close();
            return out.toByteArray();
        } catch (RuntimeException e) {
            if (doc.isOpen()) {
                doc.close();
            }
            throw e;
        }
    }

    private static void header(Document doc, TenantPublicInfo info) {
        PdfPTable t = new PdfPTable(new float[]{1f});
        t.setWidthPercentage(100);
        t.setSpacingAfter(mm(2.6f));
        PdfPCell row = new PdfPCell();
        row.setBorder(0);
        PdfPTable inner = new PdfPTable(new float[]{1f, 3f});
        inner.setWidthPercentage(100);
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(0);
        logoCell.setFixedHeight(mm(19));
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logoCell.setCellEvent(RoundedCellBorder.fillAndBorder(mm(2), WHITE, BORDER, 0.8f));
        com.lowagie.text.Image logo = info != null ? gn.lacaeducation.common.pdf.SchoolPdfHeader.loadLogo(info.logo()) : null;
        if (logo != null) {
            logo.scaleToFit(mm(16), mm(16));
            logo.setAlignment(Element.ALIGN_MIDDLE);
            logoCell.addElement(logo);
        }
        inner.addCell(logoCell);
        PdfPCell nameCell = new PdfPCell();
        nameCell.setBorder(0);
        nameCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        nameCell.setPaddingLeft(mm(4));
        nameCell.addElement(new Paragraph(safe(info != null ? info.schoolName() : null, "École"),
                font(SERIF_BOLD, FontFactory.TIMES_BOLD, 14, DARK_GREEN)));
        String addr = info != null
                ? join(" · ", info.address(), info.city(), "Guinée")
                : "";
        if (!addr.isBlank()) {
            Paragraph a = new Paragraph(addr, font(SANS, FontFactory.HELVETICA, 8, TEXT_GREY));
            a.setSpacingBefore(1.5f);
            nameCell.addElement(a);
        }
        inner.addCell(nameCell);
        row.addElement(inner);
        t.addCell(row);
        doc.add(t);
    }

    private static void titleBar(Document doc, String title) {
        PdfPTable t = new PdfPTable(new float[]{1f, 0f, 1f});
        t.setWidthPercentage(100);
        t.setSpacingAfter(mm(2.6f));
        t.getDefaultCell().setBorder(0);
        PdfPCell line1 = new PdfPCell();
        line1.setBorder(Rectangle.BOTTOM);
        line1.setBorderColorBottom(GOLD);
        line1.setBorderWidthBottom(0.8f);
        line1.setFixedHeight(mm(9));
        t.addCell(line1);
        PdfPCell titleCell = new PdfPCell(new Phrase(title, font(SERIF_BOLD, FontFactory.TIMES_BOLD, 16, DARK_GREEN)));
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
        line2.setFixedHeight(mm(9));
        t.addCell(line2);
        doc.add(t);
    }

    private static void metaCard(Document doc, AssessmentGradeSheet sheet) {
        PdfPTable grid = new PdfPTable(new float[]{1f, 1f, 1f, 1f});
        grid.setWidthPercentage(100);
        metaCell(grid, "Matière", safe(sheet.subjectName()), true);
        metaCell(grid, "Type", safe(sheet.assessmentTypeLabel()), true);
        metaCell(grid, "Date", sheet.date() != null ? sheet.date().format(DATE_FMT) : "—", true);
        metaCell(grid, "Classe", safe(sheet.className()), false);

        PdfPCell outer = new PdfPCell();
        outer.setBorder(0);
        outer.setPadding(0);
        outer.setCellEvent(RoundedCellBorder.fillAndBorder(mm(2.5f), WHITE, BORDER, 0.8f));
        outer.addElement(grid);
        PdfPTable wrapper = new PdfPTable(1);
        wrapper.setWidthPercentage(100);
        wrapper.setSpacingAfter(mm(2.6f));
        wrapper.addCell(outer);
        doc.add(wrapper);
    }

    private static void metaCell(PdfPTable grid, String label, String value, boolean borderRight) {
        PdfPCell c = new PdfPCell();
        c.setPadding(mm(2.2f));
        c.setPaddingLeft(mm(3));
        c.setBorder(0);
        if (borderRight) { c.setBorder(Rectangle.RIGHT); c.setBorderColorRight(BORDER_LIGHT); c.setBorderWidthRight(0.6f); }
        c.addElement(new Paragraph(label.toUpperCase(Locale.FRENCH), font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 6.5f, LABEL_GREY)));
        Paragraph v = new Paragraph(value, font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 11, DARK_TEXT));
        v.setSpacingBefore(1.5f);
        c.addElement(v);
        grid.addCell(c);
    }

    private static void table(Document doc, AssessmentGradeSheet sheet, List<StudentGradeRow> rows) {
        PdfPTable table = new PdfPTable(new float[]{0.5f, 1.1f, 2.6f, 1.2f});
        table.setWidthPercentage(100);
        headerCell(table, "N°");
        headerCell(table, "Matricule");
        headerCell(table, "Prénoms et nom");
        headerCellTint(table, "Note / " + trimZeros(sheet.maxScore()), GOLD);

        boolean zebra = false;
        int n = 1;
        for (StudentGradeRow r : rows) {
            Color bg = zebra ? ROW_STRIPE : WHITE;
            zebra = !zebra;
            bodyCell(table, String.valueOf(n++), Element.ALIGN_CENTER, bg, LABEL_GREY, false);
            bodyCellMono(table, safe(r.studentNumber()), bg);
            bodyCell(table, r.studentName(), Element.ALIGN_LEFT, bg, DARK_TEXT, true);
            Color noteColor = r.value() != null && r.value().compareTo(BigDecimal.TEN) < 0 ? FAIL_RED : DARK_GREEN;
            bodyCellMonoTint(table, r.value() != null ? trimZeros(r.value()) : "—", COMPO_TINT, noteColor);
        }
        if (rows.isEmpty()) {
            PdfPCell empty = new PdfPCell(new Phrase("Aucun élève dans cette classe.", font(SANS, FontFactory.HELVETICA_OBLIQUE, 9.5f, TEXT_GREY)));
            empty.setColspan(4);
            empty.setHorizontalAlignment(Element.ALIGN_CENTER);
            empty.setPadding(10f);
            table.addCell(empty);
        }
        roundedTable(doc, table, mm(2.6f));
    }

    private static void stats(Document doc, List<StudentGradeRow> rows, BigDecimal maxScore) {
        List<BigDecimal> values = rows.stream().map(StudentGradeRow::value).filter(java.util.Objects::nonNull).toList();
        if (values.isEmpty()) return;
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avg = sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
        BigDecimal max = values.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal min = values.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        long atLeastHalf = values.stream().filter(v -> v.compareTo(halfOf(maxScore)) >= 0).count();

        PdfPTable row = new PdfPTable(new float[]{1f, 1f, 1f, 1f});
        row.setWidthPercentage(100);
        row.setSpacingAfter(mm(2.6f));
        statCard(row, "Moyenne de classe", avg.toPlainString());
        statCard(row, "Note la plus haute", trimZeros(max));
        statCard(row, "Note la plus basse", trimZeros(min));
        statCard(row, "Moyenne ≥ " + trimZeros(halfOf(maxScore)), atLeastHalf + " / " + values.size());
        doc.add(row);
    }

    private static BigDecimal halfOf(BigDecimal maxScore) {
        BigDecimal max = maxScore != null ? maxScore : BigDecimal.valueOf(20);
        return max.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }

    private static void statCard(PdfPTable row, String label, String value) {
        PdfPCell c = new PdfPCell();
        c.setPadding(mm(2.5f));
        c.setPaddingLeft(mm(3.2f));
        c.setBorder(0);
        c.setCellEvent(RoundedCellBorder.fillAndBorder(mm(2.5f), WHITE, BORDER, 0.8f));
        c.addElement(new Paragraph(label.toUpperCase(Locale.FRENCH), font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 6.5f, LABEL_GREY)));
        Paragraph v = new Paragraph(value, font(SANS_EXTRABOLD, FontFactory.HELVETICA_BOLD, 14, DARK_GREEN));
        v.setSpacingBefore(1.5f);
        c.addElement(v);
        row.addCell(c);
    }

    private static void signature(Document doc, TenantPublicInfo info) {
        PdfPTable t = new PdfPTable(new float[]{1f, 1f});
        t.setWidthPercentage(72);
        t.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.setSpacingBefore(mm(4));
        signatureCell(t, "L'enseignant(e)", null);
        signatureCell(t, "Le Principal", info != null ? info.director() : null);
        doc.add(t);
    }

    private static void signatureCell(PdfPTable table, String label, String name) {
        PdfPCell c = new PdfPCell();
        c.setBorder(0);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph l = new Paragraph(label, font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 9, DARK_TEXT));
        l.setAlignment(Element.ALIGN_CENTER);
        c.addElement(l);
        PdfPCell blank = new PdfPCell(new Phrase(" ", font(SANS, FontFactory.HELVETICA, 11, WHITE)));
        blank.setBorder(0);
        PdfPTable spacer = new PdfPTable(1);
        spacer.setWidthPercentage(100);
        spacer.addCell(blank);
        c.addElement(spacer);
        PdfPCell lineHolder = new PdfPCell();
        lineHolder.setBorder(Rectangle.TOP);
        lineHolder.setBorderColorTop(new Color(184, 176, 156));
        lineHolder.setBorderWidthTop(0.6f);
        lineHolder.setPadding(0);
        if (name != null && !name.isBlank()) {
            Paragraph n = new Paragraph(name, font(SERIF_BOLD, FontFactory.TIMES_BOLD, 11, DARK_TEXT));
            n.setAlignment(Element.ALIGN_CENTER);
            n.setSpacingBefore(3f);
            lineHolder.addElement(n);
        } else {
            lineHolder.addElement(new Paragraph(" "));
        }
        PdfPTable lineWrapper = new PdfPTable(1);
        lineWrapper.setWidthPercentage(100);
        lineWrapper.setSpacingBefore(5f);
        lineWrapper.addCell(lineHolder);
        c.addElement(lineWrapper);
        table.addCell(c);
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

    private static void headerCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 8.5f, WHITE)));
        cell.setBackgroundColor(DARK_GREEN);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(3.5f);
        table.addCell(cell);
    }

    private static void headerCellTint(PdfPTable table, String text, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 8.5f, WHITE)));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(3.5f);
        table.addCell(cell);
    }

    private static void bodyCell(PdfPTable table, String text, int alignment, Color bg, Color color, boolean bold) {
        PdfPCell cell = new PdfPCell(new Phrase(text, bold
                ? font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 9f, color)
                : font(SANS, FontFactory.HELVETICA, 9f, color)));
        cell.setHorizontalAlignment(alignment);
        cell.setBackgroundColor(bg);
        cell.setPadding(3.5f);
        cell.setBorderColor(new Color(232, 234, 237));
        table.addCell(cell);
    }

    private static void bodyCellMono(PdfPTable table, String text, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(MONO, FontFactory.COURIER, 8.5f, TEXT_GREY)));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setBackgroundColor(bg);
        cell.setPadding(3.5f);
        cell.setBorderColor(new Color(232, 234, 237));
        table.addCell(cell);
    }

    private static void bodyCellMonoTint(PdfPTable table, String text, Color bg, Color textColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(MONO, FontFactory.COURIER_BOLD, 9.5f, textColor)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(bg);
        cell.setPadding(3.5f);
        cell.setBorderColor(new Color(232, 234, 237));
        table.addCell(cell);
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

    private static String join(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isBlank()) continue;
            if (sb.length() > 0) sb.append(sep);
            sb.append(p);
        }
        return sb.toString();
    }

    private static String trimZeros(BigDecimal v) {
        if (v == null) return "—";
        return v.stripTrailingZeros().toPlainString();
    }
}
