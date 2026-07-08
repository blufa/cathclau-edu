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

/** Renders a per-subject grade report (relevé de notes) to PDF using OpenPDF. */
@Component
@RequiredArgsConstructor
class OpenPdfSubjectGradeReportGenerator implements SubjectGradeReportPdfGenerator {

    private static final Color GREY = SchoolPdfHeader.GREY;
    private static final Color DARK = SchoolPdfHeader.DARK;
    private static final Color ZEBRA = new Color(247, 248, 250);

    private final TenantInfoProvider tenantInfoProvider;

    @Override
    public byte[] generate(SubjectGradeReport rep) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 36, 36, 40, 54);
        try {
            TenantPublicInfo info = rep.tenantId() == null ? null
                    : tenantInfoProvider.findTenantInfo(rep.tenantId()).orElse(null);
            Color brand = SchoolPdfHeader.parseColor(info == null ? null : info.brandColor());
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new SchoolPdfPageFooter(info == null ? null : info.schoolName(), brand));
            doc.open();

            SchoolPdfHeader.render(doc, tenantInfoProvider, rep.tenantId());

            Paragraph title = new Paragraph("RELEVÉ DE NOTES", SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 16, brand));
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(8f);
            doc.add(title);

            String sub = join(" · ", safe(rep.subjectName()), rep.className(), rep.periodLabel(), rep.schoolYear());
            if (!sub.isBlank()) {
                Paragraph subtitle = new Paragraph(sub, SchoolPdfHeader.font(FontFactory.HELVETICA, 10, GREY));
                subtitle.setAlignment(Element.ALIGN_CENTER);
                subtitle.setSpacingAfter(12f);
                doc.add(subtitle);
            }

            List<ReleveAssessment> cols = rep.assessments();
            // Colonnes : Élève + une par évaluation + Moyenne.
            float[] widths = new float[cols.size() + 2];
            widths[0] = 3.2f;
            for (int i = 1; i <= cols.size(); i++) widths[i] = 1.3f;
            widths[widths.length - 1] = 1.4f;

            PdfPTable table = new PdfPTable(widths);
            table.setWidthPercentage(100);
            table.setHeaderRows(1);

            headerCell(table, "Élève", brand, Element.ALIGN_LEFT);
            int n = 1;
            for (ReleveAssessment a : cols) {
                String head = "Éval " + n + "\n" + safe(a.label());
                if (a.maxScore() != null) head += "\n/" + a.maxScore().toPlainString()
                        + (a.coefficient() != null ? " · coef " + a.coefficient().toPlainString() : "");
                headerCell(table, head, brand, Element.ALIGN_CENTER);
                n++;
            }
            headerCell(table, "Moyenne", brand, Element.ALIGN_CENTER);

            boolean zebra = false;
            for (ReleveRow r : rep.rows()) {
                Color bg = zebra ? ZEBRA : Color.WHITE; zebra = !zebra;
                bodyCell(table, safe(r.fullName()), Element.ALIGN_LEFT, bg, false);
                for (ReleveAssessment a : cols) {
                    BigDecimal v = r.notes() == null ? null : r.notes().get(a.id());
                    bodyCell(table, v == null ? "—" : v.toPlainString(), Element.ALIGN_CENTER, bg, false);
                }
                bodyCell(table, r.average() == null ? "—" : r.average().toPlainString(), Element.ALIGN_CENTER, bg, true);
            }
            if (rep.rows().isEmpty()) {
                PdfPCell empty = new PdfPCell(new Phrase("Aucun élève dans cette classe.",
                        SchoolPdfHeader.font(FontFactory.HELVETICA_OBLIQUE, 9.5f, GREY)));
                empty.setColspan(widths.length);
                empty.setHorizontalAlignment(Element.ALIGN_CENTER);
                empty.setPadding(12f);
                table.addCell(empty);
            }
            doc.add(table);

            if (cols.isEmpty()) {
                Paragraph note = new Paragraph("Aucune évaluation programmée pour cette matière sur la période.",
                        SchoolPdfHeader.font(FontFactory.HELVETICA_OBLIQUE, 9.5f, GREY));
                note.setSpacingBefore(10f);
                doc.add(note);
            } else {
                Paragraph legend = new Paragraph(
                        "La moyenne est calculée selon le paramétrage de l'établissement (méthode, coefficients, arrondi) "
                                + "et se met à jour à chaque nouvelle évaluation.",
                        SchoolPdfHeader.font(FontFactory.HELVETICA_OBLIQUE, 8f, GREY));
                legend.setSpacingBefore(10f);
                doc.add(legend);
            }

            doc.close();
            return out.toByteArray();
        } catch (RuntimeException e) {
            if (doc.isOpen()) doc.close();
            throw e;
        }
    }

    private static void headerCell(PdfPTable table, String text, Color brand, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 8.5f, Color.WHITE)));
        cell.setBackgroundColor(brand);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5f);
        cell.setBorderColor(brand);
        table.addCell(cell);
    }

    private static void bodyCell(PdfPTable table, String text, int align, Color bg, boolean bold) {
        PdfPCell cell = new PdfPCell(new Phrase(text,
                SchoolPdfHeader.font(bold ? FontFactory.HELVETICA_BOLD : FontFactory.HELVETICA, 9.5f, DARK)));
        cell.setHorizontalAlignment(align);
        cell.setBackgroundColor(bg);
        cell.setPadding(5f);
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

    private static String safe(String s) { return s == null || s.isBlank() ? "—" : s; }
}
