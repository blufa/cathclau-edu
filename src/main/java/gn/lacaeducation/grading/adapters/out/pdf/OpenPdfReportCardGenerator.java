package gn.lacaeducation.grading.adapters.out.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import gn.lacaeducation.grading.application.port.out.ReportCardPdfGenerator;
import gn.lacaeducation.grading.domain.ReportCard;
import gn.lacaeducation.grading.domain.ReportCardLine;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

/** Renders a report card to a PDF document using OpenPDF. */
@Component
class OpenPdfReportCardGenerator implements ReportCardPdfGenerator {

    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font CELL = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);

    @Override
    public byte[] generate(ReportCard rc, List<ReportCardLine> lines) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document();
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Paragraph title = new Paragraph("BULLETIN DE NOTES", TITLE);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(12f);
            doc.add(title);

            PdfPTable head = new PdfPTable(2);
            head.setWidthPercentage(100);
            head.setSpacingAfter(12f);
            addKeyValue(head, "Élève (ID)", str(rc.getStudentId()));
            addKeyValue(head, "Classe (ID)", str(rc.getClassId()));
            addKeyValue(head, "Période (ID)", str(rc.getPeriodId()));
            addKeyValue(head, "Statut", rc.getStatus() == null ? "-" : rc.getStatus().name());
            doc.add(head);

            PdfPTable table = new PdfPTable(new float[]{3f, 1.2f, 1f, 1.5f, 1f});
            table.setWidthPercentage(100);
            table.setSpacingAfter(12f);
            headerCell(table, "Matière");
            headerCell(table, "Moyenne");
            headerCell(table, "Coef.");
            headerCell(table, "Moy. pondérée");
            headerCell(table, "Rang");
            for (ReportCardLine line : lines) {
                String subject = line.getTeacherName() != null
                        ? "Matière " + str(line.getSubjectId()) + " (" + line.getTeacherName() + ")"
                        : "Matière " + str(line.getSubjectId());
                bodyCell(table, subject, Element.ALIGN_LEFT);
                bodyCell(table, dec(line.getSubjectAverage()), Element.ALIGN_CENTER);
                bodyCell(table, dec(line.getCoefficient()), Element.ALIGN_CENTER);
                bodyCell(table, dec(line.getWeightedAverage()), Element.ALIGN_CENTER);
                bodyCell(table, str(line.getSubjectRank()), Element.ALIGN_CENTER);
            }
            doc.add(table);

            PdfPTable summary = new PdfPTable(2);
            summary.setWidthPercentage(100);
            addKeyValue(summary, "Moyenne générale", dec(rc.getOverallAverage()));
            addKeyValue(summary, "Rang", rc.getRank() == null ? "-" : rc.getRank() + " / " + str(rc.getClassSize()));
            addKeyValue(summary, "Moyenne de classe", dec(rc.getClassAverage()));
            addKeyValue(summary, "Plus forte / plus faible",
                    dec(rc.getHighestAverage()) + " / " + dec(rc.getLowestAverage()));
            addKeyValue(summary, "Mention", rc.getHonor() == null ? "-" : rc.getHonor());
            addKeyValue(summary, "Décision", rc.getDecision() == null ? "-" : rc.getDecision());
            doc.add(summary);

            if (rc.getComment() != null && !rc.getComment().isBlank()) {
                Paragraph comment = new Paragraph("Appréciation : " + rc.getComment(), CELL);
                comment.setSpacingBefore(12f);
                doc.add(comment);
            }

            doc.close();
            return out.toByteArray();
        } catch (RuntimeException e) {
            if (doc.isOpen()) {
                doc.close();
            }
            throw e;
        }
    }

    private static void addKeyValue(PdfPTable table, String key, String value) {
        PdfPCell k = new PdfPCell(new Phrase(key, LABEL));
        k.setBorder(0);
        PdfPCell v = new PdfPCell(new Phrase(value, CELL));
        v.setBorder(0);
        table.addCell(k);
        table.addCell(v);
    }

    private static void headerCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private static void bodyCell(PdfPTable table, String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, CELL));
        cell.setHorizontalAlignment(alignment);
        table.addCell(cell);
    }

    private static String dec(BigDecimal value) {
        return value == null ? "-" : value.toPlainString();
    }

    private static String str(Object value) {
        return value == null ? "-" : value.toString();
    }
}
