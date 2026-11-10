package gn.lacaeducation.grading.adapters.out.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.common.TenantPublicInfo;
import gn.lacaeducation.common.pdf.PremiumPageBackground;
import gn.lacaeducation.common.pdf.RoundedCellBorder;
import gn.lacaeducation.common.pdf.SchoolPdfPageFooter;
import gn.lacaeducation.grading.application.port.out.MonthlyReportCardPdfGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import static gn.lacaeducation.grading.adapters.out.pdf.BulletinPremiumChrome.*;

/**
 * Bulletin de contrôle mensuel (§censeur) : diagnostic de niveau en cours d'année, séparé du
 * bulletin officiel (jamais la même table, jamais compté dans la moyenne annuelle) mais avec le
 * MÊME habillage visuel ("ça doit être le même bulletin que le bulletin normal" — fond crème,
 * liseré tricolore, en-tête ministère/logo/école, titre encadré de filets or, cartes BILAN moyenne
 * + RANG) que {@code OpenPdfReportCardGenerator}, voir {@link BulletinPremiumChrome}. Pas de
 * mention/décision/distinction : ce document n'a aucune valeur de passage en classe supérieure.
 * Diagramme en bâtonnets des moyennes par matière + double appréciation (automatique, calculée
 * depuis le barème existant, et manuelle du censeur) — "pour que son parent puisse savoir si il
 * doit faire des révisions".
 */
@Component
@RequiredArgsConstructor
class OpenPdfMonthlyReportCardGenerator implements MonthlyReportCardPdfGenerator {

    private static final Color BAR_FAIL_COLOR = new Color(179, 38, 30);
    private static final Color AXIS_COLOR = new Color(180, 180, 180);
    private static final float CHART_HEIGHT = 130f;

    private final TenantInfoProvider tenantInfoProvider;

    @Override
    public byte[] generate(MonthlyReportCardDocument d) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, mm(15), mm(12), mm(10), mm(16));
        try {
            TenantPublicInfo info = d.tenantId() == null ? null : tenantInfoProvider.findTenantInfo(d.tenantId()).orElse(null);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new PremiumPageBackground());
            writer.setPageEvent(new SchoolPdfPageFooter(info, FOOTER_BORDER));
            doc.open();

            header(doc, info, null);
            titleBar(doc, "Bulletin de contrôle mensuel — " + safe(d.monthLabel(), "—"));

            identityRow(doc, d);
            subjectTable(doc, d);
            bilan(doc, d);

            if (!d.lines().isEmpty()) {
                barChart(doc, writer, d.lines());
            }

            appreciations(doc, d);
            signature(doc);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            if (doc.isOpen()) doc.close();
            throw new RuntimeException("Échec de génération du PDF du bulletin mensuel", e);
        }
    }

    private static void identityRow(Document doc, MonthlyReportCardDocument d) {
        PdfPTable grid = new PdfPTable(new float[]{1.4f, 1f});
        grid.setWidthPercentage(100);
        identityCell(grid, "Prénom(s) et Nom", safe(d.studentName(), "—"), true, true);
        identityCell(grid, "Matricule", safe(d.studentNumber(), "—"), false, true);
        identityCell(grid, "Classe", safe(d.className(), "—"), true, false);
        identityCell(grid, "Mois", safe(d.monthLabel(), "—"), false, false);

        PdfPCell outer = new PdfPCell();
        outer.setBorder(0);
        outer.setPadding(0);
        outer.setCellEvent(RoundedCellBorder.fillAndBorder(mm(2.5f), WHITE, BORDER, 0.8f));
        outer.addElement(grid);

        PdfPTable row = new PdfPTable(1);
        row.setWidthPercentage(100);
        row.setSpacingAfter(mm(3.8f));
        row.addCell(outer);
        doc.add(row);
    }

    private static void identityCell(PdfPTable grid, String label, String value, boolean borderRight, boolean borderBottom) {
        PdfPCell c = new PdfPCell();
        c.setPadding(mm(2.2f));
        c.setPaddingLeft(mm(3));
        c.setBorder(0);
        if (borderRight) { c.setBorder(c.getBorder() | Rectangle.RIGHT); c.setBorderColorRight(BORDER); c.setBorderWidthRight(0.6f); }
        if (borderBottom) { c.setBorder(c.getBorder() | Rectangle.BOTTOM); c.setBorderColorBottom(BORDER); c.setBorderWidthBottom(0.6f); }
        Paragraph l = new Paragraph(label.toUpperCase(Locale.FRENCH), font(null, FontFactory.HELVETICA_BOLD, 6.5f, LABEL_GREY));
        c.addElement(l);
        Paragraph v = new Paragraph(value, font(null, FontFactory.HELVETICA_BOLD, 11, DARK_TEXT));
        v.setSpacingBefore(1.5f);
        c.addElement(v);
        grid.addCell(c);
    }

    private static void subjectTable(Document doc, MonthlyReportCardDocument d) {
        sectionLabel(doc, "NOTES DU MOIS");

        PdfPTable table = new PdfPTable(new float[]{2.2f, 1f, 0.7f, 1.8f});
        table.setWidthPercentage(100);
        headerCell(table, "Matière");
        headerCell(table, "Moyenne");
        headerCell(table, "Coef");
        headerCell(table, "Enseignant");

        boolean zebra = false;
        for (Line l : d.lines()) {
            Color bg = zebra ? new Color(252, 250, 244) : WHITE;
            zebra = !zebra;
            bodyCell(table, l.subjectName(), Element.ALIGN_LEFT, bg);
            bodyCell(table, dec(normalized(l.average(), l.coefficient())), Element.ALIGN_CENTER, bg);
            bodyCell(table, dec(l.coefficient()), Element.ALIGN_CENTER, bg);
            bodyCell(table, safe(l.teacherName(), "—"), Element.ALIGN_LEFT, bg);
        }

        PdfPCell outer = new PdfPCell();
        outer.setBorder(0);
        outer.setPadding(0);
        outer.setCellEvent(RoundedCellBorder.fillAndBorder(mm(2.5f), WHITE, BORDER, 0.8f));
        outer.addElement(table);
        PdfPTable wrapper = new PdfPTable(1);
        wrapper.setWidthPercentage(100);
        wrapper.setSpacingAfter(mm(3.8f));
        wrapper.addCell(outer);
        doc.add(wrapper);
    }

    private static void headerCell(PdfPTable table, String text) {
        PdfPCell c = new PdfPCell(new Phrase(text.toUpperCase(Locale.FRENCH), font(null, FontFactory.HELVETICA_BOLD, 6.5f, LABEL_GREY)));
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColorBottom(BORDER);
        c.setBorderWidthBottom(0.8f);
        c.setPadding(mm(1.8f));
        c.setBackgroundColor(new Color(241, 236, 223));
        table.addCell(c);
    }

    private static void bodyCell(PdfPTable table, String text, int align, Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(text, font(null, FontFactory.HELVETICA, 9f, DARK_TEXT)));
        c.setBorder(0);
        c.setHorizontalAlignment(align);
        c.setPadding(mm(1.8f));
        c.setBackgroundColor(bg);
        table.addCell(c);
    }

    /** BILAN : moyenne générale du mois + RANG, même carte que le bulletin officiel — pas de ligne
     * retards/absences/décision ici, ce n'est pas un document de passage en classe supérieure. */
    private static void bilan(Document doc, MonthlyReportCardDocument d) {
        sectionLabel(doc, "BILAN DU MOIS");
        PdfPTable row = cardRow(2);
        // Pas de "mention" courte ici (contrairement au bulletin officiel) : l'appréciation complète
        // (auto + censeur) est déjà affichée dans sa propre section ci-dessous, voir appreciations().
        addCard(row, "Moyenne du mois", dec(d.overallAverage()), "/ 20", "", true, true);
        addRankCard(row, d.rank(), d.classSize(), false);
        doc.add(row);
    }

    /** Diagramme en bâtonnets des moyennes par matière (§censeur) — un coup d'œil pour le parent,
     * en plus du tableau détaillé ci-dessus. Barre rouge sous la moyenne de passage (10/20). */
    private static void barChart(Document doc, PdfWriter writer, List<Line> lines) {
        float left = doc.left();
        float right = doc.right();
        float top = writer.getVerticalPosition(true) - 18f;

        // Réserve l'espace dans le flux du document (sinon le texte suivant chevaucherait le dessin,
        // qui se fait en coordonnées absolues via PdfContentByte, hors du flux normal).
        PdfPTable spacer = new PdfPTable(1);
        spacer.setWidthPercentage(100);
        PdfPCell spacerCell = new PdfPCell();
        spacerCell.setBorder(0);
        spacerCell.setFixedHeight(CHART_HEIGHT + 30f);
        spacer.addCell(spacerCell);
        doc.add(spacer);

        float baseline = top - CHART_HEIGHT;
        PdfContentByte cb = writer.getDirectContent();

        cb.setColorStroke(AXIS_COLOR);
        cb.setLineWidth(0.6f);
        cb.moveTo(left, baseline);
        cb.lineTo(right, baseline);
        cb.stroke();

        int n = lines.size();
        float slot = (right - left) / n;
        float barWidth = Math.min(slot * 0.5f, 34f);

        for (int i = 0; i < n; i++) {
            Line l = lines.get(i);
            // IMPORTANT : l.average() est la moyenne DÉJÀ multipliée par le coefficient de la matière
            // (même convention que le bulletin officiel, voir SubjectSemesterAverage — "gonflée", pas
            // une note sur 20). Le diagramme et le seuil de réussite (10/20) ont besoin de la note
            // brute : on divise par le coefficient avant de calculer la hauteur de la barre — bug réel
            // rencontré en test (coefficient 4 → barre de 390pt de haut, débordait toute la page).
            BigDecimal avg = normalized(l.average(), l.coefficient());
            float x = left + i * slot + (slot - barWidth) / 2f;
            float h = avg == null ? 0 : avg.floatValue() / 20f * CHART_HEIGHT;
            boolean passing = avg != null && avg.compareTo(BigDecimal.TEN) >= 0;

            cb.setColorFill(passing ? DARK_GREEN : BAR_FAIL_COLOR);
            cb.rectangle(x, baseline, barWidth, h);
            cb.fill();

            cb.beginText();
            cb.setFontAndSize(gn.lacaeducation.common.pdf.DesignFonts.SANS_SEMIBOLD, 7f);
            cb.showTextAligned(Element.ALIGN_CENTER, dec(avg), x + barWidth / 2f, baseline + h + 3f, 0);
            cb.endText();

            String label = truncate(l.subjectName(), 10);
            cb.beginText();
            cb.setFontAndSize(gn.lacaeducation.common.pdf.DesignFonts.SANS, 6.5f);
            cb.showTextAligned(Element.ALIGN_CENTER, label, x + barWidth / 2f, baseline - 10f, 0);
            cb.endText();
        }
    }

    /** Appréciation automatique (barème existant) + appréciation manuelle du censeur, si renseignée —
     * jamais l'une sans l'autre quand les deux existent, pour que le parent ait toujours un avis
     * explicite sur la nécessité de réviser. */
    private static void appreciations(Document doc, MonthlyReportCardDocument d) {
        if (d.autoAppreciation() == null && (d.comment() == null || d.comment().isBlank())) return;
        PdfPCell box = new PdfPCell();
        box.setBorder(0);
        box.setPadding(mm(2.5f));
        box.setCellEvent(RoundedCellBorder.fillAndBorder(mm(2.5f), WHITE, BORDER, 0.8f));
        if (d.autoAppreciation() != null) {
            box.addElement(new Paragraph("Appréciation : " + d.autoAppreciation(), font(null, FontFactory.HELVETICA, 9.5f, DARK_TEXT)));
        }
        if (d.comment() != null && !d.comment().isBlank()) {
            Paragraph manual = new Paragraph("Commentaire du censeur : " + d.comment(), font(null, FontFactory.HELVETICA_OBLIQUE, 9.5f, TEXT_GREY));
            manual.setSpacingBefore(4f);
            box.addElement(manual);
        }
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(mm(2f));
        t.setSpacingAfter(mm(5f));
        t.addCell(box);
        doc.add(t);
    }

    private static void signature(Document doc) {
        PdfPCell box = new PdfPCell(new Phrase("Signature et cachet de l'établissement", font(null, FontFactory.HELVETICA_OBLIQUE, 8.5f, TEXT_GREY)));
        box.setBorder(0);
        box.setFixedHeight(mm(16));
        box.setHorizontalAlignment(Element.ALIGN_RIGHT);
        box.setVerticalAlignment(Element.ALIGN_BOTTOM);
        box.setCellEvent(RoundedCellBorder.fillAndBorder(mm(2.5f), WHITE, BORDER, 0.8f));
        PdfPTable t = new PdfPTable(new float[]{1f, 1f});
        t.setWidthPercentage(100);
        PdfPCell empty = new PdfPCell();
        empty.setBorder(0);
        t.addCell(empty);
        t.addCell(box);
        doc.add(t);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    /** Ramène une moyenne déjà "gonflée" par le coefficient (voir SubjectSemesterAverage) à une note
     * sur 20 lisible par un parent — utilisé uniquement pour l'affichage (tableau + diagramme), jamais
     * pour un calcul ultérieur. */
    private static BigDecimal normalized(BigDecimal avg, BigDecimal coefficient) {
        if (avg == null) return null;
        if (coefficient == null || coefficient.signum() <= 0) return avg;
        return avg.divide(coefficient, 2, java.math.RoundingMode.HALF_UP);
    }
}
