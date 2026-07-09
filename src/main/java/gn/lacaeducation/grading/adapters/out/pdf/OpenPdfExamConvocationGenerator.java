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
import gn.lacaeducation.common.pdf.SchoolPdfHeader;
import gn.lacaeducation.common.pdf.SchoolPdfPageFooter;
import gn.lacaeducation.grading.application.port.out.ExamConvocationPdfGenerator;
import gn.lacaeducation.grading.domain.Exam;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

/** Renders a branded exam convocation to PDF using OpenPDF (school header + names + footer). */
@Component
@RequiredArgsConstructor
class OpenPdfExamConvocationGenerator implements ExamConvocationPdfGenerator {

    private static final Color GREY = SchoolPdfHeader.GREY;
    private static final Color DARK = SchoolPdfHeader.DARK;
    private static final Color LINE = new Color(232, 232, 232);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final TenantInfoProvider tenantInfoProvider;

    @Override
    public byte[] generate(Exam exam, ExamConvocationContext ctx) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 40, 54);
        try {
            TenantPublicInfo info = exam.getTenantId() == null ? null
                    : tenantInfoProvider.findTenantInfo(exam.getTenantId()).orElse(null);
            Color brand = SchoolPdfHeader.parseColor(info == null ? null : info.brandColor());

            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new SchoolPdfPageFooter(info == null ? null : info.schoolName(), brand));
            doc.open();

            SchoolPdfHeader.render(doc, tenantInfoProvider, exam.getTenantId());

            Paragraph title = new Paragraph("CONVOCATION À L'EXAMEN", SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 16, brand));
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(8f);
            doc.add(title);

            if (ctx != null && ctx.schoolYear() != null && !ctx.schoolYear().isBlank()) {
                Paragraph sy = new Paragraph("Année scolaire " + ctx.schoolYear(), SchoolPdfHeader.font(FontFactory.HELVETICA, 10, GREY));
                sy.setAlignment(Element.ALIGN_CENTER);
                sy.setSpacingAfter(14f);
                doc.add(sy);
            }

            PdfPTable table = new PdfPTable(new float[]{1.2f, 2.5f});
            table.setWidthPercentage(100);
            table.setSpacingBefore(6f);
            row(table, "Épreuve", safe(exam.getLabel()));
            row(table, "Type", typeLabel(exam));
            row(table, "Classe", ctx == null ? "-" : safe(ctx.className()));
            if (ctx != null && ctx.periodLabel() != null && !ctx.periodLabel().isBlank()) {
                row(table, "Période", ctx.periodLabel());
            }
            row(table, "Date de l'épreuve", exam.getScheduledDate() == null ? "-" : exam.getScheduledDate().format(DATE));
            row(table, "Statut", statusLabel(exam));
            doc.add(table);

            Paragraph note = new Paragraph(
                    "Les candidats sont priés de se présenter au moins 30 minutes avant le début de l'épreuve, "
                            + "munis de leur convocation et d'une pièce d'identité. Tout retard ou absence non justifiée "
                            + "pourra entraîner l'exclusion de l'épreuve.",
                    SchoolPdfHeader.font(FontFactory.HELVETICA, 9.5f, DARK));
            note.setSpacingBefore(22f);
            note.setSpacingAfter(6f);
            doc.add(note);

            // Bloc signature (l'administration).
            PdfPTable sign = new PdfPTable(new float[]{2f, 1f});
            sign.setWidthPercentage(100);
            sign.setSpacingBefore(30f);
            sign.addCell(blank());
            PdfPCell s = new PdfPCell();
            s.setBorder(0);
            Paragraph line = new Paragraph("_______________________", SchoolPdfHeader.font(FontFactory.HELVETICA, 9, new Color(180, 180, 180)));
            line.setAlignment(Element.ALIGN_CENTER);
            s.addElement(line);
            Paragraph r = new Paragraph("Le Chef d'établissement", SchoolPdfHeader.font(FontFactory.HELVETICA, 8.5f, GREY));
            r.setAlignment(Element.ALIGN_CENTER);
            r.setSpacingBefore(3f);
            s.addElement(r);
            sign.addCell(s);
            doc.add(sign);

            doc.close();
            return out.toByteArray();
        } catch (RuntimeException e) {
            if (doc.isOpen()) {
                doc.close();
            }
            throw e;
        }
    }

    private static void row(PdfPTable t, String label, String value) {
        PdfPCell l = new PdfPCell(new Phrase(label, SchoolPdfHeader.font(FontFactory.HELVETICA, 10, GREY)));
        l.setBorder(Rectangle.BOTTOM);
        l.setBorderColor(LINE);
        l.setPadding(7f);
        PdfPCell v = new PdfPCell(new Phrase(value == null ? "-" : value, SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 10.5f, DARK)));
        v.setBorder(Rectangle.BOTTOM);
        v.setBorderColor(LINE);
        v.setPadding(7f);
        t.addCell(l);
        t.addCell(v);
    }

    private static PdfPCell blank() {
        PdfPCell c = new PdfPCell();
        c.setBorder(0);
        return c;
    }

    private static String typeLabel(Exam e) {
        if (e.getType() == null) return "-";
        return switch (e.getType().name()) {
            case "MOCK" -> "Examen blanc";
            case "SEMESTER" -> "Composition semestrielle";
            case "ANNUAL" -> "Examen annuel";
            case "NATIONAL" -> "Examen national";
            default -> e.getType().name();
        };
    }

    private static String statusLabel(Exam e) {
        if (e.getStatus() == null) return "-";
        return switch (e.getStatus().name()) {
            case "PLANNED" -> "Planifié";
            case "ONGOING" -> "En cours";
            case "COMPLETED" -> "Terminé";
            default -> e.getStatus().name();
        };
    }

    private static String safe(String s) {
        return s == null || s.isBlank() ? "-" : s;
    }
}
