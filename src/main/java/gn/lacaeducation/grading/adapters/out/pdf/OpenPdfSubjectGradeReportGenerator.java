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
import gn.lacaeducation.grading.application.port.in.GradeUseCase.ReleveAssessment;
import gn.lacaeducation.grading.application.port.in.GradeUseCase.ReleveRow;
import gn.lacaeducation.grading.application.port.in.GradeUseCase.SubjectGradeReport;
import gn.lacaeducation.grading.application.port.out.SubjectGradeReportPdfGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/**
 * Relevé de notes d'une matière pour une classe (une colonne par évaluation + moyenne calculée) —
 * reproduit fidèlement le design premium de référence (§document-school, "Relevé de notes classe.dc.html") :
 * fond crème, liseré tricolore, en-tête logo/école, titre encadré de filets or, carte Matière/Classe/
 * Période/Année, tableau à coins arrondis (colonne Moyenne teintée or), légende moyenne &lt; 10, signature
 * professeur principal + principal. Paysage (nombre de colonnes variable selon les évaluations).
 */
@Component
@RequiredArgsConstructor
class OpenPdfSubjectGradeReportGenerator implements SubjectGradeReportPdfGenerator {

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

    private static final com.lowagie.text.pdf.BaseFont SANS = gn.lacaeducation.common.pdf.DesignFonts.SANS;
    private static final com.lowagie.text.pdf.BaseFont SANS_BOLD = gn.lacaeducation.common.pdf.DesignFonts.SANS_SEMIBOLD;
    private static final com.lowagie.text.pdf.BaseFont SERIF_BOLD = gn.lacaeducation.common.pdf.DesignFonts.SERIF_BOLD;
    private static final com.lowagie.text.pdf.BaseFont MONO = gn.lacaeducation.common.pdf.DesignFonts.MONO;

    private final TenantInfoProvider tenantInfoProvider;

    @Override
    public byte[] generate(SubjectGradeReport rep) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), mm(13), mm(10), mm(8), mm(14));
        try {
            TenantPublicInfo info = rep.tenantId() == null ? null
                    : tenantInfoProvider.findTenantInfo(rep.tenantId()).orElse(null);

            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new BulletinPageBackground());
            writer.setPageEvent(new gn.lacaeducation.common.pdf.SchoolPdfPageFooter(info, BORDER_LIGHT));
            doc.open();

            header(doc, info, rep.className());
            metaCard(doc, rep);
            table(doc, rep);
            signature(doc, info);

            doc.close();
            return out.toByteArray();
        } catch (RuntimeException e) {
            if (doc.isOpen()) doc.close();
            throw e;
        }
    }

    private static void header(Document doc, TenantPublicInfo info, String className) {
        PdfPTable t = new PdfPTable(new float[]{1f, 1f});
        t.setWidthPercentage(100);
        t.setSpacingAfter(mm(2.6f));
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
        inner.addCell(nameCell);
        left.addElement(inner);
        t.addCell(left);

        PdfPCell right = new PdfPCell(new Phrase("Relevé de notes" + (className != null && !className.isBlank() ? " — " + className : ""),
                font(SERIF_BOLD, FontFactory.TIMES_BOLD, 18, DARK_GREEN)));
        right.setBorder(0);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        right.setVerticalAlignment(Element.ALIGN_MIDDLE);
        t.addCell(right);
        doc.add(t);
    }

    private static void metaCard(Document doc, SubjectGradeReport rep) {
        PdfPTable grid = new PdfPTable(new float[]{1f, 1f, 1f, 1f});
        grid.setWidthPercentage(100);
        metaCell(grid, "Matière", safe(rep.subjectName()), true);
        metaCell(grid, "Classe", safe(rep.className()), true);
        metaCell(grid, "Période", safe(rep.periodLabel()), true);
        metaCell(grid, "Année scolaire", safe(rep.schoolYear()), false);

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
        c.setPadding(mm(2f));
        c.setPaddingLeft(mm(3));
        c.setBorder(0);
        if (borderRight) { c.setBorder(Rectangle.RIGHT); c.setBorderColorRight(BORDER_LIGHT); c.setBorderWidthRight(0.6f); }
        c.addElement(new Paragraph(label.toUpperCase(Locale.FRENCH), font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 6.5f, LABEL_GREY)));
        Paragraph v = new Paragraph(value, font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 10.5f, DARK_TEXT));
        v.setSpacingBefore(1.5f);
        c.addElement(v);
        grid.addCell(c);
    }

    private static void table(Document doc, SubjectGradeReport rep) {
        List<ReleveAssessment> cols = rep.assessments();
        float[] widths = new float[cols.size() + 2];
        widths[0] = 2.6f;
        for (int i = 1; i <= cols.size(); i++) widths[i] = 1.1f;
        widths[widths.length - 1] = 1.1f;

        PdfPTable t = new PdfPTable(widths);
        t.setWidthPercentage(100);

        headerCell(t, "Prénoms et nom");
        // En-tête compressé sur 2 lignes (pas 4) : la matière est déjà dans la carte Matière/Classe/
        // Période juste au-dessus — la répéter par colonne gaspillait une hauteur précieuse en paysage
        // (moins de hauteur disponible qu'en portrait) et faisait déborder la signature sur une 2ème page.
        for (ReleveAssessment a : cols) {
            String head = safe(a.type() != null ? a.type() : a.label());
            if (a.date() != null) head += " · " + a.date().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"));
            head += "\n/" + (a.maxScore() != null ? a.maxScore().stripTrailingZeros().toPlainString() : "20")
                    + (a.coefficient() != null ? " · coef " + a.coefficient().stripTrailingZeros().toPlainString() : "");
            headerCell(t, head);
        }
        headerCellTint(t, "Moyenne", GOLD);

        boolean zebra = false;
        for (ReleveRow r : rep.rows()) {
            Color bg = zebra ? ROW_STRIPE : WHITE;
            zebra = !zebra;
            bodyCell(t, safe(r.fullName()), Element.ALIGN_LEFT, bg, DARK_TEXT, true);
            for (ReleveAssessment a : cols) {
                BigDecimal v = r.notes() == null ? null : r.notes().get(a.id());
                bodyCellMono(t, v == null ? "—" : trimZeros(v), bg);
            }
            Color avgColor = r.average() != null && r.average().compareTo(BigDecimal.TEN) < 0 ? FAIL_RED : DARK_GREEN;
            bodyCellMonoTint(t, r.average() == null ? "—" : trimZeros(r.average()), COMPO_TINT, avgColor);
        }
        if (rep.rows().isEmpty()) {
            PdfPCell empty = new PdfPCell(new Phrase("Aucun élève dans cette classe.", font(SANS, FontFactory.HELVETICA_OBLIQUE, 9.5f, TEXT_GREY)));
            empty.setColspan(widths.length);
            empty.setHorizontalAlignment(Element.ALIGN_CENTER);
            empty.setPadding(10f);
            t.addCell(empty);
        }
        roundedTable(doc, t, mm(2.6f));

        if (cols.isEmpty()) {
            Paragraph note = new Paragraph("Aucune évaluation programmée pour cette matière sur la période.",
                    font(SANS, FontFactory.HELVETICA_OBLIQUE, 8.5f, TEXT_GREY));
            note.setSpacingAfter(mm(2.6f));
            doc.add(note);
        } else {
            Paragraph legend = new Paragraph(
                    "La moyenne est calculée selon le paramétrage de l'établissement (méthode, coefficients, arrondi) "
                            + "et se met à jour à chaque nouvelle évaluation.",
                    font(SANS, FontFactory.HELVETICA_OBLIQUE, 7.5f, TEXT_GREY));
            legend.setSpacingAfter(mm(2.6f));
            doc.add(legend);
        }
    }

    private static void signature(Document doc, TenantPublicInfo info) {
        PdfPTable t = new PdfPTable(new float[]{1f, 1f});
        t.setWidthPercentage(55);
        t.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.setSpacingBefore(mm(3));
        signatureCell(t, "Le Professeur principal", null);
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
        PdfPCell lineHolder = new PdfPCell();
        lineHolder.setBorder(Rectangle.TOP);
        lineHolder.setBorderColorTop(new Color(184, 176, 156));
        lineHolder.setBorderWidthTop(0.6f);
        lineHolder.setPadding(0);
        if (name != null && !name.isBlank()) {
            Paragraph n = new Paragraph(name, font(SERIF_BOLD, FontFactory.TIMES_BOLD, 10.5f, DARK_TEXT));
            n.setAlignment(Element.ALIGN_CENTER);
            n.setSpacingBefore(3f);
            lineHolder.addElement(n);
        } else {
            lineHolder.addElement(new Paragraph(" "));
        }
        PdfPTable lineWrapper = new PdfPTable(1);
        lineWrapper.setWidthPercentage(100);
        lineWrapper.setSpacingBefore(mm(3));
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
        PdfPCell cell = new PdfPCell(new Phrase(text, font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 7.5f, WHITE)));
        cell.setBackgroundColor(DARK_GREEN);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4f);
        table.addCell(cell);
    }

    private static void headerCellTint(PdfPTable table, String text, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 7.5f, WHITE)));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4f);
        table.addCell(cell);
    }

    private static void bodyCell(PdfPTable table, String text, int alignment, Color bg, Color color, boolean bold) {
        PdfPCell cell = new PdfPCell(new Phrase(text, bold
                ? font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 8.5f, color)
                : font(SANS, FontFactory.HELVETICA, 8.5f, color)));
        cell.setHorizontalAlignment(alignment);
        cell.setBackgroundColor(bg);
        cell.setPadding(2.6f);
        cell.setBorderColor(new Color(232, 234, 237));
        table.addCell(cell);
    }

    private static void bodyCellMono(PdfPTable table, String text, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(MONO, FontFactory.COURIER, 8f, TEXT_GREY)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(bg);
        cell.setPadding(2.6f);
        cell.setBorderColor(new Color(232, 234, 237));
        table.addCell(cell);
    }

    private static void bodyCellMonoTint(PdfPTable table, String text, Color bg, Color textColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(MONO, FontFactory.COURIER_BOLD, 9f, textColor)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(bg);
        cell.setPadding(2.6f);
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

    private static String trimZeros(BigDecimal v) {
        if (v == null) return "—";
        return v.stripTrailingZeros().toPlainString();
    }
}
