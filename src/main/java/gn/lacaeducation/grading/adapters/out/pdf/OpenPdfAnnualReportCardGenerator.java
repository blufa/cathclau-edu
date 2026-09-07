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
import gn.lacaeducation.grading.application.port.out.AnnualReportCardPdfGenerator;
import gn.lacaeducation.grading.domain.AnnualReportCard;
import gn.lacaeducation.grading.domain.AnnualReportCardLine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

/**
 * Bulletin de notes annuel officiel (§documents ministère) : reproduit la mise en page du bulletin
 * réellement distribué par le GSP Christine Camara (N°|MATIERES|COEF|[1er SEMESTRE: COURS|COMPO|MOY]
 * |[2ème SEMESTRE: COURS|COMPO|MOY]|MOY ANNUELLE, avec lignes TOTAL et MOYENNES).
 */
@Component
@RequiredArgsConstructor
class OpenPdfAnnualReportCardGenerator implements AnnualReportCardPdfGenerator {

    private static final Color GREY = SchoolPdfHeader.GREY;
    private static final Color DARK = SchoolPdfHeader.DARK;
    private static final Color ZEBRA = new Color(247, 248, 250);
    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);

    private final TenantInfoProvider tenantInfoProvider;

    @Override
    public byte[] generate(AnnualReportCard card, AnnualReportCardContext ctx) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 30, 30, 36, 50);
        try {
            TenantPublicInfo info = ctx.tenantId() == null ? null
                    : tenantInfoProvider.findTenantInfo(ctx.tenantId()).orElse(null);
            Color brand = SchoolPdfHeader.parseColor(info == null ? null : info.brandColor());

            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new SchoolPdfPageFooter(info, brand));
            doc.open();

            SchoolPdfHeader.render(doc, tenantInfoProvider, ctx.tenantId(), "BULLETIN DE NOTES ANNUELLES");

            Paragraph title = new Paragraph("BULLETIN DE NOTES ANNUELLES", SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 15, brand));
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(6f);
            doc.add(title);

            String sub = join(" · ", ctx.schoolYear(), ctx.className());
            if (!sub.isBlank()) {
                Paragraph subtitle = new Paragraph(sub, SchoolPdfHeader.font(FontFactory.HELVETICA, 10, GREY));
                subtitle.setAlignment(Element.ALIGN_CENTER);
                subtitle.setSpacingAfter(4f);
                doc.add(subtitle);
            }

            Paragraph student = new Paragraph(
                    "Prénoms et Nom : " + safe(ctx.studentName()) + (ctx.studentNumber() != null ? "   —   Matricule : " + ctx.studentNumber() : ""),
                    SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 11, DARK));
            student.setSpacingAfter(10f);
            doc.add(student);

            List<AnnualReportCardLine> lines = card.getLines() != null ? card.getLines() : List.of();

            PdfPTable table = new PdfPTable(new float[]{0.6f, 3f, 0.9f, 1f, 1f, 1.1f, 1f, 1f, 1.1f, 1.2f});
            table.setWidthPercentage(100);
            headerCell(table, "N°", brand, 1, 2);
            headerCell(table, "MATIÈRES", brand, 1, 2);
            headerCell(table, "COEF", brand, 1, 2);
            headerCell(table, ctx.s1Label() != null ? ctx.s1Label().toUpperCase() : "1ER SEMESTRE", brand, 3, 1);
            headerCell(table, ctx.s2Label() != null ? ctx.s2Label().toUpperCase() : "2ÈME SEMESTRE", brand, 3, 1);
            headerCell(table, "MOY ANNUELLE", brand, 1, 2);
            for (String h : new String[]{"COURS", "COMPO", "MOY", "COURS", "COMPO", "MOY"}) {
                subHeaderCell(table, h, brand);
            }

            int n = 1;
            BigDecimal sumCoef = BigDecimal.ZERO;
            BigDecimal sumS1Cours = BigDecimal.ZERO, sumS1Compo = BigDecimal.ZERO;
            BigDecimal sumS2Cours = BigDecimal.ZERO, sumS2Compo = BigDecimal.ZERO;
            int i = 0;
            for (AnnualReportCardLine l : lines) {
                Color bg = (i++ % 2 == 0) ? Color.WHITE : ZEBRA;
                bodyCell(table, String.valueOf(n++), Element.ALIGN_CENTER, bg);
                bodyCell(table, ctx.subjectName(l.getSubjectId()), Element.ALIGN_LEFT, bg);
                bodyCell(table, dec(l.getCoefficient()), Element.ALIGN_CENTER, bg);
                bodyCell(table, dec(l.getS1Cours()), Element.ALIGN_CENTER, bg);
                bodyCell(table, dec(l.getS1Compo()), Element.ALIGN_CENTER, bg);
                bodyCell(table, dec(l.getS1Average()), Element.ALIGN_CENTER, bg);
                bodyCell(table, dec(l.getS2Cours()), Element.ALIGN_CENTER, bg);
                bodyCell(table, dec(l.getS2Compo()), Element.ALIGN_CENTER, bg);
                bodyCell(table, dec(l.getS2Average()), Element.ALIGN_CENTER, bg);
                bodyCell(table, dec(l.getAnnualAverage()), Element.ALIGN_CENTER, bg);

                if (l.getCoefficient() != null) sumCoef = sumCoef.add(l.getCoefficient());
                if (l.getS1Cours() != null) sumS1Cours = sumS1Cours.add(l.getS1Cours());
                if (l.getS1Compo() != null) sumS1Compo = sumS1Compo.add(l.getS1Compo());
                if (l.getS2Cours() != null) sumS2Cours = sumS2Cours.add(l.getS2Cours());
                if (l.getS2Compo() != null) sumS2Compo = sumS2Compo.add(l.getS2Compo());
            }

            // Ligne TOTAL : somme des colonnes cours/composition de l'élève.
            totalCell(table, "TOTAL", Element.ALIGN_LEFT, 2);
            totalCell(table, dec(sumCoef), Element.ALIGN_CENTER, 1);
            totalCell(table, dec(sumS1Cours), Element.ALIGN_CENTER, 1);
            totalCell(table, dec(sumS1Compo), Element.ALIGN_CENTER, 1);
            totalCell(table, "", Element.ALIGN_CENTER, 1);
            totalCell(table, dec(sumS2Cours), Element.ALIGN_CENTER, 1);
            totalCell(table, dec(sumS2Compo), Element.ALIGN_CENTER, 1);
            totalCell(table, "", Element.ALIGN_CENTER, 1);
            totalCell(table, "", Element.ALIGN_CENTER, 1);

            // Ligne MOYENNES : moyenne de cours (simple) / composition (pondérée coef) / générale — même
            // méthode que la moyenne générale du bulletin (§GradeCalculationService.computeOverallAverage).
            BigDecimal s1CoursGeneral = simpleAverage(lines, AnnualReportCardLine::getS1Cours);
            BigDecimal s1CompoGeneral = weightedAverage(lines, AnnualReportCardLine::getS1Compo);
            BigDecimal s2CoursGeneral = simpleAverage(lines, AnnualReportCardLine::getS2Cours);
            BigDecimal s2CompoGeneral = weightedAverage(lines, AnnualReportCardLine::getS2Compo);
            totalCell(table, "MOYENNES", Element.ALIGN_LEFT, 2);
            totalCell(table, "", Element.ALIGN_CENTER, 1);
            totalCell(table, dec(s1CoursGeneral), Element.ALIGN_CENTER, 1);
            totalCell(table, dec(s1CompoGeneral), Element.ALIGN_CENTER, 1);
            totalCell(table, dec(card.getS1Average()), Element.ALIGN_CENTER, 1);
            totalCell(table, dec(s2CoursGeneral), Element.ALIGN_CENTER, 1);
            totalCell(table, dec(s2CompoGeneral), Element.ALIGN_CENTER, 1);
            totalCell(table, dec(card.getS2Average()), Element.ALIGN_CENTER, 1);
            totalCell(table, dec(card.getOverallAverage()), Element.ALIGN_CENTER, 1);

            doc.add(table);

            Paragraph avg = new Paragraph(
                    "Moyenne annuelle générale : " + dec(card.getOverallAverage()) + " / 20"
                            + (card.getRank() != null ? "   ·   Rang : " + card.getRank() + (card.getClassSize() != null ? "e/" + card.getClassSize() : "") : ""),
                    SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 12, brand));
            avg.setAlignment(Element.ALIGN_CENTER);
            avg.setSpacingBefore(14f);
            doc.add(avg);

            Paragraph appreciation = new Paragraph(
                    "APPRÉCIATION GÉNÉRALE : " + decisionLabel(card.getDecision())
                            + "          MENTION : " + safe(card.getHonor()),
                    SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 10.5f, DARK));
            appreciation.setAlignment(Element.ALIGN_CENTER);
            appreciation.setSpacingBefore(8f);
            doc.add(appreciation);

            PdfPTable sign = new PdfPTable(new float[]{1f, 1f});
            sign.setWidthPercentage(60);
            sign.setHorizontalAlignment(Element.ALIGN_CENTER);
            sign.setSpacingBefore(30f);
            signatureCell(sign, "Le Censeur");
            signatureCell(sign, "Le Proviseur");
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

    private static String decisionLabel(String decision) {
        if (decision == null) return "—";
        return switch (decision) {
            case "ADMIS" -> "ADMIS";
            case "A_REPECHER" -> "À REPÊCHER";
            case "REDOUBLE" -> "REDOUBLE";
            default -> decision;
        };
    }

    private static BigDecimal simpleAverage(List<AnnualReportCardLine> lines,
                                            java.util.function.Function<AnnualReportCardLine, BigDecimal> getter) {
        List<BigDecimal> values = lines.stream().map(getter).filter(java.util.Objects::nonNull).toList();
        if (values.isEmpty()) return null;
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), MC).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal weightedAverage(List<AnnualReportCardLine> lines,
                                              java.util.function.Function<AnnualReportCardLine, BigDecimal> getter) {
        BigDecimal num = BigDecimal.ZERO;
        BigDecimal den = BigDecimal.ZERO;
        for (AnnualReportCardLine l : lines) {
            BigDecimal v = getter.apply(l);
            if (v == null) continue;
            BigDecimal coeff = l.getCoefficient() != null ? l.getCoefficient() : BigDecimal.ONE;
            num = num.add(v.multiply(coeff, MC));
            den = den.add(coeff);
        }
        return den.signum() == 0 ? null : num.divide(den, MC).setScale(2, RoundingMode.HALF_UP);
    }

    private static void headerCell(PdfPTable table, String text, Color brand, int colspan, int rowspan) {
        PdfPCell cell = new PdfPCell(new Phrase(text, SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 8.5f, Color.WHITE)));
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
        PdfPCell cell = new PdfPCell(new Phrase(text, SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 8f, Color.WHITE)));
        cell.setBackgroundColor(SchoolPdfHeader.tint(brand, 0.85));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(3f);
        cell.setBorderColor(brand);
        table.addCell(cell);
    }

    private static void bodyCell(PdfPTable table, String text, int alignment, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, SchoolPdfHeader.font(FontFactory.HELVETICA, 8.5f, DARK)));
        cell.setHorizontalAlignment(alignment);
        cell.setBackgroundColor(bg);
        cell.setPadding(4f);
        cell.setBorderColor(new Color(232, 234, 237));
        table.addCell(cell);
    }

    private static void totalCell(PdfPTable table, String text, int alignment, int colspan) {
        PdfPCell cell = new PdfPCell(new Phrase(text, SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 8.5f, DARK)));
        cell.setColspan(colspan);
        cell.setHorizontalAlignment(alignment);
        cell.setBackgroundColor(new Color(240, 242, 245));
        cell.setPadding(4f);
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
