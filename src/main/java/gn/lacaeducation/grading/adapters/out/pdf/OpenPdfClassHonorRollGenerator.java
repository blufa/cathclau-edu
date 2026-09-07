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
import gn.lacaeducation.grading.application.port.out.ClassHonorRollPdfGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

/** Renders a class honour roll / ranking (palmarès) to PDF using OpenPDF. */
@Component
@RequiredArgsConstructor
class OpenPdfClassHonorRollGenerator implements ClassHonorRollPdfGenerator {

    private static final Color GREY = SchoolPdfHeader.GREY;
    private static final Color DARK = SchoolPdfHeader.DARK;
    private static final Color ZEBRA = new Color(247, 248, 250);
    private static final Color GOLD = new Color(255, 247, 214);
    private static final Color SILVER = new Color(240, 242, 245);
    private static final Color BRONZE = new Color(250, 240, 230);

    private final TenantInfoProvider tenantInfoProvider;

    @Override
    public byte[] generate(HonorRoll roll) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 40, 54);
        try {
            TenantPublicInfo info = roll.tenantId() == null ? null
                    : tenantInfoProvider.findTenantInfo(roll.tenantId()).orElse(null);
            Color brand = SchoolPdfHeader.parseColor(info == null ? null : info.brandColor());

            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new SchoolPdfPageFooter(info, brand));
            doc.open();

            SchoolPdfHeader.render(doc, tenantInfoProvider, roll.tenantId(), "PALMARÈS — TABLEAU D'HONNEUR");

            Paragraph title = new Paragraph("PALMARÈS — TABLEAU D'HONNEUR", SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 16, brand));
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(8f);
            doc.add(title);

            String sub = join(" · ", roll.className(), roll.periodLabel(), roll.schoolYear());
            if (!sub.isBlank()) {
                Paragraph subtitle = new Paragraph(sub, SchoolPdfHeader.font(FontFactory.HELVETICA, 10, GREY));
                subtitle.setAlignment(Element.ALIGN_CENTER);
                subtitle.setSpacingAfter(6f);
                doc.add(subtitle);
            }
            if (roll.classAverage() != null) {
                Paragraph avg = new Paragraph("Moyenne de la classe : " + dec(roll.classAverage()) + " / 20   ·   Effectif : " + roll.rows().size(),
                        SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 9.5f, DARK));
                avg.setAlignment(Element.ALIGN_CENTER);
                avg.setSpacingAfter(12f);
                doc.add(avg);
            }

            PdfPTable table = new PdfPTable(new float[]{1f, 4.5f, 1.6f, 2.2f});
            table.setWidthPercentage(100);
            for (String h : new String[]{"Rang", "Élève", "Moyenne", "Mention"}) {
                headerCell(table, h, brand);
            }
            int i = 0;
            for (Row r : roll.rows()) {
                Color bg = switch (r.rank() == null ? 0 : r.rank()) {
                    case 1 -> GOLD;
                    case 2 -> SILVER;
                    case 3 -> BRONZE;
                    default -> (i % 2 == 0) ? Color.WHITE : ZEBRA;
                };
                boolean top = r.rank() != null && r.rank() <= 3;
                bodyCell(table, r.rank() == null ? "—" : String.valueOf(r.rank()), Element.ALIGN_CENTER, bg, top);
                bodyCell(table, safe(r.studentName()), Element.ALIGN_LEFT, bg, top);
                bodyCell(table, dec(r.average()), Element.ALIGN_CENTER, bg, top);
                bodyCell(table, safe(r.honor()), Element.ALIGN_CENTER, bg, false);
                i++;
            }
            if (roll.rows().isEmpty()) {
                PdfPCell empty = new PdfPCell(new Phrase("Aucun bulletin généré pour cette classe et cette période.",
                        SchoolPdfHeader.font(FontFactory.HELVETICA_OBLIQUE, 9.5f, GREY)));
                empty.setColspan(4);
                empty.setHorizontalAlignment(Element.ALIGN_CENTER);
                empty.setPadding(12f);
                table.addCell(empty);
            }
            doc.add(table);

            PdfPTable sign = new PdfPTable(new float[]{2f, 1f});
            sign.setWidthPercentage(100);
            sign.setSpacingBefore(30f);
            PdfPCell blank = new PdfPCell(); blank.setBorder(0); sign.addCell(blank);
            PdfPCell s = new PdfPCell(); s.setBorder(0);
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

    private static void headerCell(PdfPTable table, String text, Color brand) {
        PdfPCell cell = new PdfPCell(new Phrase(text, SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 9.5f, Color.WHITE)));
        cell.setBackgroundColor(brand);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(6f);
        cell.setBorderColor(brand);
        table.addCell(cell);
    }

    private static void bodyCell(PdfPTable table, String text, int alignment, Color bg, boolean bold) {
        PdfPCell cell = new PdfPCell(new Phrase(text,
                SchoolPdfHeader.font(bold ? FontFactory.HELVETICA_BOLD : FontFactory.HELVETICA, 10f, DARK)));
        cell.setHorizontalAlignment(alignment);
        cell.setBackgroundColor(bg);
        cell.setPadding(6f);
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
