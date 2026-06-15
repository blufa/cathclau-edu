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
import gn.lacaeducation.grading.application.port.out.ExamConvocationPdfGenerator;
import gn.lacaeducation.grading.domain.Exam;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

/** Renders an exam convocation to a PDF document using OpenPDF. */
@Component
class OpenPdfExamConvocationGenerator implements ExamConvocationPdfGenerator {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
    private static final Font CELL = FontFactory.getFont(FontFactory.HELVETICA, 11);

    @Override
    public byte[] generate(Exam exam) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document();
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Paragraph title = new Paragraph("CONVOCATION À L'EXAMEN", TITLE);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(16f);
            doc.add(title);

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            addKeyValue(table, "Intitulé", str(exam.getLabel()));
            addKeyValue(table, "Type", exam.getType() == null ? "-" : exam.getType().name());
            addKeyValue(table, "Classe (ID)", str(exam.getClassId()));
            addKeyValue(table, "Période (ID)", str(exam.getPeriodId()));
            addKeyValue(table, "Date", exam.getScheduledDate() == null ? "-" : exam.getScheduledDate().format(DATE));
            addKeyValue(table, "Statut", exam.getStatus() == null ? "-" : exam.getStatus().name());
            doc.add(table);

            Paragraph note = new Paragraph(
                    "Les candidats sont priés de se présenter munis de leur convocation et d'une pièce d'identité.",
                    CELL);
            note.setSpacingBefore(20f);
            doc.add(note);

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

    private static String str(Object value) {
        return value == null ? "-" : value.toString();
    }
}
