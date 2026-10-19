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
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

/**
 * Bulletin de notes de fin d'année — reproduit fidèlement le modèle papier de référence
 * (§retour fondateur, "bulletin-test.webp") : le tableau principal n'affiche QUE le détail du 2ème
 * semestre (une colonne par mois + Composition), le 1er semestre n'apparaissant que sous forme de
 * moyenne déjà connue dans le bloc BILAN — pas de colonne Moyenne/Rang par matière (fidèle à la
 * référence), seul le rang de l'élève figure, une fois, dans le BILAN.
 */
@Component
@RequiredArgsConstructor
class OpenPdfAnnualReportCardGenerator implements AnnualReportCardPdfGenerator {

    private static final Color GREY = AdminDocumentLayout.GREY;
    private static final Color DARK = AdminDocumentLayout.DARK;
    private static final Color RED = new Color(196, 30, 30);
    private static final Color GREEN_BANNER = new Color(124, 176, 56);
    private static final Color GREEN_LIGHT = new Color(223, 236, 188);
    private static final Color ZEBRA = new Color(247, 248, 250);
    private static final int SCALE = 2;

    private final TenantInfoProvider tenantInfoProvider;

    @Override
    public byte[] generate(AnnualReportCard card, AnnualReportCardContext ctx) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 36, 36, 40, 54);
        try {
            TenantPublicInfo info = ctx.tenantId() == null ? null
                    : tenantInfoProvider.findTenantInfo(ctx.tenantId()).orElse(null);

            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new SchoolPdfPageFooter(info, RED));
            doc.open();

            BulletinHeader.render(doc, info, RED);

            banner(doc, "BULLETIN DE NOTES DE FIN D'ANNÉE", RED);
            if (ctx.schoolYear() != null && !ctx.schoolYear().isBlank()) {
                Paragraph year = new Paragraph("Année Scolaire : " + ctx.schoolYear(), font(FontFactory.HELVETICA, 9.5f, GREY));
                year.setAlignment(Element.ALIGN_CENTER);
                year.setSpacingAfter(8f);
                doc.add(year);
            }

            identityBlock(doc, ctx);

            List<AnnualReportCardLine> lines = card.getLines() != null ? card.getLines() : List.of();
            Map<Long, SubjectMonthlyBreakdown> s2Monthly = (ctx.s2MonthlyBreakdown() == null ? List.<SubjectMonthlyBreakdown>of() : ctx.s2MonthlyBreakdown())
                    .stream().collect(java.util.stream.Collectors.toMap(SubjectMonthlyBreakdown::subjectId, b -> b, (a, b) -> a));
            List<YearMonth> months = allMonths(s2Monthly);

            String s2 = safe(ctx.s2Label(), "DEUXIÈME SEMESTRE").toUpperCase(Locale.FRENCH);
            banner(doc, "NOTES DU " + s2, RED);

            float[] widths = new float[4 + months.size()];
            widths[0] = 0.4f;
            widths[1] = 2.4f;
            widths[2] = 0.6f;
            for (int i = 0; i < months.size(); i++) widths[3 + i] = 0.9f;
            widths[3 + months.size()] = 1f;

            PdfPTable table = new PdfPTable(widths);
            table.setWidthPercentage(100);
            table.setSpacingAfter(4f);
            headerCell(table, "N°");
            headerCell(table, "Matières");
            headerCell(table, "Coef");
            for (YearMonth m : months) headerCell(table, monthLabel(m));
            headerCell(table, "Composition");

            int n = 1;
            BigDecimal totalCoef = BigDecimal.ZERO;
            BigDecimal[] monthTotals = new BigDecimal[months.size()];
            java.util.Arrays.fill(monthTotals, BigDecimal.ZERO);
            BigDecimal compoTotal = BigDecimal.ZERO;
            boolean zebra = false;
            for (AnnualReportCardLine l : lines) {
                Color bg = zebra ? ZEBRA : Color.WHITE;
                zebra = !zebra;
                BigDecimal coef = l.getCoefficient() != null ? l.getCoefficient() : BigDecimal.ONE;
                SubjectMonthlyBreakdown breakdown = s2Monthly.get(l.getSubjectId());

                bodyCell(table, String.valueOf(n++), Element.ALIGN_CENTER, bg);
                bodyCell(table, ctx.subjectName(l.getSubjectId()), Element.ALIGN_LEFT, bg);
                bodyCell(table, dec(coef), Element.ALIGN_CENTER, bg);
                for (int m = 0; m < months.size(); m++) {
                    BigDecimal v = breakdown == null ? null : breakdown.monthlyCours().get(months.get(m));
                    bodyCell(table, dec(v), Element.ALIGN_CENTER, bg);
                    if (v != null) monthTotals[m] = monthTotals[m].add(v);
                }
                bodyCell(table, dec(l.getS2Compo()), Element.ALIGN_CENTER, bg);
                if (l.getS2Compo() != null) compoTotal = compoTotal.add(l.getS2Compo());

                totalCoef = totalCoef.add(coef);
            }

            // Le coefficient est déjà appliqué dans chaque case (voir SubjectSemesterAverage, confirmé
            // avec l'utilisateur) : TOTAL = simple somme de colonne, MOYENNE OBTENUE = TOTAL / Σcoef.
            totalCell(table, "TOTAL", Element.ALIGN_LEFT, 2);
            totalCell(table, dec(totalCoef), Element.ALIGN_CENTER, 1);
            for (BigDecimal monthTotal : monthTotals) totalCell(table, dec(monthTotal), Element.ALIGN_CENTER, 1);
            totalCell(table, dec(compoTotal), Element.ALIGN_CENTER, 1);

            boolean hasCoef = totalCoef.compareTo(BigDecimal.ZERO) > 0;
            totalCell(table, "MOYENNE OBTENUE SUR 20", Element.ALIGN_LEFT, 2, RED);
            totalCell(table, "", Element.ALIGN_CENTER, 1, RED);
            for (BigDecimal monthTotal : monthTotals) {
                totalCell(table, hasCoef ? dec(monthTotal.divide(totalCoef, SCALE, RoundingMode.HALF_UP)) : "—", Element.ALIGN_CENTER, 1, RED);
            }
            totalCell(table, hasCoef ? dec(compoTotal.divide(totalCoef, SCALE, RoundingMode.HALF_UP)) : "—", Element.ALIGN_CENTER, 1, RED);

            doc.add(table);

            bilan(doc, card);
            signature(doc, info);

            doc.close();
            return out.toByteArray();
        } catch (RuntimeException e) {
            if (doc.isOpen()) {
                doc.close();
            }
            throw e;
        }
    }

    // ── En-tête de section ──────────────────────────────────────────────────────
    private static void banner(Document doc, String text, Color bg) {
        doc.add(bannerTable(text, bg));
    }

    private static PdfPTable bannerTable(String text, Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(text, font(FontFactory.HELVETICA_BOLD, 13, Color.WHITE)));
        c.setBackgroundColor(bg);
        c.setPadding(7f);
        c.setBorder(0);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(6f);
        t.setSpacingAfter(4f);
        t.addCell(c);
        return t;
    }

    // ── Identité élève : 4 lignes empilées libellé/valeur, bien visibles (demande explicite) ──
    private static void identityBlock(Document doc, AnnualReportCardContext ctx) {
        PdfPTable t = new PdfPTable(new float[]{1.3f, 2.7f});
        t.setWidthPercentage(100);
        t.setSpacingAfter(10f);
        infoRow(t, "Prénom(s) et Nom", safe(ctx.studentName(), "—"));
        infoRow(t, "Matricule", safe(ctx.studentNumber(), "—"));
        infoRow(t, "Option / Niveau", safe(ctx.cycleLabel(), "—"));
        infoRow(t, "Classe", safe(ctx.className(), "—"));
        doc.add(t);
    }

    private static void infoRow(PdfPTable t, String label, String value) {
        PdfPCell l = new PdfPCell(new Phrase(label.toUpperCase(Locale.FRENCH), font(FontFactory.HELVETICA_BOLD, 9.5f, DARK)));
        l.setBackgroundColor(new Color(237, 239, 242));
        l.setPadding(7f);
        l.setBorderColor(DARK);
        l.setBorderWidth(0.8f);
        l.setVerticalAlignment(Element.ALIGN_MIDDLE);
        t.addCell(l);
        PdfPCell v = new PdfPCell(new Phrase(value, font(FontFactory.HELVETICA_BOLD, 12.5f, DARK)));
        v.setBackgroundColor(GREEN_LIGHT);
        v.setPadding(7f);
        v.setBorderColor(DARK);
        v.setBorderWidth(0.8f);
        v.setVerticalAlignment(Element.ALIGN_MIDDLE);
        t.addCell(v);
    }

    /** BILAN : moyennes des deux semestres + annuelle, rang affiché une seule fois (rowspan, fidèle à
     * la référence), décision finale, et retards/absences (toujours affichés, même à 0 — demande
     * explicite). Bandeau + tableau regroupés dans une seule cellule keepTogether (voir bilan() du
     * bulletin semestriel) : empêche "BILAN" de rester seul en bas de page. */
    private static void bilan(Document doc, AnnualReportCard card) {
        int absent = card.getAbsentCount() != null ? card.getAbsentCount() : 0;
        int late = card.getLateCount() != null ? card.getLateCount() : 0;

        PdfPTable t = new PdfPTable(new float[]{2.4f, 1.3f, 1.3f, 1.4f});
        t.setWidthPercentage(100);

        bilanLabel(t, "Moyenne du premier semestre");
        bilanValue(t, dec(card.getS1Average()) + " / 20");
        bilanValue(t, honorFor(card.getS1Average()));
        PdfPCell rang = new PdfPCell(new Phrase(
                card.getRank() != null ? card.getRank() + "e / " + str(card.getClassSize()) + " élèves" : "—",
                font(FontFactory.HELVETICA_BOLD, 13, RED)));
        rang.setRowspan(5);
        rang.setHorizontalAlignment(Element.ALIGN_CENTER);
        rang.setVerticalAlignment(Element.ALIGN_MIDDLE);
        rang.setBorderColor(new Color(225, 228, 232));
        t.addCell(rang);

        bilanLabel(t, "Moyenne du deuxième semestre");
        bilanValue(t, dec(card.getS2Average()) + " / 20");
        bilanValue(t, honorFor(card.getS2Average()));

        bilanLabel(t, "Moyenne annuelle");
        bilanValue(t, dec(card.getOverallAverage()) + " / 20");
        bilanValue(t, safe(card.getHonor(), "—"));

        bilanLabel(t, "Retards");
        bilanValue(t, String.valueOf(late));
        bilanValue(t, "");
        bilanLabel(t, "Absences");
        bilanValue(t, String.valueOf(absent));
        bilanValue(t, "");

        bilanLabel(t, "Décision finale");
        PdfPCell decision = new PdfPCell(new Phrase(decisionLabel(card.getDecision()), font(FontFactory.HELVETICA_BOLD, 10.5f, RED)));
        decision.setColspan(2);
        decision.setHorizontalAlignment(Element.ALIGN_CENTER);
        decision.setPadding(6f);
        decision.setBorderColor(new Color(225, 228, 232));
        t.addCell(decision);
        PdfPCell fillRang = new PdfPCell(new Phrase(""));
        fillRang.setBorderColor(new Color(225, 228, 232));
        t.addCell(fillRang);

        // Bandeau + tableau regroupés dans UNE SEULE cellule avec keepTogether : empêche que "BILAN"
        // reste seul en bas d'une page pendant que le tableau démarre sur la suivante (vu en test réel).
        PdfPCell outer = new PdfPCell();
        outer.setBorder(0);
        outer.setPadding(0);
        outer.addElement(bannerTable("BILAN", GREEN_BANNER));
        outer.addElement(t);
        PdfPTable wrapper = new PdfPTable(1);
        wrapper.setWidthPercentage(100);
        wrapper.setSpacingAfter(16f);
        wrapper.setKeepTogether(true);
        wrapper.addCell(outer);
        doc.add(wrapper);
    }

    private static void bilanLabel(PdfPTable t, String label) {
        PdfPCell c = new PdfPCell(new Phrase(label.toUpperCase(Locale.FRENCH), font(FontFactory.HELVETICA_BOLD, 9, DARK)));
        c.setPadding(6f);
        c.setBorderColor(new Color(225, 228, 232));
        t.addCell(c);
    }

    private static void bilanValue(PdfPTable t, String value) {
        PdfPCell c = new PdfPCell(new Phrase(value, font(FontFactory.HELVETICA_BOLD, 10.5f, DARK)));
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPadding(6f);
        c.setBorderColor(new Color(225, 228, 232));
        t.addCell(c);
    }

    /** Appréciation par semestre (§fidèle à la référence, qui répète la mention pour chaque ligne du
     * BILAN) — recalculée avec le même barème standard que celui qui calcule déjà card.getHonor(). */
    private static String honorFor(BigDecimal average) {
        return average == null ? "—" : gn.lacaeducation.grading.domain.GradingScale.defaults().honorFor(average);
    }

    // ── Signature (fidèle à la référence : une seule signature, pas trois) ────────
    private static void signature(Document doc, TenantPublicInfo info) {
        String city = info != null ? info.city() : null;
        String date = java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH).format(java.time.LocalDate.now());

        PdfPCell cell = new PdfPCell();
        cell.setBorder(0);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Paragraph faitA = new Paragraph(
                (city != null && !city.isBlank() ? "Fait à " + city + ", " : "Fait ") + "le " + date,
                font(FontFactory.HELVETICA_OBLIQUE, 9.5f, DARK));
        faitA.setAlignment(Element.ALIGN_RIGHT);
        cell.addElement(faitA);

        Paragraph role = new Paragraph("Le Principal", font(FontFactory.HELVETICA_BOLD, 10, DARK));
        role.setAlignment(Element.ALIGN_RIGHT);
        role.setSpacingBefore(4f);
        cell.addElement(role);

        if (info != null && info.director() != null && !info.director().isBlank()) {
            Paragraph director = new Paragraph(info.director(), font(FontFactory.HELVETICA_BOLD, 10, DARK));
            director.setAlignment(Element.ALIGN_RIGHT);
            director.setSpacingBefore(22f);
            cell.addElement(director);
        }

        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(10f);
        t.setKeepTogether(true);
        t.addCell(cell);
        doc.add(t);
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

    private static String decisionLabel(String decision) {
        if (decision == null) return "—";
        return switch (decision) {
            case "ADMIS" -> "PASSE EN CLASSE SUPÉRIEURE";
            case "A_REPECHER" -> "À REPÊCHER";
            case "REDOUBLE" -> "REDOUBLE";
            default -> decision;
        };
    }

    // ── Cell helpers ────────────────────────────────────────────────────────────
    private static void headerCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(FontFactory.HELVETICA_BOLD, 8.5f, Color.WHITE)));
        cell.setBackgroundColor(DARK);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5f);
        table.addCell(cell);
    }

    private static void bodyCell(PdfPTable table, String text, int alignment, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(FontFactory.HELVETICA, 9f, DARK)));
        cell.setHorizontalAlignment(alignment);
        cell.setBackgroundColor(bg);
        cell.setPadding(5f);
        cell.setBorderColor(new Color(232, 234, 237));
        table.addCell(cell);
    }

    private static void totalCell(PdfPTable table, String text, int alignment, int colspan) {
        totalCell(table, text, alignment, colspan, DARK);
    }

    private static void totalCell(PdfPTable table, String text, int alignment, int colspan, Color textColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(FontFactory.HELVETICA_BOLD, 9f, textColor)));
        cell.setColspan(colspan);
        cell.setHorizontalAlignment(alignment);
        cell.setBackgroundColor(new Color(240, 242, 245));
        cell.setPadding(5f);
        table.addCell(cell);
    }

    private static com.lowagie.text.Font font(String base, float size, Color color) {
        com.lowagie.text.Font f = FontFactory.getFont(base, size);
        f.setColor(color);
        return f;
    }

    private static String dec(BigDecimal v) { return v == null ? "—" : v.toPlainString(); }

    private static String str(Object v) { return v == null ? "—" : v.toString(); }

    private static String safe(String s, String fallback) { return s == null || s.isBlank() ? fallback : s; }
}
