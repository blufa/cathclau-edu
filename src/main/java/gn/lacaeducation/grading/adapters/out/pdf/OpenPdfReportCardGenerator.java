package gn.lacaeducation.grading.adapters.out.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.common.TenantPublicInfo;
import gn.lacaeducation.common.pdf.PremiumPageBackground;
import gn.lacaeducation.common.pdf.RoundedCellBorder;
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
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

/**
 * Bulletin de notes — reproduit fidèlement le design premium de référence
 * (§retour fondateur, /features/document-school/Bulletin de notes.dc.html) : fond crème, liseré
 * tricolore guinéen sur le bord gauche, en-tête ministère/logo/République, titre encadré de filets or,
 * carte d'identité élève (grille 2×2), tableau de notes à coins arrondis (N°/Matières/Coef/mois/
 * Composition, SANS colonne moyenne/rang par matière), BILAN en cartes, appréciation + signature.
 * Polices de substitution (Times/Helvetica/Courier, OpenPDF n'embarque pas Cormorant
 * Garamond/Sora/JetBrains Mono) — mêmes proportions et hiérarchie visuelle que la référence.
 */
@Component
@RequiredArgsConstructor
class OpenPdfReportCardGenerator implements ReportCardPdfGenerator {

    // ── Palette (convertie des couleurs hex de la référence) ──────────────────────
    private static final Color DARK_GREEN = new Color(14, 77, 58);
    private static final Color GOLD = new Color(184, 137, 43);
    private static final Color BORDER = new Color(217, 210, 192);
    private static final Color BORDER_LIGHT = new Color(234, 228, 214);
    private static final Color BORDER_LIGHTER = new Color(239, 234, 223);
    private static final Color LABEL_GREY = new Color(138, 122, 85);
    private static final Color TEXT_GREY = new Color(94, 107, 101);
    private static final Color DARK_TEXT = new Color(19, 38, 31);
    private static final Color ROW_STRIPE = new Color(252, 250, 244);
    private static final Color TOTAL_BG = new Color(241, 236, 223);
    private static final Color COMPO_TINT = new Color(251, 244, 226);
    private static final Color COMPO_TOTAL_TINT = new Color(243, 230, 196);
    private static final Color GOLD_LIGHT_TEXT = new Color(233, 215, 158);
    private static final Color DECISION_BORDER = new Color(0, 148, 96);
    private static final Color DECISION_BG = new Color(234, 246, 239);
    private static final Color DECISION_TEXT = new Color(0, 122, 79);
    private static final Color FOOTER_BORDER = new Color(226, 220, 205);
    private static final Color WHITE = Color.WHITE;
    private static final int SCALE = 2;

    private final TenantInfoProvider tenantInfoProvider;

    @Override
    public byte[] generate(ReportCard rc, List<ReportCardLine> lines, ReportCardContext ctx) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // Marge gauche = 12mm de la référence + 3mm occupés par le liseré tricolore (dessiné par
        // PremiumPageBackground, hors-marge, au bord physique de la page).
        Document doc = new Document(PageSize.A4, mm(15), mm(12), mm(10), mm(16));
        try {
            TenantPublicInfo info = ctx.tenantId() == null ? null
                    : tenantInfoProvider.findTenantInfo(ctx.tenantId()).orElse(null);

            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new PremiumPageBackground());
            writer.setPageEvent(new SchoolPdfPageFooter(info, FOOTER_BORDER));
            doc.open();

            header(doc, info, ctx.schoolYear());

            String period = safe(ctx.periodLabel(), "Semestre");
            // Le bulletin du 2ème semestre fait office de bulletin de fin d'année (demande explicite) —
            // détecté par la présence d'un semestre précédent dans la même année scolaire.
            boolean endOfYear = ctx.previousSemesterLabel() != null;
            titleBar(doc, endOfYear ? "Bulletin de notes de fin d'année" : "Bulletin de notes du " + period.toLowerCase(Locale.FRENCH));

            identityCard(doc, ctx);

            Map<Long, SubjectMonthlyBreakdown> monthlyBySubject = (ctx.monthlyBreakdown() == null ? List.<SubjectMonthlyBreakdown>of() : ctx.monthlyBreakdown())
                    .stream().collect(java.util.stream.Collectors.toMap(SubjectMonthlyBreakdown::subjectId, b -> b, (a, b) -> a));
            // Secondaire si au moins une matière a un détail "cours"/composition — vide en primaire
            // (voir SubjectSemesterAverage) : même test qu'avant (coursAverage/compoAverage non nuls).
            boolean secondaire = !monthlyBySubject.isEmpty()
                    || lines.stream().anyMatch(l -> l.getCoursAverage() != null || l.getCompoAverage() != null);

            List<YearMonth> months = secondaire ? allMonths(monthlyBySubject) : List.of();

            if (secondaire) {
                renderSecondaireTable(doc, lines, monthlyBySubject, months, ctx, period);
            } else {
                renderPrimaireTable(doc, lines, ctx, period);
            }

            bilan(doc, rc, ctx, period);

            appreciationAndSignature(doc, rc, info);

            doc.close();
            return out.toByteArray();
        } catch (RuntimeException e) {
            if (doc.isOpen()) {
                doc.close();
            }
            throw e;
        }
    }

    // ── En-tête : ministère/logo/école (gauche) + République/devise/admin (droite) ──
    private static void header(Document doc, TenantPublicInfo info, String schoolYear) {
        PdfPTable t = new PdfPTable(new float[]{1.3f, 1f});
        t.setWidthPercentage(100);
        t.setSpacingAfter(mm(3.8f));

        PdfPCell left = new PdfPCell();
        left.setBorder(0);
        left.setVerticalAlignment(Element.ALIGN_TOP);
        PdfPTable leftInner = new PdfPTable(new float[]{1f, 3f});
        leftInner.setWidthPercentage(100);
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(0);
        logoCell.setFixedHeight(mm(21));
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logoCell.setCellEvent(RoundedCellBorder.fillAndBorder(mm(2), WHITE, BORDER, 0.8f));
        Image logo = info != null ? SchoolPdfHeader.loadLogo(info.logo()) : null;
        if (logo != null) {
            logo.scaleToFit(mm(18), mm(18));
            logo.setAlignment(Element.ALIGN_MIDDLE);
            logoCell.addElement(logo);
        }
        leftInner.addCell(logoCell);
        PdfPCell nameCell = new PdfPCell();
        nameCell.setBorder(0);
        nameCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        nameCell.setPaddingLeft(mm(4));
        Paragraph name = new Paragraph(safe(info != null ? info.schoolName() : null, "École"), font(SERIF_BOLD, FontFactory.TIMES_BOLD, 15, DARK_GREEN));
        name.setLeading(13f);
        nameCell.addElement(name);
        if (schoolYear != null && !schoolYear.isBlank()) {
            Paragraph year = new Paragraph("Année Scolaire : " + schoolYear, font(SANS, FontFactory.HELVETICA, 8, TEXT_GREY));
            year.setSpacingBefore(2f);
            nameCell.addElement(year);
        }
        leftInner.addCell(nameCell);
        left.addElement(leftInner);
        t.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(0);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        right.setVerticalAlignment(Element.ALIGN_TOP);
        Paragraph republic = new Paragraph("RÉPUBLIQUE DE GUINÉE", font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 9, DARK_TEXT));
        republic.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(republic);
        Paragraph motto = new Paragraph("Travail — Justice — Solidarité", font(SERIF_ITALIC, FontFactory.TIMES_BOLDITALIC, 10.5f, TEXT_GREY));
        motto.setAlignment(Element.ALIGN_RIGHT);
        motto.setSpacingBefore(1f);
        right.addElement(motto);
        right.addElement(flagBars());
        String admin = "MEPU-A";
        String ireDpe = join(" · ", prefix("IRE : ", info != null ? info.ire() : null), prefix("DPE/DCE : ", info != null ? info.dpeLabel() : null));
        String sousPref = prefix("DSSEE : ", info != null ? info.sousPrefectureLabel() : null);
        Paragraph adminP = new Paragraph(admin, font(SANS, FontFactory.HELVETICA, 7.5f, TEXT_GREY));
        adminP.setAlignment(Element.ALIGN_RIGHT);
        adminP.setLeading(10.5f);
        adminP.setSpacingBefore(2f);
        if (!ireDpe.isBlank()) adminP.add("\n" + ireDpe);
        if (!sousPref.isBlank()) adminP.add("\n" + sousPref);
        right.addElement(adminP);
        t.addCell(right);

        doc.add(t);
    }

    private static PdfPTable flagBars() {
        PdfPTable bars = new PdfPTable(new float[]{1f, 1f, 1f});
        bars.setWidthPercentage(23f);
        bars.setHorizontalAlignment(Element.ALIGN_RIGHT);
        bars.getDefaultCell().setBorder(0);
        bars.setSpacingBefore(2f);
        bars.setSpacingAfter(2f);
        for (Color c : new Color[]{new Color(206, 17, 38), new Color(252, 209, 22), new Color(0, 148, 96)}) {
            PdfPCell cell = new PdfPCell();
            cell.setBorder(0);
            cell.setFixedHeight(mm(1));
            cell.setCellEvent(RoundedCellBorder.fillOnly(0, c));
            bars.addCell(cell);
        }
        return bars;
    }

    // ── Titre centré encadré de filets or ──────────────────────────────────────────
    private static void titleBar(Document doc, String title) {
        PdfPTable t = new PdfPTable(new float[]{1f, 0f, 1f});
        t.setWidthPercentage(100);
        t.setSpacingAfter(mm(3.8f));
        t.getDefaultCell().setBorder(0);

        PdfPCell line1 = new PdfPCell();
        line1.setBorder(Rectangle.BOTTOM);
        line1.setBorderColorBottom(GOLD);
        line1.setBorderWidthBottom(0.8f);
        line1.setFixedHeight(mm(10));
        t.addCell(line1);

        PdfPCell titleCell = new PdfPCell(new Phrase(title, font(SERIF_BOLD, FontFactory.TIMES_BOLD, 17, DARK_GREEN)));
        titleCell.setBorder(0);
        titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        titleCell.setPadding(0);
        titleCell.setPaddingLeft(mm(3));
        titleCell.setPaddingRight(mm(3));
        titleCell.setNoWrap(true);
        t.addCell(titleCell);

        PdfPCell line2 = new PdfPCell();
        line2.setBorder(Rectangle.BOTTOM);
        line2.setBorderColorBottom(GOLD);
        line2.setBorderWidthBottom(0.8f);
        line2.setFixedHeight(mm(10));
        t.addCell(line2);

        doc.add(t);
    }

    // ── Photo élève (gauche, fidèle à la référence) + carte d'identité : grille 2×2 à coins arrondis ──
    private static void identityCard(Document doc, ReportCardContext ctx) {
        PdfPTable grid = new PdfPTable(new float[]{1.4f, 1f});
        grid.setWidthPercentage(100);
        identityCell(grid, "Prénom(s) et Nom", safe(ctx.studentName(), "—"), 13, true, true);
        identityCell(grid, "Matricule", safe(ctx.studentNumber(), "—"), 11, false, true);
        identityCell(grid, "Option / Niveau", safe(ctx.cycleLabel(), "—"), 11, true, false);
        identityCell(grid, "Classe", safe(ctx.className(), "—"), 11, false, false);

        PdfPCell gridOuter = new PdfPCell();
        gridOuter.setBorder(0);
        gridOuter.setPadding(0);
        gridOuter.setCellEvent(RoundedCellBorder.fillAndBorder(mm(2.5f), WHITE, BORDER, 0.8f));
        gridOuter.addElement(grid);

        PdfPTable row = new PdfPTable(new float[]{0.62f, 3f});
        row.setWidthPercentage(100);
        row.setSpacingAfter(mm(3.8f));

        PdfPCell photoCell = new PdfPCell();
        photoCell.setBorder(0);
        photoCell.setPadding(0);
        photoCell.setFixedHeight(mm(28));
        photoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        photoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        photoCell.setCellEvent(RoundedCellBorder.fillAndBorder(mm(2.5f), new Color(241, 236, 223), BORDER, 0.8f));
        Image photo = SchoolPdfHeader.loadLogo(ctx.studentPhoto());
        if (photo != null) {
            photo.scaleToFit(mm(22), mm(27));
            photo.setAlignment(Element.ALIGN_MIDDLE);
            photoCell.addElement(photo);
        }
        row.addCell(photoCell);
        row.addCell(gridOuter);
        doc.add(row);
    }

    private static void identityCell(PdfPTable grid, String label, String value, float valueSize, boolean borderRight, boolean borderBottom) {
        PdfPCell c = new PdfPCell();
        c.setPadding(mm(2.2f));
        c.setPaddingLeft(mm(3));
        c.setBorder(0);
        if (borderRight) { c.setBorder(c.getBorder() | Rectangle.RIGHT); c.setBorderColorRight(BORDER_LIGHT); c.setBorderWidthRight(0.6f); }
        if (borderBottom) { c.setBorder(c.getBorder() | Rectangle.BOTTOM); c.setBorderColorBottom(BORDER_LIGHT); c.setBorderWidthBottom(0.6f); }
        Paragraph l = new Paragraph(label.toUpperCase(Locale.FRENCH), font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 6.5f, LABEL_GREY));
        c.addElement(l);
        Paragraph v = new Paragraph(value, font(SANS_BOLD, FontFactory.HELVETICA_BOLD, valueSize, DARK_TEXT));
        v.setSpacingBefore(1.5f);
        c.addElement(v);
        grid.addCell(c);
    }

    /** Secondaire : N° | Matière | Coef | <mois...> | Composition — pas de colonne Moyenne/Rang par
     * matière (seule la ligne TOTAL/MOYENNE OBTENUE SUR 20 agrège, fidèle à la référence). */
    private void renderSecondaireTable(Document doc, List<ReportCardLine> lines,
                                       Map<Long, SubjectMonthlyBreakdown> monthlyBySubject, List<YearMonth> months,
                                       ReportCardContext ctx, String period) {
        sectionLabel(doc, "NOTES DU " + period.toUpperCase(Locale.FRENCH));

        float[] widths = new float[4 + months.size()];
        widths[0] = 0.4f;
        widths[1] = 2.4f;
        widths[2] = 0.6f;
        for (int i = 0; i < months.size(); i++) widths[3 + i] = 0.9f;
        widths[3 + months.size()] = 1f;

        PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(100);

        headerCell(table, "N°");
        headerCell(table, "Matières");
        headerCell(table, "Coef");
        for (YearMonth m : months) headerCell(table, monthLabel(m));
        headerCellTint(table, "Composition", GOLD);

        BigDecimal totalCoef = BigDecimal.ZERO;
        BigDecimal[] monthTotals = new BigDecimal[months.size()];
        java.util.Arrays.fill(monthTotals, BigDecimal.ZERO);
        BigDecimal compoTotal = BigDecimal.ZERO;

        boolean zebra = false;
        int n = 1;
        for (ReportCardLine line : lines) {
            Color bg = zebra ? ROW_STRIPE : WHITE;
            zebra = !zebra;
            SubjectMonthlyBreakdown breakdown = monthlyBySubject.get(line.getSubjectId());
            BigDecimal coef = line.getCoefficient() != null ? line.getCoefficient() : BigDecimal.ONE;

            bodyCell(table, String.valueOf(n++), Element.ALIGN_CENTER, bg, LABEL_GREY, true);
            bodyCell(table, ctx.subjectName(line.getSubjectId()), Element.ALIGN_LEFT, bg, DARK_TEXT, true);
            bodyCell(table, dec(coef), Element.ALIGN_CENTER, bg, TEXT_GREY, false);
            for (int i = 0; i < months.size(); i++) {
                BigDecimal v = breakdown == null ? null : breakdown.monthlyCours().get(months.get(i));
                bodyCellMono(table, dec(v), bg);
                if (v != null) monthTotals[i] = monthTotals[i].add(v);
            }
            bodyCellMonoTint(table, dec(line.getCompoAverage()), COMPO_TINT);
            if (line.getCompoAverage() != null) compoTotal = compoTotal.add(line.getCompoAverage());

            totalCoef = totalCoef.add(coef);
        }

        // Le coefficient est déjà appliqué dans chaque case (voir SubjectSemesterAverage, confirmé avec
        // l'utilisateur) : TOTAL = simple somme de colonne, MOYENNE OBTENUE SUR 20 = TOTAL / Σcoef.
        totalCell(table, "TOTAL", Element.ALIGN_LEFT, 2, TOTAL_BG, DARK_TEXT);
        totalCell(table, dec(totalCoef), Element.ALIGN_CENTER, 1, TOTAL_BG, DARK_TEXT);
        for (BigDecimal monthTotal : monthTotals) totalCellMono(table, dec(monthTotal), TOTAL_BG, DARK_TEXT);
        totalCellMono(table, dec(compoTotal), COMPO_TOTAL_TINT, DARK_TEXT);

        boolean hasCoef = totalCoef.compareTo(BigDecimal.ZERO) > 0;
        totalCell(table, "MOYENNE OBTENUE SUR 20", Element.ALIGN_LEFT, 2, DARK_GREEN, WHITE);
        totalCell(table, "", Element.ALIGN_CENTER, 1, DARK_GREEN, WHITE);
        for (BigDecimal monthTotal : monthTotals) {
            totalCellMono(table, hasCoef ? dec(monthTotal.divide(totalCoef, SCALE, RoundingMode.HALF_UP)) : "—", DARK_GREEN, WHITE);
        }
        totalCellMono(table, hasCoef ? dec(compoTotal.divide(totalCoef, SCALE, RoundingMode.HALF_UP)) : "—", GOLD, WHITE);

        roundedTable(doc, table, mm(3.8f));
    }

    /** Primaire : N° | Matière | Composition — pas de coefficient (toujours neutralisé à 1 dans ce
     * cycle, voir SubjectSemesterAverage) ni de colonne mensuelle (pas de notion de "cours"). */
    private void renderPrimaireTable(Document doc, List<ReportCardLine> lines, ReportCardContext ctx, String period) {
        sectionLabel(doc, "NOTES DU " + period.toUpperCase(Locale.FRENCH));

        PdfPTable table = new PdfPTable(new float[]{0.5f, 3.5f, 1.3f});
        table.setWidthPercentage(100);
        headerCell(table, "N°");
        headerCell(table, "Matières");
        headerCellTint(table, "Composition", GOLD);

        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        boolean zebra = false;
        int n = 1;
        if (lines.isEmpty()) {
            PdfPCell empty = new PdfPCell(new Phrase("Aucune note pour cette période.", font(SANS, FontFactory.HELVETICA_OBLIQUE, 9.5f, TEXT_GREY)));
            empty.setColspan(3);
            empty.setHorizontalAlignment(Element.ALIGN_CENTER);
            empty.setPadding(10f);
            table.addCell(empty);
        }
        for (ReportCardLine line : lines) {
            Color bg = zebra ? ROW_STRIPE : WHITE;
            zebra = !zebra;
            bodyCell(table, String.valueOf(n++), Element.ALIGN_CENTER, bg, LABEL_GREY, true);
            bodyCell(table, ctx.subjectName(line.getSubjectId()), Element.ALIGN_LEFT, bg, DARK_TEXT, true);
            bodyCellMonoTint(table, dec(line.getSubjectAverage()), COMPO_TINT);
            if (line.getSubjectAverage() != null) { sum = sum.add(line.getSubjectAverage()); count++; }
        }
        BigDecimal avg = count > 0 ? sum.divide(BigDecimal.valueOf(count), SCALE, RoundingMode.HALF_UP) : null;
        totalCell(table, "TOTAL", Element.ALIGN_LEFT, 2, TOTAL_BG, DARK_TEXT);
        totalCellMono(table, dec(sum), COMPO_TOTAL_TINT, DARK_TEXT);
        totalCell(table, "MOYENNE OBTENUE SUR 20", Element.ALIGN_LEFT, 2, DARK_GREEN, WHITE);
        totalCellMono(table, dec(avg), GOLD, WHITE);
        roundedTable(doc, table, mm(3.8f));
    }

    /** Enveloppe une table à bords droits dans une cellule à coins arrondis — OpenPDF ne permet pas de
     * clipper les coins d'une PdfPTable elle-même (contrairement au CSS {@code overflow:hidden} de la
     * référence), donc on obtient le même rendu visuel en imbriquant : conteneur arrondi + grille droite
     * à l'intérieur (les divisions internes restent droites, comme sur la référence). */
    private static void roundedTable(Document doc, PdfPTable inner, float spacingAfter) {
        PdfPCell outer = new PdfPCell();
        outer.setBorder(0);
        outer.setPadding(0);
        outer.setCellEvent(RoundedCellBorder.borderOnly(mm(2.5f), BORDER, 0.8f));
        outer.addElement(inner);
        PdfPTable wrapper = new PdfPTable(1);
        wrapper.setWidthPercentage(100);
        wrapper.setSpacingAfter(spacingAfter);
        wrapper.addCell(outer);
        doc.add(wrapper);
    }

    private static void sectionLabel(Document doc, String text) {
        Paragraph p = new Paragraph(text, font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 7.5f, DARK_GREEN));
        p.setSpacingAfter(mm(1.8f));
        doc.add(p);
    }

    /** BILAN : moyenne du semestre précédent + moyenne du semestre courant, et — si ce bulletin est le
     * 2ème de l'année, donc fait office de bulletin de fin d'année (demande explicite) — moyenne
     * annuelle (moyenne simple des deux semestres, c'est elle qui détermine le passage en classe
     * supérieure, pas la seule moyenne du 2ème semestre), rang (fidèle à la référence : RANG est sur la
     * MÊME ligne que les moyennes, pas regroupé avec retards/absences/décision) ; 2ème ligne :
     * retards/absences (toujours affichés, même à 0 — assiduité déjà connue via le scan de badge, voir
     * AttendanceSummaryProvider) et décision. */
    private static void bilan(Document doc, ReportCard rc, ReportCardContext ctx, String period) {
        int absent = rc.getAbsentCount() != null ? rc.getAbsentCount() : 0;
        int late = rc.getLateCount() != null ? rc.getLateCount() : 0;
        boolean hasPrevious = ctx.previousSemesterLabel() != null;
        BigDecimal annualAverage = hasPrevious ? average(ctx.previousSemesterAverage(), rc.getOverallAverage()) : null;
        String decisionLabel = hasPrevious
                ? gn.lacaeducation.grading.domain.GradingScale.defaults().decisionFor(annualAverage)
                : rc.getDecision();

        sectionLabel(doc, "BILAN");

        // Ligne 1 : moyenne(s) + RANG côte à côte (fidèle à la référence) — une carte foncée distingue
        // la moyenne "principale" du semestre en cours.
        PdfPTable row1 = cardRow(hasPrevious ? 4 : 2);
        if (hasPrevious) {
            addCard(row1, "Moyenne du " + ctx.previousSemesterLabel().toLowerCase(Locale.FRENCH),
                    dec(ctx.previousSemesterAverage()), "/ 20", safe(ctx.previousSemesterHonor(), "—"), false, true);
            addCard(row1, "Moyenne du " + period.toLowerCase(Locale.FRENCH),
                    dec(rc.getOverallAverage()), "/ 20", safe(rc.getHonor(), "—"), true, false);
            addCard(row1, "Moyenne annuelle", dec(annualAverage), "/ 20",
                    safe(gn.lacaeducation.grading.domain.GradingScale.defaults().honorFor(annualAverage), "—"), false, false);
            addRankCard(row1, rc.getRank(), rc.getClassSize(), false);
        } else {
            addCard(row1, "Moyenne du " + period.toLowerCase(Locale.FRENCH),
                    dec(rc.getOverallAverage()), "/ 20", safe(rc.getHonor(), "—"), true, true);
            addRankCard(row1, rc.getRank(), rc.getClassSize(), false);
        }
        doc.add(row1);

        PdfPTable row2 = cardRow(3);
        addSimpleCard(row2, "Retards", String.valueOf(late), true);
        addSimpleCard(row2, "Absences", String.valueOf(absent), false);
        addDecisionCard(row2, decisionLabel, false);
        doc.add(row2);
    }

    private static PdfPTable cardRow(int cardCount) {
        float[] widths = new float[cardCount * 2 - 1];
        for (int i = 0; i < widths.length; i++) widths[i] = (i % 2 == 0) ? 1f : 0.08f;
        PdfPTable t = new PdfPTable(widths);
        t.setWidthPercentage(100);
        t.setSpacingAfter(mm(3));
        return t;
    }

    private static void spacerCell(PdfPTable row) {
        PdfPCell spacer = new PdfPCell();
        spacer.setBorder(0);
        row.addCell(spacer);
    }

    private static void addCard(PdfPTable row, String label, String value, String unit, String mention, boolean highlighted, boolean first) {
        if (!first) spacerCell(row);
        PdfPCell c = new PdfPCell();
        c.setPadding(mm(2.5f));
        c.setPaddingLeft(mm(3.2f));
        c.setBorder(0);
        c.setCellEvent(highlighted
                ? RoundedCellBorder.fillOnly(mm(2.5f), DARK_GREEN)
                : RoundedCellBorder.fillAndBorder(mm(2.5f), WHITE, BORDER, 0.8f));
        Color labelColor = highlighted ? GOLD_LIGHT_TEXT : LABEL_GREY;
        Color valueColor = highlighted ? WHITE : DARK_TEXT;
        Color unitColor = highlighted ? GOLD_LIGHT_TEXT : TEXT_GREY;
        Color mentionColor = highlighted ? GOLD_LIGHT_TEXT : DARK_GREEN;
        Paragraph l = new Paragraph(label.toUpperCase(Locale.FRENCH), font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 6.5f, labelColor));
        c.addElement(l);
        // Valeur + unité + mention sur UNE seule ligne (fidèle à la référence) : largeur de colonne
        // généreuse pour la valeur (une carte sur 4 depuis l'ajout du RANG à cette ligne, voir bilan())
        // et taille réduite (17→14) pour que "16.50 / 20" ne retombe jamais à la ligne.
        PdfPTable valueRow = new PdfPTable(new float[]{2.1f, 1.2f});
        valueRow.setWidthPercentage(100);
        valueRow.setSpacingBefore(2f);
        PdfPCell valCell = new PdfPCell();
        valCell.setBorder(0);
        valCell.setPadding(0);
        valCell.setNoWrap(true);
        Phrase valPhrase = new Phrase();
        valPhrase.add(new com.lowagie.text.Chunk(value, font(SANS_EXTRABOLD, FontFactory.HELVETICA_BOLD, 14, valueColor)));
        valPhrase.add(new com.lowagie.text.Chunk("  " + unit, font(SANS, FontFactory.HELVETICA, 8, unitColor)));
        valCell.addElement(new Paragraph(valPhrase));
        valueRow.addCell(valCell);
        PdfPCell mentionCell = new PdfPCell(new Phrase(mention, font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 8, mentionColor)));
        mentionCell.setBorder(0);
        mentionCell.setPadding(0);
        mentionCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        mentionCell.setVerticalAlignment(Element.ALIGN_BOTTOM);
        valueRow.addCell(mentionCell);
        c.addElement(valueRow);
        row.addCell(c);
    }

    private static void addRankCard(PdfPTable row, Integer rank, Integer classSize, boolean first) {
        if (!first) spacerCell(row);
        PdfPCell c = new PdfPCell();
        c.setPadding(mm(2.5f));
        c.setPaddingLeft(mm(3.2f));
        c.setBorder(0);
        c.setCellEvent(RoundedCellBorder.fillAndBorder(mm(2.5f), WHITE, BORDER, 0.8f));
        c.addElement(new Paragraph("RANG", font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 6.5f, LABEL_GREY)));
        Phrase p = new Phrase();
        p.add(new com.lowagie.text.Chunk(rank != null ? rank + "e" : "—", font(SANS_EXTRABOLD, FontFactory.HELVETICA_BOLD, 14, DARK_TEXT)));
        p.add(new com.lowagie.text.Chunk(" / " + (classSize != null ? classSize : "—") + " él.", font(SANS, FontFactory.HELVETICA, 8, TEXT_GREY)));
        Paragraph pp = new Paragraph(p);
        pp.setSpacingBefore(2f);
        c.addElement(pp);
        row.addCell(c);
    }

    private static void addSimpleCard(PdfPTable row, String label, String value, boolean first) {
        if (!first) spacerCell(row);
        PdfPCell c = new PdfPCell();
        c.setPadding(mm(2f));
        c.setPaddingLeft(mm(3f));
        c.setPaddingRight(mm(3f));
        c.setBorder(0);
        c.setCellEvent(RoundedCellBorder.fillAndBorder(mm(2.5f), WHITE, BORDER, 0.8f));
        PdfPTable inner = new PdfPTable(new float[]{1f, 1f});
        inner.setWidthPercentage(100);
        PdfPCell l = new PdfPCell(new Phrase(label.toUpperCase(Locale.FRENCH), font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 6.5f, LABEL_GREY)));
        l.setBorder(0);
        l.setPadding(0);
        l.setVerticalAlignment(Element.ALIGN_MIDDLE);
        inner.addCell(l);
        PdfPCell v = new PdfPCell(new Phrase(value, font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 12, DARK_TEXT)));
        v.setBorder(0);
        v.setPadding(0);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        v.setVerticalAlignment(Element.ALIGN_MIDDLE);
        inner.addCell(v);
        c.addElement(inner);
        row.addCell(c);
    }

    private static void addDecisionCard(PdfPTable row, String decisionLabel, boolean first) {
        if (!first) spacerCell(row);
        PdfPCell c = new PdfPCell();
        c.setPadding(mm(2f));
        c.setPaddingLeft(mm(3f));
        c.setPaddingRight(mm(3f));
        c.setBorder(0);
        c.setCellEvent(RoundedCellBorder.fillAndBorder(mm(2.5f), DECISION_BG, DECISION_BORDER, 1f));
        PdfPTable inner = new PdfPTable(new float[]{1f, 1f});
        inner.setWidthPercentage(100);
        PdfPCell l = new PdfPCell(new Phrase("DÉCISION", font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 6.5f, DARK_GREEN)));
        l.setBorder(0);
        l.setPadding(0);
        l.setVerticalAlignment(Element.ALIGN_MIDDLE);
        inner.addCell(l);
        PdfPCell v = new PdfPCell(new Phrase(safe(decisionLabel, "—"), font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 11, DECISION_TEXT)));
        v.setBorder(0);
        v.setPadding(0);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        v.setVerticalAlignment(Element.ALIGN_MIDDLE);
        inner.addCell(v);
        c.addElement(inner);
        row.addCell(c);
    }

    // ── Appréciation du conseil de classe (gauche) + signature (droite) ────────────
    private static void appreciationAndSignature(Document doc, ReportCard rc, TenantPublicInfo info) {
        PdfPTable t = new PdfPTable(new float[]{1.6f, 1f});
        t.setWidthPercentage(100);
        t.setSpacingBefore(mm(4));

        // Carte "appréciation" : barre d'accent or sur le bord gauche (nid de table à 2 colonnes, le
        // tout dans une cellule à coins arrondis), texte en citation.
        PdfPTable appreciationInner = new PdfPTable(new float[]{0.08f, 1f});
        appreciationInner.setWidthPercentage(100);
        PdfPCell bar = new PdfPCell();
        bar.setBorder(0);
        bar.setBackgroundColor(GOLD);
        appreciationInner.addCell(bar);
        PdfPCell content = new PdfPCell();
        content.setBorder(0);
        content.setPadding(mm(3));
        content.addElement(new Paragraph("APPRÉCIATION DU CONSEIL DE CLASSE", font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 6.5f, LABEL_GREY)));
        Paragraph quote = new Paragraph(safe(rc.getComment(), "—"), font(SERIF_ITALIC, FontFactory.TIMES_BOLDITALIC, 12, DARK_TEXT));
        quote.setSpacingBefore(3f);
        content.addElement(quote);
        appreciationInner.addCell(content);

        // Hauteur minimale commune aux deux colonnes (appréciation + signature) : sans elle, iText
        // dimensionne chaque cellule à son propre contenu et la carte d'appréciation (courte) finit
        // visuellement plus basse que le bloc signature (plus grand à cause de l'espace de signature) —
        // "la carte n'est pas au même niveau que la signature" (§retour fondateur).
        float blockHeight = mm(26);
        PdfPCell left = new PdfPCell();
        left.setBorder(0);
        left.setPadding(0);
        left.setMinimumHeight(blockHeight);
        left.setCellEvent(RoundedCellBorder.fillAndBorder(mm(2.5f), WHITE, BORDER, 0.8f));
        left.addElement(appreciationInner);
        t.addCell(left);

        // Signature : date / rôle / espace de signature / filet / nom du directeur.
        String city = info != null ? info.city() : null;
        String date = java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH).format(java.time.LocalDate.now());
        PdfPTable sign = new PdfPTable(1);
        sign.setWidthPercentage(100);

        Paragraph faitA = new Paragraph((city != null && !city.isBlank() ? "Fait à " + city + ", " : "Fait ") + "le " + date,
                font(SANS, FontFactory.HELVETICA, 8.5f, TEXT_GREY));
        faitA.setAlignment(Element.ALIGN_CENTER);
        sign.addCell(noBorderCell(faitA));

        Paragraph role = new Paragraph("Le Principal", font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 9, DARK_TEXT));
        role.setAlignment(Element.ALIGN_CENTER);
        role.setSpacingBefore(2f);
        sign.addCell(noBorderCell(role));

        PdfPCell blank = new PdfPCell(new Phrase(" ", font(SANS, FontFactory.HELVETICA, 11, WHITE)));
        blank.setBorder(0);
        sign.addCell(blank);

        PdfPCell lineHolder = new PdfPCell();
        lineHolder.setBorder(Rectangle.TOP);
        lineHolder.setBorderColorTop(new Color(184, 176, 156));
        lineHolder.setBorderWidthTop(0.6f);
        lineHolder.setPadding(0);
        if (info != null && info.director() != null && !info.director().isBlank()) {
            Paragraph director = new Paragraph(info.director(), font(SERIF_BOLD, FontFactory.TIMES_BOLD, 13, DARK_TEXT));
            director.setAlignment(Element.ALIGN_CENTER);
            director.setSpacingBefore(3f);
            lineHolder.addElement(director);
        } else {
            lineHolder.addElement(new Paragraph(" "));
        }
        sign.addCell(lineHolder);

        PdfPCell right = new PdfPCell();
        right.setBorder(0);
        right.setMinimumHeight(blockHeight);
        right.setVerticalAlignment(Element.ALIGN_BOTTOM);
        right.addElement(sign);
        t.addCell(right);

        PdfPTable wrapper = new PdfPTable(1);
        wrapper.setWidthPercentage(100);
        wrapper.setKeepTogether(true);
        PdfPCell outerCell = new PdfPCell(t);
        outerCell.setBorder(0);
        outerCell.setPadding(0);
        wrapper.addCell(outerCell);
        doc.add(wrapper);
    }

    private static PdfPCell noBorderCell(Paragraph p) {
        PdfPCell c = new PdfPCell();
        c.setBorder(0);
        c.setPadding(0);
        c.addElement(p);
        return c;
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
    private static void headerCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 8.5f, WHITE)));
        cell.setBackgroundColor(DARK_GREEN);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(mm(2.2f));
        cell.setBorderColor(DARK_GREEN);
        table.addCell(cell);
    }

    private static void headerCellTint(PdfPTable table, String text, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 8.5f, WHITE)));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(mm(2.2f));
        cell.setBorderColor(bg);
        table.addCell(cell);
    }

    private static void bodyCell(PdfPTable table, String text, int alignment, Color bg, Color color, boolean bold) {
        PdfPCell cell = new PdfPCell(new Phrase(text, bold
                ? font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 9f, color)
                : font(SANS, FontFactory.HELVETICA, 9f, color)));
        cell.setHorizontalAlignment(alignment);
        cell.setBackgroundColor(bg);
        cell.setPadding(mm(1.5f));
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColorBottom(BORDER_LIGHTER);
        cell.setBorderWidthBottom(0.4f);
        table.addCell(cell);
    }

    private static void bodyCellMono(PdfPTable table, String text, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(MONO, FontFactory.COURIER, 9f, DARK_TEXT)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(bg);
        cell.setPadding(mm(1.5f));
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColorBottom(BORDER_LIGHTER);
        cell.setBorderWidthBottom(0.4f);
        table.addCell(cell);
    }

    private static void bodyCellMonoTint(PdfPTable table, String text, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(MONO_BOLD, FontFactory.COURIER_BOLD, 9f, DARK_TEXT)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(bg);
        cell.setPadding(mm(1.5f));
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColorBottom(BORDER_LIGHTER);
        cell.setBorderWidthBottom(0.4f);
        table.addCell(cell);
    }

    private static void totalCell(PdfPTable table, String text, int alignment, int colspan, Color bg, Color color) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 9f, color)));
        cell.setColspan(colspan);
        cell.setHorizontalAlignment(alignment);
        cell.setBackgroundColor(bg);
        cell.setPadding(mm(2.2f));
        cell.setBorderColor(bg);
        table.addCell(cell);
    }

    private static void totalCellMono(PdfPTable table, String text, Color bg, Color color) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(MONO_BOLD, FontFactory.COURIER_BOLD, 9f, color)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(bg);
        cell.setPadding(mm(2.2f));
        cell.setBorderColor(bg);
        table.addCell(cell);
    }

    // ── Polices embarquées (Cormorant Garamond/Sora/JetBrains Mono, §DesignFonts) — repli
    // automatique sur Times/Helvetica/Courier si le chargement a échoué au démarrage. ─────
    private static final com.lowagie.text.pdf.BaseFont SANS = gn.lacaeducation.common.pdf.DesignFonts.SANS;
    private static final com.lowagie.text.pdf.BaseFont SANS_BOLD = gn.lacaeducation.common.pdf.DesignFonts.SANS_SEMIBOLD;
    private static final com.lowagie.text.pdf.BaseFont SANS_EXTRABOLD = gn.lacaeducation.common.pdf.DesignFonts.SANS_EXTRABOLD;
    private static final com.lowagie.text.pdf.BaseFont SERIF_BOLD = gn.lacaeducation.common.pdf.DesignFonts.SERIF_BOLD;
    private static final com.lowagie.text.pdf.BaseFont SERIF_ITALIC = gn.lacaeducation.common.pdf.DesignFonts.SERIF_SEMIBOLD_ITALIC;
    private static final com.lowagie.text.pdf.BaseFont MONO = gn.lacaeducation.common.pdf.DesignFonts.MONO;
    private static final com.lowagie.text.pdf.BaseFont MONO_BOLD = gn.lacaeducation.common.pdf.DesignFonts.MONO_SEMIBOLD;

    private static com.lowagie.text.Font font(com.lowagie.text.pdf.BaseFont base, String fallbackFamily, float size, Color color) {
        if (base != null) {
            com.lowagie.text.Font f = new com.lowagie.text.Font(base, size);
            f.setColor(color);
            return f;
        }
        return FontFactory.getFont(fallbackFamily, size, color);
    }

    /** mm → points (72pt/pouce, 25.4mm/pouce) — toutes les dimensions de la référence sont en mm. */
    private static float mm(float v) {
        return v * 72f / 25.4f;
    }

    private static String dec(BigDecimal value) {
        return value == null ? "—" : value.toPlainString();
    }

    /** Moyenne simple de deux valeurs non nulles (même règle que le bulletin de fin d'année dédié,
     * AnnualReportCardService) ; si une seule est renseignée, elle est utilisée telle quelle. */
    private static BigDecimal average(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) return null;
        if (a == null) return b;
        if (b == null) return a;
        return a.add(b).divide(BigDecimal.valueOf(2), SCALE, RoundingMode.HALF_UP);
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

    private static String prefix(String prefix, String value) {
        return value == null || value.isBlank() ? "" : prefix + value;
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
