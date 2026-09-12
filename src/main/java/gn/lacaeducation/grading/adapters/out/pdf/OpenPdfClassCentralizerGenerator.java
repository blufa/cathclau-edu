package gn.lacaeducation.grading.adapters.out.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.common.TenantPublicInfo;
import gn.lacaeducation.common.pdf.SchoolPdfHeader;
import gn.lacaeducation.common.pdf.SchoolPdfPageFooter;
import gn.lacaeducation.grading.application.port.out.ClassCentralizerPdfGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

/**
 * Centralisateur de classe (§documents ministère) : une ligne par élève, colonnes Cours/Composition
 * par matière, moyenne générale et rang — paysage vu le nombre de colonnes.
 */
@Component
@RequiredArgsConstructor
class OpenPdfClassCentralizerGenerator implements ClassCentralizerPdfGenerator {

    private static final Color GREY = SchoolPdfHeader.GREY;
    private static final Color DARK = SchoolPdfHeader.DARK;
    private static final Color ZEBRA = new Color(247, 248, 250);

    private final TenantInfoProvider tenantInfoProvider;

    @Override
    public byte[] generate(Centralizer data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 24, 24, 36, 50);
        try {
            TenantPublicInfo info = data.tenantId() == null ? null
                    : tenantInfoProvider.findTenantInfo(data.tenantId()).orElse(null);
            Color brand = SchoolPdfHeader.parseColor(info == null ? null : info.brandColor());

            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new SchoolPdfPageFooter(info, brand));
            doc.open();

            SchoolPdfHeader.render(doc, tenantInfoProvider, data.tenantId(), "FICHE DE CENTRALISATION DES MOYENNES PAR MATIÈRE");

            Paragraph title = new Paragraph("FICHE DE CENTRALISATION DES MOYENNES PAR MATIÈRE",
                    SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 14, brand));
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(6f);
            doc.add(title);

            String sub = join(" · ", data.className(), data.periodLabel(), data.schoolYear());
            if (!sub.isBlank()) {
                Paragraph subtitle = new Paragraph(sub, SchoolPdfHeader.font(FontFactory.HELVETICA, 9.5f, GREY));
                subtitle.setAlignment(Element.ALIGN_CENTER);
                subtitle.setSpacingAfter(8f);
                doc.add(subtitle);
            }

            java.util.List<Long> subjectIds = data.subjectIds() != null ? data.subjectIds() : java.util.List.of();
            int nCols = 3 + subjectIds.size() * 2 + 2; // N°, Élève, [Cours,Compo]xN, Moyenne, Rang
            float[] widths = new float[nCols];
            widths[0] = 0.5f; widths[1] = 2.6f;
            for (int i = 0; i < subjectIds.size() * 2; i++) widths[2 + i] = 0.9f;
            widths[nCols - 2] = 1.1f; widths[nCols - 1] = 0.7f;

            PdfPTable table = new PdfPTable(widths);
            table.setWidthPercentage(100);

            headerCell(table, "N°", brand, 1, 2);
            headerCell(table, "ÉLÈVE", brand, 1, 2);
            for (Long subjectId : subjectIds) {
                headerCell(table, data.subjectName(subjectId), brand, 2, 1);
            }
            headerCell(table, "MOY. GÉN.", brand, 1, 2);
            headerCell(table, "RANG", brand, 1, 2);
            for (int i = 0; i < subjectIds.size(); i++) {
                subHeaderCell(table, "Cours", brand);
                subHeaderCell(table, "Compo", brand);
            }

            int n = 1;
            int i = 0;
            for (StudentRow row : data.rows()) {
                Color bg = (i++ % 2 == 0) ? Color.WHITE : ZEBRA;
                bodyCell(table, String.valueOf(n++), Element.ALIGN_CENTER, bg);
                bodyCell(table, safe(row.studentName()), Element.ALIGN_LEFT, bg);
                for (Long subjectId : subjectIds) {
                    SubjectCell cell = row.bySubject() != null ? row.bySubject().get(subjectId) : null;
                    bodyCell(table, dec(cell == null ? null : cell.cours()), Element.ALIGN_CENTER, bg);
                    bodyCell(table, dec(cell == null ? null : cell.compo()), Element.ALIGN_CENTER, bg);
                }
                bodyCell(table, dec(row.overallAverage()), Element.ALIGN_CENTER, bg);
                bodyCell(table, row.rank() == null ? "—" : String.valueOf(row.rank()), Element.ALIGN_CENTER, bg);
            }
            if (data.rows().isEmpty()) {
                PdfPCell empty = new PdfPCell(new Phrase("Aucun bulletin généré pour cette classe et cette période.",
                        SchoolPdfHeader.font(FontFactory.HELVETICA_OBLIQUE, 9.5f, GREY)));
                empty.setColspan(nCols);
                empty.setHorizontalAlignment(Element.ALIGN_CENTER);
                empty.setPadding(12f);
                table.addCell(empty);
            }
            doc.add(table);

            doc.close();
            return out.toByteArray();
        } catch (RuntimeException e) {
            if (doc.isOpen()) {
                doc.close();
            }
            throw e;
        }
    }

    private static void headerCell(PdfPTable table, String text, Color brand, int colspan, int rowspan) {
        PdfPCell cell = new PdfPCell(new Phrase(text, SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 7.5f, Color.WHITE)));
        cell.setColspan(colspan);
        cell.setRowspan(rowspan);
        cell.setBackgroundColor(brand);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4f);
        cell.setBorderColor(brand);
        table.addCell(cell);
    }

    private static void subHeaderCell(PdfPTable table, String text, Color brand) {
        PdfPCell cell = new PdfPCell(new Phrase(text, SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 7f, Color.WHITE)));
        cell.setBackgroundColor(SchoolPdfHeader.tint(brand, 0.85));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(3f);
        cell.setBorderColor(brand);
        table.addCell(cell);
    }

    private static void bodyCell(PdfPTable table, String text, int alignment, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, SchoolPdfHeader.font(FontFactory.HELVETICA, 7.5f, DARK)));
        cell.setHorizontalAlignment(alignment);
        cell.setBackgroundColor(bg);
        cell.setPadding(3.5f);
        cell.setBorderColor(new Color(232, 234, 237));
        table.addCell(cell);
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

    private static String dec(BigDecimal v) { return v == null ? "—" : v.toPlainString(); }

    private static String safe(String s) { return s == null || s.isBlank() ? "—" : s; }
}
