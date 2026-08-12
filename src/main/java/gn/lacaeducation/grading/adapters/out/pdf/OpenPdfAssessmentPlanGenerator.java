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
import gn.lacaeducation.grading.application.port.out.AssessmentPlanPdfGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Fiche de notes vierge (§documents ministère) : planning des évaluations imprimé et donné à
 * l'enseignant, qui y consigne à la main les notes mensuelles et les moyennes de cours/composition
 * de chaque période — aucun calcul, uniquement la liste des élèves et des colonnes vides.
 */
@Component
@RequiredArgsConstructor
class OpenPdfAssessmentPlanGenerator implements AssessmentPlanPdfGenerator {

    private static final Color GREY = SchoolPdfHeader.GREY;
    private static final Color DARK = SchoolPdfHeader.DARK;

    private final TenantInfoProvider tenantInfoProvider;

    @Override
    public byte[] generate(AssessmentPlan plan) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 24, 24, 36, 50);
        try {
            TenantPublicInfo info = plan.tenantId() == null ? null
                    : tenantInfoProvider.findTenantInfo(plan.tenantId()).orElse(null);
            Color brand = SchoolPdfHeader.parseColor(info == null ? null : info.brandColor());

            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new SchoolPdfPageFooter(info == null ? null : info.schoolName(), brand));
            doc.open();

            SchoolPdfHeader.render(doc, tenantInfoProvider, plan.tenantId());

            Paragraph title = new Paragraph("FICHE DE NOTES — PLANNING DES ÉVALUATIONS",
                    SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 14, brand));
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(6f);
            doc.add(title);

            PdfPTable meta = new PdfPTable(new float[]{1f, 1f, 1f, 1f});
            meta.setWidthPercentage(100);
            meta.setSpacingBefore(10f);
            meta.setSpacingAfter(10f);
            metaCell(meta, "Nom du chargé de cours", "………………………………………………");
            metaCell(meta, "Matière", safe(plan.subjectName()));
            metaCell(meta, "Classe", safe(plan.className()));
            metaCell(meta, "Année scolaire", safe(plan.schoolYear()));
            doc.add(meta);

            PdfPTable table = new PdfPTable(new float[]{0.5f, 2.2f, 0.8f, 0.8f, 0.8f, 0.9f, 0.9f, 0.9f,
                    0.8f, 0.8f, 0.8f, 0.9f, 0.9f, 0.9f, 0.9f});
            table.setWidthPercentage(100);

            headerCell(table, "N°", brand, 1, 2);
            headerCell(table, "PRÉNOMS ET NOM", brand, 1, 2);
            headerCell(table, "PÉRIODE 1", brand, 6, 1);
            headerCell(table, "PÉRIODE 2", brand, 6, 1);
            headerCell(table, "MOY. AN", brand, 1, 2);
            for (String h : new String[]{"Oct", "Nov", "Déc", "Moy Cours", "Moy Comp", "Moy Trim",
                    "Fév", "Mars", "Avr", "Moy Cours", "Moy Comp", "Moy Trim"}) {
                subHeaderCell(table, h, brand);
            }

            int n = 1;
            List<String> students = plan.studentNames() != null ? plan.studentNames() : List.of();
            for (String name : students) {
                blankCell(table, String.valueOf(n++), Element.ALIGN_CENTER, false);
                blankCell(table, name, Element.ALIGN_LEFT, false);
                for (int i = 0; i < 13; i++) {
                    blankCell(table, "", Element.ALIGN_CENTER, false);
                }
            }
            doc.add(table);

            Paragraph note = new Paragraph(
                    "NB : Veuillez inscrire les noms des élèves ne figurant pas sur la liste.",
                    SchoolPdfHeader.font(FontFactory.HELVETICA_OBLIQUE, 8.5f, GREY));
            note.setSpacingBefore(8f);
            doc.add(note);

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

    private static void headerCell(PdfPTable table, String text, Color brand, int colspan, int rowspan) {
        PdfPCell cell = new PdfPCell(new Phrase(text, SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 8f, Color.WHITE)));
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

    private static void blankCell(PdfPTable table, String text, int alignment, boolean bold) {
        PdfPCell cell = new PdfPCell(new Phrase(text,
                SchoolPdfHeader.font(bold ? FontFactory.HELVETICA_BOLD : FontFactory.HELVETICA, 8f, DARK)));
        cell.setHorizontalAlignment(alignment);
        cell.setMinimumHeight(16f);
        cell.setPadding(3.5f);
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
}
