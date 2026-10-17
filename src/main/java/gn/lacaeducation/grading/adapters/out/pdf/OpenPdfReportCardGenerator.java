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
import gn.lacaeducation.grading.application.port.out.ReportCardPdfGenerator;
import gn.lacaeducation.grading.domain.ReportCard;
import gn.lacaeducation.grading.domain.ReportCardLine;
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
 * Bulletin de notes (secondaire : une colonne par mois d'évaluations "cours" + Composition + Moyenne ;
 * primaire : juste Composition + Moyenne, pas de notion de "cours" dans ce cycle) — reproduit la mise
 * en page d'un bulletin réellement distribué (§retour fondateur, "bulletin-test"), avec le même en-tête
 * sobre que le reçu de paiement ({@link AdminDocumentLayout}) au lieu du bandeau coloré précédent.
 */
@Component
@RequiredArgsConstructor
class OpenPdfReportCardGenerator implements ReportCardPdfGenerator {

    private static final Color GREY = AdminDocumentLayout.GREY;
    private static final Color DARK = AdminDocumentLayout.DARK;
    private static final Color ZEBRA = new Color(247, 248, 250);
    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);
    private static final int SCALE = 2;

    private final TenantInfoProvider tenantInfoProvider;

    @Override
    public byte[] generate(ReportCard rc, List<ReportCardLine> lines, ReportCardContext ctx) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 36, 36, 40, 54);
        try {
            TenantPublicInfo info = ctx.tenantId() == null ? null
                    : tenantInfoProvider.findTenantInfo(ctx.tenantId()).orElse(null);
            Color brand = SchoolPdfHeader.parseColor(info == null ? null : info.brandColor());

            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new SchoolPdfPageFooter(info, brand));
            doc.open();

            header(doc, info, ctx.schoolYear());
            titleBox(doc, "BULLETIN SCOLAIRE");

            String sub = join(" · ", ctx.periodLabel(), ctx.schoolYear());
            if (!sub.isBlank()) {
                Paragraph subtitle = new Paragraph(sub, font(FontFactory.HELVETICA, 10, GREY));
                subtitle.setAlignment(Element.ALIGN_CENTER);
                subtitle.setSpacingAfter(10f);
                doc.add(subtitle);
            }

            // ── Identité de l'élève ────────────────────────────────────────────
            PdfPTable ident = new PdfPTable(new float[]{1f, 1f, 1f});
            ident.setWidthPercentage(100);
            ident.setSpacingAfter(12f);
            identCell(ident, "Élève", safe(ctx.studentName(), "—"));
            identCell(ident, "Matricule", safe(ctx.studentNumber(), "—"));
            identCell(ident, "Classe", safe(ctx.className(), "—"));
            doc.add(ident);

            Map<Long, SubjectMonthlyBreakdown> monthlyBySubject = (ctx.monthlyBreakdown() == null ? List.<SubjectMonthlyBreakdown>of() : ctx.monthlyBreakdown())
                    .stream().collect(java.util.stream.Collectors.toMap(SubjectMonthlyBreakdown::subjectId, b -> b, (a, b) -> a));
            // Secondaire si au moins une matière a un détail "cours"/composition — vide en primaire
            // (voir SubjectSemesterAverage) : même test qu'avant (coursAverage/compoAverage non nuls).
            boolean secondaire = !monthlyBySubject.isEmpty()
                    || lines.stream().anyMatch(l -> l.getCoursAverage() != null || l.getCompoAverage() != null);

            List<YearMonth> months = secondaire ? allMonths(monthlyBySubject) : List.of();

            if (secondaire) {
                renderSecondaireTable(doc, lines, monthlyBySubject, months, ctx, brand);
            } else {
                renderPrimaireTable(doc, lines, ctx, brand);
            }

            // ── Synthèse : moyenne générale + statistiques de classe ───────────
            PdfPTable synth = new PdfPTable(new float[]{1.3f, 2.7f});
            synth.setWidthPercentage(100);
            synth.setSpacingAfter(10f);

            PdfPCell avg = new PdfPCell();
            avg.setBackgroundColor(brand);
            avg.setBorder(0);
            avg.setPadding(12f);
            avg.setHorizontalAlignment(Element.ALIGN_CENTER);
            Paragraph avgLabel = new Paragraph("MOYENNE GÉNÉRALE", font(FontFactory.HELVETICA_BOLD, 8.5f, Color.WHITE));
            avgLabel.setAlignment(Element.ALIGN_CENTER);
            avg.addElement(avgLabel);
            Paragraph avgVal = new Paragraph(dec(rc.getOverallAverage()) + " / 20", font(FontFactory.HELVETICA_BOLD, 22, Color.WHITE));
            avgVal.setAlignment(Element.ALIGN_CENTER);
            avg.addElement(avgVal);
            String rank = rc.getRank() == null ? null : "Rang : " + rc.getRank() + " / " + str(rc.getClassSize());
            if (rank != null) {
                Paragraph r = new Paragraph(rank, font(FontFactory.HELVETICA, 9, Color.WHITE));
                r.setAlignment(Element.ALIGN_CENTER);
                r.setSpacingBefore(3f);
                avg.addElement(r);
            }
            synth.addCell(avg);

            PdfPCell stats = new PdfPCell();
            stats.setBackgroundColor(tint(brand, 0.08));
            stats.setBorder(0);
            stats.setPadding(10f);
            stats.addElement(kv("Moyenne de la classe", dec(rc.getClassAverage())));
            stats.addElement(kv("Plus forte moyenne", dec(rc.getHighestAverage())));
            stats.addElement(kv("Plus faible moyenne", dec(rc.getLowestAverage())));
            stats.addElement(kv("Effectif", rc.getClassSize() == null ? "—" : str(rc.getClassSize())));
            synth.addCell(stats);
            doc.add(synth);

            // ── Mention & décision ─────────────────────────────────────────────
            PdfPTable verdict = new PdfPTable(new float[]{1f, 1f});
            verdict.setWidthPercentage(100);
            verdict.setSpacingAfter(6f);
            verdict.addCell(verdictCell("Mention", safe(rc.getHonor(), "—"), brand));
            verdict.addCell(verdictCell("Décision", safe(rc.getDecision(), "—"), brand));
            doc.add(verdict);

            if (rc.getDistinction() != null && !"NONE".equals(rc.getDistinction().name())) {
                Paragraph dist = new Paragraph("Appréciation du conseil de classe : " + distinctionLabel(rc.getDistinction().name()),
                        font(FontFactory.HELVETICA_BOLD, 10, brand));
                dist.setSpacingBefore(2f);
                doc.add(dist);
            }
            if (rc.getComment() != null && !rc.getComment().isBlank()) {
                Paragraph comment = new Paragraph("Appréciation générale : " + rc.getComment(),
                        font(FontFactory.HELVETICA, 9.5f, DARK));
                comment.setSpacingBefore(6f);
                doc.add(comment);
            }

            // ── Signatures ─────────────────────────────────────────────────────
            PdfPTable sign = new PdfPTable(new float[]{1f, 1f, 1f});
            sign.setWidthPercentage(100);
            sign.setSpacingBefore(28f);
            signatureCell(sign, "Le Professeur principal");
            signatureCell(sign, "Les Parents / Tuteur");
            signatureCell(sign, "Le Chef d'établissement");
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

    /** Secondaire : Matière | Coef | <mois...> | Composition | Moyenne | Rang, + TOTAL/MOYENNE OBTENUE
     * pondérées par coefficient — même principe que la ligne "MOYENNES" du bulletin annuel. */
    private void renderSecondaireTable(Document doc, List<ReportCardLine> lines,
                                       Map<Long, SubjectMonthlyBreakdown> monthlyBySubject, List<YearMonth> months,
                                       ReportCardContext ctx, Color brand) {
        float[] widths = new float[4 + months.size()];
        widths[0] = 2.2f;
        widths[1] = 0.7f;
        for (int i = 0; i < months.size(); i++) widths[2 + i] = 0.9f;
        widths[2 + months.size()] = 1f;
        widths[3 + months.size()] = 1f;
        float[] withRank = java.util.Arrays.copyOf(widths, widths.length + 1);
        withRank[withRank.length - 1] = 0.7f;

        PdfPTable table = new PdfPTable(withRank);
        table.setWidthPercentage(100);
        table.setSpacingAfter(12f);

        headerCell(table, "Matière", brand);
        headerCell(table, "Coef.", brand);
        for (YearMonth m : months) headerCell(table, monthLabel(m), brand);
        headerCell(table, "Composition", brand);
        headerCell(table, "Moyenne", brand);
        headerCell(table, "Rang", brand);

        BigDecimal totalCoef = BigDecimal.ZERO;
        BigDecimal[] monthTotals = new BigDecimal[months.size()];
        java.util.Arrays.fill(monthTotals, BigDecimal.ZERO);
        BigDecimal compoTotal = BigDecimal.ZERO;

        boolean zebra = false;
        for (ReportCardLine line : lines) {
            Color bg = zebra ? ZEBRA : Color.WHITE;
            zebra = !zebra;
            SubjectMonthlyBreakdown breakdown = monthlyBySubject.get(line.getSubjectId());
            BigDecimal coef = line.getCoefficient() != null ? line.getCoefficient() : BigDecimal.ONE;

            String subject = ctx.subjectName(line.getSubjectId());
            if (line.getTeacherName() != null && !line.getTeacherName().isBlank()) {
                subject += "\n" + line.getTeacherName();
            }
            bodyCell(table, subject, Element.ALIGN_LEFT, bg, line.getTeacherName() != null);
            bodyCell(table, dec(coef), Element.ALIGN_CENTER, bg, false);
            for (int i = 0; i < months.size(); i++) {
                BigDecimal v = breakdown == null ? null : breakdown.monthlyCours().get(months.get(i));
                bodyCell(table, dec(v), Element.ALIGN_CENTER, bg, false);
                if (v != null) monthTotals[i] = monthTotals[i].add(v.multiply(coef, MC).setScale(SCALE, RoundingMode.HALF_UP));
            }
            bodyCell(table, dec(line.getCompoAverage()), Element.ALIGN_CENTER, bg, false);
            if (line.getCompoAverage() != null) compoTotal = compoTotal.add(line.getCompoAverage().multiply(coef, MC).setScale(SCALE, RoundingMode.HALF_UP));
            bodyCell(table, dec(line.getSubjectAverage()), Element.ALIGN_CENTER, bg, false);
            bodyCell(table, str(line.getSubjectRank()), Element.ALIGN_CENTER, bg, false);

            totalCoef = totalCoef.add(coef);
        }

        // Ligne TOTAL : somme pondérée par coefficient de chaque colonne (vérifié par calcul manuel sur
        // le bulletin de référence : Σ(valeur_du_mois × coef)) ; MOYENNE OBTENUE = TOTAL / Σcoef — les
        // deux dernières colonnes (Moyenne/Rang) restent vides, non applicables à une ligne de synthèse.
        totalCell(table, "TOTAL", Element.ALIGN_LEFT);
        totalCell(table, dec(totalCoef), Element.ALIGN_CENTER);
        for (BigDecimal monthTotal : monthTotals) totalCell(table, dec(monthTotal), Element.ALIGN_CENTER);
        totalCell(table, dec(compoTotal), Element.ALIGN_CENTER);
        totalCell(table, "", Element.ALIGN_CENTER);
        totalCell(table, "", Element.ALIGN_CENTER);

        boolean hasCoef = totalCoef.compareTo(BigDecimal.ZERO) > 0;
        totalCell(table, "MOYENNE OBTENUE SUR 20", Element.ALIGN_LEFT);
        totalCell(table, "", Element.ALIGN_CENTER);
        for (BigDecimal monthTotal : monthTotals) {
            totalCell(table, hasCoef ? dec(monthTotal.divide(totalCoef, SCALE, RoundingMode.HALF_UP)) : "—", Element.ALIGN_CENTER);
        }
        totalCell(table, hasCoef ? dec(compoTotal.divide(totalCoef, SCALE, RoundingMode.HALF_UP)) : "—", Element.ALIGN_CENTER);
        totalCell(table, "", Element.ALIGN_CENTER);
        totalCell(table, "", Element.ALIGN_CENTER);

        doc.add(table);
    }

    /** Primaire : Matière | Composition | Moyenne | Rang — pas de coefficient (toujours neutralisé à 1
     * dans ce cycle, voir SubjectSemesterAverage) ni de colonne mensuelle (pas de notion de "cours"). */
    private void renderPrimaireTable(Document doc, List<ReportCardLine> lines, ReportCardContext ctx, Color brand) {
        PdfPTable table = new PdfPTable(new float[]{3.4f, 1.3f, 1.3f, 0.9f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(12f);
        headerCell(table, "Matière", brand);
        headerCell(table, "Composition", brand);
        headerCell(table, "Moyenne", brand);
        headerCell(table, "Rang", brand);

        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        boolean zebra = false;
        if (lines.isEmpty()) {
            PdfPCell empty = new PdfPCell(new Phrase("Aucune note pour cette période.", font(FontFactory.HELVETICA_OBLIQUE, 9.5f, GREY)));
            empty.setColspan(4);
            empty.setHorizontalAlignment(Element.ALIGN_CENTER);
            empty.setPadding(10f);
            table.addCell(empty);
        }
        for (ReportCardLine line : lines) {
            Color bg = zebra ? ZEBRA : Color.WHITE;
            zebra = !zebra;
            String subject = ctx.subjectName(line.getSubjectId());
            if (line.getTeacherName() != null && !line.getTeacherName().isBlank()) {
                subject += "\n" + line.getTeacherName();
            }
            bodyCell(table, subject, Element.ALIGN_LEFT, bg, line.getTeacherName() != null);
            bodyCell(table, dec(line.getSubjectAverage()), Element.ALIGN_CENTER, bg, false);
            bodyCell(table, dec(line.getSubjectAverage()), Element.ALIGN_CENTER, bg, false);
            bodyCell(table, str(line.getSubjectRank()), Element.ALIGN_CENTER, bg, false);
            if (line.getSubjectAverage() != null) { sum = sum.add(line.getSubjectAverage()); count++; }
        }
        // TOTAL/MOYENNE : simple somme/moyenne, sans pondération (cohérent avec la formule primaire,
        // voir SubjectSemesterAverage) — Composition et Moyenne affichent la même valeur comme pour
        // chaque ligne élève (en primaire, la "moyenne" d'une matière EST la moyenne des compositions).
        BigDecimal avg = count > 0 ? sum.divide(BigDecimal.valueOf(count), SCALE, RoundingMode.HALF_UP) : null;
        totalCell(table, "TOTAL", Element.ALIGN_LEFT);
        totalCell(table, dec(sum), Element.ALIGN_CENTER);
        totalCell(table, dec(sum), Element.ALIGN_CENTER);
        totalCell(table, "", Element.ALIGN_CENTER);
        totalCell(table, "MOYENNE OBTENUE SUR 20", Element.ALIGN_LEFT);
        totalCell(table, dec(avg), Element.ALIGN_CENTER);
        totalCell(table, dec(avg), Element.ALIGN_CENTER);
        totalCell(table, "", Element.ALIGN_CENTER);
        doc.add(table);
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

    // ── Cell helpers ────────────────────────────────────────────────────────────
    private static void identCell(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(new Color(225, 228, 232));
        cell.setPadding(8f);
        Paragraph l = new Paragraph(label.toUpperCase(), font(FontFactory.HELVETICA_BOLD, 7.5f, GREY));
        cell.addElement(l);
        Paragraph v = new Paragraph(value, font(FontFactory.HELVETICA_BOLD, 11, DARK));
        v.setSpacingBefore(2f);
        cell.addElement(v);
        table.addCell(cell);
    }

    private static void headerCell(PdfPTable table, String text, Color brand) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(FontFactory.HELVETICA_BOLD, 8.5f, Color.WHITE)));
        cell.setBackgroundColor(brand);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5f);
        cell.setBorderColor(brand);
        table.addCell(cell);
    }

    private static void bodyCell(PdfPTable table, String text, int alignment, Color bg, boolean multiline) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(FontFactory.HELVETICA, multiline ? 8.5f : 9f, DARK)));
        cell.setHorizontalAlignment(alignment);
        cell.setBackgroundColor(bg);
        cell.setPadding(5f);
        cell.setBorderColor(new Color(232, 234, 237));
        table.addCell(cell);
    }

    private static void totalCell(PdfPTable table, String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(FontFactory.HELVETICA_BOLD, 8.5f, DARK)));
        cell.setHorizontalAlignment(alignment);
        cell.setBackgroundColor(new Color(240, 242, 245));
        cell.setPadding(5f);
        table.addCell(cell);
    }

    private static Paragraph kv(String key, String value) {
        Paragraph p = new Paragraph();
        p.add(new Phrase(key + " : ", font(FontFactory.HELVETICA, 9f, GREY)));
        p.add(new Phrase(value, font(FontFactory.HELVETICA_BOLD, 9.5f, DARK)));
        p.setSpacingAfter(3f);
        return p;
    }

    private static PdfPCell verdictCell(String label, String value, Color brand) {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(new Color(225, 228, 232));
        cell.setPadding(9f);
        cell.addElement(new Paragraph(label.toUpperCase(), font(FontFactory.HELVETICA_BOLD, 7.5f, GREY)));
        Paragraph v = new Paragraph(value, font(FontFactory.HELVETICA_BOLD, 12, brand));
        v.setSpacingBefore(2f);
        cell.addElement(v);
        return cell;
    }

    private static void signatureCell(PdfPTable table, String role) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(0);
        cell.setPaddingTop(2f);
        Paragraph line = new Paragraph("_______________________", font(FontFactory.HELVETICA, 9, new Color(180, 180, 180)));
        line.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(line);
        Paragraph r = new Paragraph(role, font(FontFactory.HELVETICA, 8.5f, GREY));
        r.setAlignment(Element.ALIGN_CENTER);
        r.setSpacingBefore(3f);
        cell.addElement(r);
        table.addCell(cell);
    }

    private static String distinctionLabel(String name) {
        return switch (name) {
            case "FELICITATIONS" -> "Félicitations";
            case "COMPLIMENTS" -> "Compliments";
            case "ENCOURAGEMENTS" -> "Encouragements";
            case "TABLEAU_HONNEUR" -> "Tableau d'honneur";
            case "MISE_EN_GARDE" -> "Mise en garde";
            default -> name;
        };
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

    private static String dec(BigDecimal value) {
        return value == null ? "—" : value.toPlainString();
    }

    private static String str(Object value) {
        return value == null ? "—" : value.toString();
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
