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
import gn.lacaeducation.grading.application.port.out.AssessmentGradeSheetPdfGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Fiche de notes d'UNE évaluation précise (ex. « Anglais — Oral — Octobre ») : liste des élèves avec
 * leur note déjà saisie, ou une ligne vide sinon — téléchargeable/partageable (contrairement à
 * l'aperçu imprimé côté client qui existait avant).
 */
@Component
@RequiredArgsConstructor
class OpenPdfAssessmentGradeSheetGenerator implements AssessmentGradeSheetPdfGenerator {

    private static final Color GREY = SchoolPdfHeader.GREY;
    private static final Color DARK = SchoolPdfHeader.DARK;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final TenantInfoProvider tenantInfoProvider;

    @Override
    public byte[] generate(AssessmentGradeSheet sheet) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 36, 36, 36, 50);
        try {
            TenantPublicInfo info = sheet.tenantId() == null ? null
                    : tenantInfoProvider.findTenantInfo(sheet.tenantId()).orElse(null);
            Color brand = SchoolPdfHeader.parseColor(info == null ? null : info.brandColor());

            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new SchoolPdfPageFooter(info, brand));
            doc.open();

            SchoolPdfHeader.render(doc, tenantInfoProvider, sheet.tenantId(), "FICHE DE NOTES");

            Paragraph title = new Paragraph(safe(sheet.assessmentLabel()).toUpperCase(),
                    SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 14, brand));
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(6f);
            doc.add(title);

            PdfPTable meta = new PdfPTable(new float[]{1f, 1f, 1f, 1f});
            meta.setWidthPercentage(100);
            meta.setSpacingBefore(10f);
            meta.setSpacingAfter(10f);
            metaCell(meta, "Matière", safe(sheet.subjectName()));
            metaCell(meta, "Classe", safe(sheet.className()));
            metaCell(meta, "Type", safe(sheet.assessmentTypeLabel()));
            metaCell(meta, "Date", sheet.date() != null ? sheet.date().format(DATE_FMT) : "—");

            doc.add(meta);

            PdfPTable table = new PdfPTable(new float[]{0.6f, 3f, 1.2f});
            table.setWidthPercentage(100);
            headerCell(table, "N°", brand);
            headerCell(table, "PRÉNOMS ET NOM", brand);
            headerCell(table, "NOTE / " + trimZeros(sheet.maxScore()), brand);

            int n = 1;
            List<StudentGradeRow> rows = sheet.rows() != null ? sheet.rows() : List.of();
            for (StudentGradeRow row : rows) {
                blankCell(table, String.valueOf(n++), Element.ALIGN_CENTER);
                blankCell(table, row.studentName(), Element.ALIGN_LEFT);
                blankCell(table, row.value() != null ? trimZeros(row.value()) : "", Element.ALIGN_CENTER);
            }
            doc.add(table);

            PdfPTable sign = new PdfPTable(new float[]{1f, 1f});
            sign.setWidthPercentage(60);
            sign.setHorizontalAlignment(Element.ALIGN_CENTER);
            sign.setSpacingBefore(24f);
            signatureCell(sign, "Le Chargé de cours");
            signatureCell(sign, "Le Censeur");
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

    private static void metaCell(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(new Color(225, 228, 232));
        cell.setPadding(7f);
        cell.addElement(new Paragraph(label.toUpperCase(), SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 7f, GREY)));
        Paragraph v = new Paragraph(value, SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 10, DARK));
        v.setSpacingBefore(2f);
        cell.addElement(v);
        table.addCell(cell);
    }

    private static void headerCell(PdfPTable table, String text, Color brand) {
        PdfPCell cell = new PdfPCell(new Phrase(text, SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 8f, Color.WHITE)));
        cell.setBackgroundColor(brand);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5f);
        cell.setBorderColor(brand);
        table.addCell(cell);
    }

    private static void blankCell(PdfPTable table, String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, SchoolPdfHeader.font(FontFactory.HELVETICA, 9f, DARK)));
        cell.setHorizontalAlignment(alignment);
        cell.setMinimumHeight(18f);
        cell.setPadding(4f);
        cell.setBorderColor(new Color(210, 213, 218));
        table.addCell(cell);
    }

    private static void signatureCell(PdfPTable table, String label) {
        PdfPCell s = new PdfPCell();
        s.setBorder(0);
        Paragraph line = new Paragraph("_______________________", SchoolPdfHeader.font(FontFactory.HELVETICA, 9, new Color(180, 180, 180)));
        line.setAlignment(Element.ALIGN_CENTER);
        s.addElement(line);
        Paragraph r = new Paragraph(label, SchoolPdfHeader.font(FontFactory.HELVETICA, 9, GREY));
        r.setAlignment(Element.ALIGN_CENTER);
        r.setSpacingBefore(3f);
        s.addElement(r);
        table.addCell(s);
    }

    private static String safe(String s) { return s == null || s.isBlank() ? "—" : s; }

    private static String trimZeros(java.math.BigDecimal v) {
        if (v == null) return "—";
        return v.stripTrailingZeros().toPlainString();
    }
}
