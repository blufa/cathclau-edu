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
import gn.lacaeducation.common.pdf.AdminDocumentLayout;
import gn.lacaeducation.common.pdf.SchoolPdfHeader;
import gn.lacaeducation.common.pdf.SchoolPdfPageFooter;
import gn.lacaeducation.grading.application.port.out.AnnualReportCardPdfGenerator;
import gn.lacaeducation.grading.domain.AnnualReportCard;
import gn.lacaeducation.grading.domain.AnnualReportCardLine;
import gn.lacaeducation.grading.domain.SubjectMonthlyBreakdown;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

import static gn.lacaeducation.common.pdf.AdminDocumentLayout.header;
import static gn.lacaeducation.common.pdf.AdminDocumentLayout.titleBox;

/**
 * Bulletin de notes de fin d'année (§documents ministère, "bulletin-test" de référence) : 1er semestre
 * en colonnes agrégées (COURS/COMPO/MOY), 2ème semestre en colonnes MENSUELLES (une par mois
 * d'évaluations "cours" + Composition + Moyenne) — le détail mensuel du 2ème semestre est celui encore
 * "frais" au moment d'imprimer ce document, le 1er étant déjà connu/archivé. En-tête sobre façon reçu
 * de paiement ({@link AdminDocumentLayout}), bloc BILAN (moyennes des deux semestres + annuelle,
 * décision, retards/absences).
 */
@Component
@RequiredArgsConstructor
class OpenPdfAnnualReportCardGenerator implements AnnualReportCardPdfGenerator {

    private static final Color GREY = AdminDocumentLayout.GREY;
    private static final Color DARK = AdminDocumentLayout.DARK;
    private static final Color ZEBRA = new Color(247, 248, 250);
    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);
    private static final int SCALE = 2;

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

            header(doc, info, ctx.schoolYear());
            titleBox(doc, "BULLETIN DE NOTES DE FIN D'ANNÉE");

            String sub = join(" · ", ctx.schoolYear(), ctx.className());
            if (!sub.isBlank()) {
                Paragraph subtitle = new Paragraph(sub, font(FontFactory.HELVETICA, 10, GREY));
                subtitle.setAlignment(Element.ALIGN_CENTER);
                subtitle.setSpacingAfter(4f);
                doc.add(subtitle);
            }

            Paragraph student = new Paragraph(
                    "Prénoms et Nom : " + safe(ctx.studentName()) + (ctx.studentNumber() != null ? "   —   Matricule : " + ctx.studentNumber() : ""),
                    font(FontFactory.HELVETICA_BOLD, 11, DARK));
            student.setSpacingAfter(10f);
            doc.add(student);

            List<AnnualReportCardLine> lines = card.getLines() != null ? card.getLines() : List.of();
            Map<Long, SubjectMonthlyBreakdown> s2Monthly = (ctx.s2MonthlyBreakdown() == null ? List.<SubjectMonthlyBreakdown>of() : ctx.s2MonthlyBreakdown())
                    .stream().collect(java.util.stream.Collectors.toMap(SubjectMonthlyBreakdown::subjectId, b -> b, (a, b) -> a));
            List<YearMonth> months = allMonths(s2Monthly);

            float[] widths = new float[9 + months.size()];
            widths[0] = 0.5f; widths[1] = 2.3f; widths[2] = 0.7f;
            widths[3] = 0.8f; widths[4] = 0.8f; widths[5] = 0.8f; // S1 : cours, compo, moy
            for (int i = 0; i < months.size(); i++) widths[6 + i] = 0.8f;
            widths[6 + months.size()] = 0.8f; // S2 compo
            widths[7 + months.size()] = 0.8f; // S2 moy
            widths[8 + months.size()] = 1f;   // moy annuelle

            PdfPTable table = new PdfPTable(widths);
            table.setWidthPercentage(100);
            headerCell(table, "N°", brand, 1, 2);
            headerCell(table, "MATIÈRES", brand, 1, 2);
            headerCell(table, "COEF", brand, 1, 2);
            headerCell(table, ctx.s1Label() != null ? ctx.s1Label().toUpperCase() : "1ER SEMESTRE", brand, 3, 1);
            headerCell(table, ctx.s2Label() != null ? ctx.s2Label().toUpperCase() : "2ÈME SEMESTRE", brand, months.size() + 2, 1);
            headerCell(table, "MOY ANNUELLE", brand, 1, 2);
            for (String h : new String[]{"COURS", "COMPO", "MOY"}) subHeaderCell(table, h, brand);
            for (YearMonth m : months) subHeaderCell(table, monthLabel(m), brand);
            subHeaderCell(table, "COMPO", brand);
            subHeaderCell(table, "MOY", brand);

            int n = 1;
            BigDecimal sumCoef = BigDecimal.ZERO;
            BigDecimal sumS1Cours = BigDecimal.ZERO, sumS1Compo = BigDecimal.ZERO;
            BigDecimal[] sumS2Months = new BigDecimal[months.size()];
            java.util.Arrays.fill(sumS2Months, BigDecimal.ZERO);
            BigDecimal sumS2Compo = BigDecimal.ZERO;
            int i = 0;
            for (AnnualReportCardLine l : lines) {
                Color bg = (i++ % 2 == 0) ? Color.WHITE : ZEBRA;
                BigDecimal coef = l.getCoefficient() != null ? l.getCoefficient() : BigDecimal.ONE;
                SubjectMonthlyBreakdown breakdown = s2Monthly.get(l.getSubjectId());

                bodyCell(table, String.valueOf(n++), Element.ALIGN_CENTER, bg);
                bodyCell(table, ctx.subjectName(l.getSubjectId()), Element.ALIGN_LEFT, bg);
                bodyCell(table, dec(coef), Element.ALIGN_CENTER, bg);
                bodyCell(table, dec(l.getS1Cours()), Element.ALIGN_CENTER, bg);
                bodyCell(table, dec(l.getS1Compo()), Element.ALIGN_CENTER, bg);
                bodyCell(table, dec(l.getS1Average()), Element.ALIGN_CENTER, bg);
                for (int m = 0; m < months.size(); m++) {
                    BigDecimal v = breakdown == null ? null : breakdown.monthlyCours().get(months.get(m));
                    bodyCell(table, dec(v), Element.ALIGN_CENTER, bg);
                    if (v != null) sumS2Months[m] = sumS2Months[m].add(v.multiply(coef, MC).setScale(SCALE, RoundingMode.HALF_UP));
                }
                bodyCell(table, dec(l.getS2Compo()), Element.ALIGN_CENTER, bg);
                bodyCell(table, dec(l.getS2Average()), Element.ALIGN_CENTER, bg);
                bodyCell(table, dec(l.getAnnualAverage()), Element.ALIGN_CENTER, bg);

                sumCoef = sumCoef.add(coef);
                if (l.getS1Cours() != null) sumS1Cours = sumS1Cours.add(l.getS1Cours());
                if (l.getS1Compo() != null) sumS1Compo = sumS1Compo.add(l.getS1Compo());
                if (l.getS2Compo() != null) sumS2Compo = sumS2Compo.add(l.getS2Compo());
            }

            // Ligne TOTAL : somme des colonnes (cours S1 simple, S2/compo pondérées par coefficient —
            // même règle que le bulletin semestriel).
            totalCell(table, "TOTAL", Element.ALIGN_LEFT, 2);
            totalCell(table, dec(sumCoef), Element.ALIGN_CENTER, 1);
            totalCell(table, dec(sumS1Cours), Element.ALIGN_CENTER, 1);
            totalCell(table, dec(sumS1Compo), Element.ALIGN_CENTER, 1);
            totalCell(table, "", Element.ALIGN_CENTER, 1);
            for (BigDecimal monthTotal : sumS2Months) totalCell(table, dec(monthTotal), Element.ALIGN_CENTER, 1);
            totalCell(table, dec(sumS2Compo), Element.ALIGN_CENTER, 1);
            totalCell(table, "", Element.ALIGN_CENTER, 1);
            totalCell(table, "", Element.ALIGN_CENTER, 1);

            // Ligne MOYENNES : moyenne de cours (simple) / composition (pondérée coef) / générale — même
            // méthode que la moyenne générale du bulletin.
            BigDecimal s1CoursGeneral = simpleAverage(lines, AnnualReportCardLine::getS1Cours);
            BigDecimal s1CompoGeneral = weightedAverage(lines, AnnualReportCardLine::getS1Compo);
            BigDecimal s2CompoGeneral = weightedAverage(lines, AnnualReportCardLine::getS2Compo);
            boolean hasCoef = sumCoef.compareTo(BigDecimal.ZERO) > 0;
            totalCell(table, "MOYENNES", Element.ALIGN_LEFT, 2);
            totalCell(table, "", Element.ALIGN_CENTER, 1);
            totalCell(table, dec(s1CoursGeneral), Element.ALIGN_CENTER, 1);
            totalCell(table, dec(s1CompoGeneral), Element.ALIGN_CENTER, 1);
            totalCell(table, dec(card.getS1Average()), Element.ALIGN_CENTER, 1);
            for (BigDecimal monthTotal : sumS2Months) {
                totalCell(table, hasCoef ? dec(monthTotal.divide(sumCoef, SCALE, RoundingMode.HALF_UP)) : "—", Element.ALIGN_CENTER, 1);
            }
            totalCell(table, dec(s2CompoGeneral), Element.ALIGN_CENTER, 1);
            totalCell(table, dec(card.getS2Average()), Element.ALIGN_CENTER, 1);
            totalCell(table, dec(card.getOverallAverage()), Element.ALIGN_CENTER, 1);

            doc.add(table);

            // ── BILAN ────────────────────────────────────────────────────────────
            PdfPTable bilanTitle = new PdfPTable(1);
            bilanTitle.setWidthPercentage(100);
            bilanTitle.setSpacingBefore(14f);
            PdfPCell bilanTitleCell = new PdfPCell(new Phrase("BILAN", font(FontFactory.HELVETICA_BOLD, 11, Color.WHITE)));
            bilanTitleCell.setBackgroundColor(brand);
            bilanTitleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            bilanTitleCell.setPadding(6f);
            bilanTitle.addCell(bilanTitleCell);
            doc.add(bilanTitle);

            var attendance = card.getAbsentCount() != null || card.getLateCount() != null;
            PdfPTable bilan = new PdfPTable(new float[]{2.2f, 1f});
            bilan.setWidthPercentage(60);
            bilan.setSpacingBefore(4f);
            // Évite que le BILAN soit coupé entre deux pages (vu en test réel) — bascule le tableau
            // entier sur la page suivante plutôt que de le fragmenter.
            bilan.setKeepTogether(true);
            bilanRow(bilan, "Moyenne du premier semestre", dec(card.getS1Average()) + " / 20");
            bilanRow(bilan, "Moyenne du deuxième semestre", dec(card.getS2Average()) + " / 20");
            bilanRow(bilan, "Moyenne annuelle", dec(card.getOverallAverage()) + " / 20");
            bilanRow(bilan, "Mention", safe(card.getHonor()));
            bilanRow(bilan, "Décision finale", decisionLabel(card.getDecision()));
            if (card.getRank() != null) {
                bilanRow(bilan, "Rang", card.getRank() + (card.getClassSize() != null ? "e / " + card.getClassSize() + " élèves" : ""));
            }
            if (attendance) {
                bilanRow(bilan, "Retards", str(card.getLateCount()));
                bilanRow(bilan, "Absences", str(card.getAbsentCount()));
            }
            doc.add(bilan);

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

    private static List<YearMonth> allMonths(Map<Long, SubjectMonthlyBreakdown> monthlyBySubject) {
        TreeSet<YearMonth> months = new TreeSet<>();
        for (SubjectMonthlyBreakdown b : monthlyBySubject.values()) {
            months.addAll(b.monthlyCours().keySet());
        }
        return List.copyOf(months);
    }

    private static String monthLabel(YearMonth m) {
        return m.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH).toUpperCase(Locale.FRENCH);
    }

    private static void bilanRow(PdfPTable table, String label, String value) {
        PdfPCell l = new PdfPCell(new Phrase(label, font(FontFactory.HELVETICA_BOLD, 9.5f, DARK)));
        l.setBorderColor(new Color(225, 228, 232));
        l.setPadding(6f);
        PdfPCell v = new PdfPCell(new Phrase(value, font(FontFactory.HELVETICA, 9.5f, DARK)));
        v.setBorderColor(new Color(225, 228, 232));
        v.setPadding(6f);
        v.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(l);
        table.addCell(v);
    }

    private static String decisionLabel(String decision) {
        if (decision == null) return "—";
        return switch (decision) {
            case "ADMIS" -> "PASSE EN CLASSE SUPÉRIEURE";
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
        PdfPCell cell = new PdfPCell(new Phrase(text, font(FontFactory.HELVETICA_BOLD, 8.5f, Color.WHITE)));
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
        PdfPCell cell = new PdfPCell(new Phrase(text, font(FontFactory.HELVETICA_BOLD, 8f, Color.WHITE)));
        cell.setBackgroundColor(tint(brand, 0.85));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(3f);
        cell.setBorderColor(brand);
        table.addCell(cell);
    }

    private static void bodyCell(PdfPTable table, String text, int alignment, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(FontFactory.HELVETICA, 8.5f, DARK)));
        cell.setHorizontalAlignment(alignment);
        cell.setBackgroundColor(bg);
        cell.setPadding(4f);
        cell.setBorderColor(new Color(232, 234, 237));
        table.addCell(cell);
    }

    private static void totalCell(PdfPTable table, String text, int alignment, int colspan) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(FontFactory.HELVETICA_BOLD, 8.5f, DARK)));
        cell.setColspan(colspan);
        cell.setHorizontalAlignment(alignment);
        cell.setBackgroundColor(new Color(240, 242, 245));
        cell.setPadding(4f);
        table.addCell(cell);
    }

    private static void signatureCell(PdfPTable table, String label) {
        PdfPCell s = new PdfPCell();
        s.setBorder(0);
        Paragraph line = new Paragraph("_______________________", font(FontFactory.HELVETICA, 9, new Color(180, 180, 180)));
        line.setAlignment(Element.ALIGN_CENTER);
        s.addElement(line);
        Paragraph r = new Paragraph(label, font(FontFactory.HELVETICA, 9, GREY));
        r.setAlignment(Element.ALIGN_CENTER);
        r.setSpacingBefore(3f);
        s.addElement(r);
        table.addCell(s);
    }

    private static Color tint(Color c, double f) {
        return new Color(
                (int) Math.round(255 + (c.getRed() - 255) * f),
                (int) Math.round(255 + (c.getGreen() - 255) * f),
                (int) Math.round(255 + (c.getBlue() - 255) * f));
    }

    private static com.lowagie.text.Font font(String base, float size, Color color) {
        com.lowagie.text.Font f = FontFactory.getFont(base, size);
        f.setColor(color);
        return f;
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

    private static String str(Object v) { return v == null ? "—" : v.toString(); }

    private static String safe(String s) { return s == null || s.isBlank() ? "—" : s; }
}
