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
import java.util.ArrayList;
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
    // Une couleur par matière (cycle), §retour utilisateur : "mettre différentes couleurs sur ces
    // diagrammes" — le rouge (BAR_FAIL_COLOR) reste réservé aux moyennes sous la moyenne de passage
    // (10/20), jamais réutilisé ici, pour ne pas perdre ce signal.
    private static final Color[] BAR_PALETTE = {
            DARK_GREEN, GOLD, new Color(30, 86, 160), new Color(106, 76, 156),
            new Color(23, 128, 135), new Color(180, 95, 6), new Color(60, 90, 150), new Color(120, 110, 40),
    };
    private static final float MIN_CHART_HEIGHT = 55f;
    private static final float MAX_CHART_HEIGHT = 120f;
    private static final float CHART_LABEL_SPACE = 30f;
    // Hauteur estimée du bloc appréciation+signature (voir appreciationAndSignature) — réservée AVANT
    // de calculer la hauteur du diagramme, pour que les deux tiennent ensemble sur la page.
    private static final float FOOTER_BLOCK_RESERVE = mm(30f);

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

            // Hauteur du diagramme calculée sur l'espace RESTANT jusqu'au bas de page, moins une
            // réserve pour le bloc appréciation+signature ci-dessous — pour que le document tienne
            // sur une seule page quel que soit le nombre de matières (§retour utilisateur), plutôt
            // qu'une hauteur fixe qui déborde dès qu'il y a beaucoup de matières dans la classe. Si
            // même la hauteur minimale lisible ne tient plus (classe avec un très grand nombre de
            // matières), on omet le diagramme plutôt que de forcer un débordement sur une 2e page —
            // la contrainte "une seule page" prime sur le diagramme, qui reste un bonus visuel.
            if (!d.lines().isEmpty()) {
                float available = writer.getVerticalPosition(true) - doc.bottom() - FOOTER_BLOCK_RESERVE - CHART_LABEL_SPACE;
                if (available >= MIN_CHART_HEIGHT) {
                    barChart(doc, writer, d.lines(), Math.min(MAX_CHART_HEIGHT, available));
                }
            }

            appreciationAndSignatureBlock(doc, d, info);

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
        c.setPadding(mm(1.6f));
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

    /** N° | Matière | Coef | Moyenne | Enseignant + ligne TOTAL/MOYENNE OBTENUE SUR 20 — même habillage
     * que le tableau du bulletin officiel (§retour utilisateur : "il faut rendre le tableau comme le
     * bulletin normal"), voir les cellules partagées dans {@link BulletinPremiumChrome}. La colonne
     * Enseignant n'existe pas sur le bulletin officiel : sa cellule reste vide (même fond) sur la
     * ligne TOTAL pour ne pas casser l'alignement des colonnes. */
    private static void subjectTable(Document doc, MonthlyReportCardDocument d) {
        sectionLabel(doc, "NOTES DU MOIS");

        PdfPTable table = new PdfPTable(new float[]{0.4f, 2.2f, 0.6f, 1f, 1.8f});
        table.setWidthPercentage(100);
        // Padding plus compact (1.6mm/1.1mm) que le bulletin officiel (2.2mm/1.5mm, inchangé) : ce
        // document ajoute un diagramme en bâtonnets en plus, il faut regagner de la place pour tenir
        // sur une seule page quel que soit le nombre de matières (§retour utilisateur).
        headerCell(table, "N°", mm(1.6f));
        headerCell(table, "Matière", mm(1.6f));
        headerCell(table, "Coef", mm(1.6f));
        headerCellTint(table, "Moyenne", GOLD, mm(1.6f));
        headerCell(table, "Enseignant", mm(1.6f));

        boolean zebra = false;
        int n = 1;
        for (Line l : d.lines()) {
            Color bg = zebra ? ROW_STRIPE : WHITE;
            zebra = !zebra;
            BigDecimal coef = l.coefficient() != null ? l.coefficient() : BigDecimal.ONE;
            bodyCell(table, String.valueOf(n++), Element.ALIGN_CENTER, bg, LABEL_GREY, true, mm(1.1f));
            bodyCell(table, l.subjectName(), Element.ALIGN_LEFT, bg, DARK_TEXT, true, mm(1.1f));
            bodyCell(table, dec(coef), Element.ALIGN_CENTER, bg, TEXT_GREY, false, mm(1.1f));
            bodyCellMonoTint(table, dec(normalized(l.average(), coef)), RESULT_TINT, mm(1.1f));
            bodyCell(table, safe(l.teacherName(), "—"), Element.ALIGN_LEFT, bg, DARK_TEXT, false, mm(1.1f));
        }

        totalCell(table, "MOYENNE OBTENUE SUR 20", Element.ALIGN_LEFT, 3, DARK_GREEN, WHITE, mm(1.6f));
        totalCellMono(table, dec(d.overallAverage()), GOLD, WHITE, mm(1.6f));
        totalCell(table, "", Element.ALIGN_LEFT, 1, DARK_GREEN, WHITE, mm(1.6f));

        roundedTable(doc, table, mm(3.8f));
    }

    /** BILAN : moyenne générale du mois + RANG, même carte que le bulletin officiel — pas de ligne
     * retards/absences/décision ici, ce n'est pas un document de passage en classe supérieure. */
    private static void bilan(Document doc, MonthlyReportCardDocument d) {
        sectionLabel(doc, "BILAN DU MOIS");
        PdfPTable row = cardRow(2);
        // Pas de "mention" courte ici (contrairement au bulletin officiel) : l'appréciation complète
        // (auto + censeur) est déjà affichée dans sa propre section ci-dessous, voir appreciationAndSignature().
        addCard(row, "Moyenne du mois", dec(d.overallAverage()), "/ 20", "", true, true);
        addRankCard(row, d.rank(), d.classSize(), false);
        doc.add(row);
    }

    /** Diagramme en bâtonnets des moyennes par matière (§censeur) — un coup d'œil pour le parent,
     * en plus du tableau détaillé ci-dessus. Barre rouge sous la moyenne de passage (10/20).
     * {@code chartHeight} est calculée dynamiquement par l'appelant (espace restant sur la page,
     * voir {@code generate()}) pour garantir un document tenant sur une seule page. */
    private static void barChart(Document doc, PdfWriter writer, List<Line> lines, float chartHeight) {
        float left = doc.left();
        float right = doc.right();
        float top = writer.getVerticalPosition(true) - 18f;

        // Réserve l'espace dans le flux du document (sinon le texte suivant chevaucherait le dessin,
        // qui se fait en coordonnées absolues via PdfContentByte, hors du flux normal).
        PdfPTable spacer = new PdfPTable(1);
        spacer.setWidthPercentage(100);
        PdfPCell spacerCell = new PdfPCell();
        spacerCell.setBorder(0);
        spacerCell.setFixedHeight(chartHeight + CHART_LABEL_SPACE);
        spacer.addCell(spacerCell);
        doc.add(spacer);

        float baseline = top - chartHeight;
        PdfContentByte cb = writer.getDirectContent();

        cb.setColorStroke(AXIS_COLOR);
        cb.setLineWidth(0.6f);
        cb.moveTo(left, baseline);
        cb.lineTo(right, baseline);
        cb.stroke();

        int n = lines.size();
        float slot = (right - left) / n;
        // Barres plus fines (§retour utilisateur : "diminuer l'épaisseur") — 0.5/34pt max devenait
        // large dès que peu de matières, resserré à 0.3/20pt.
        float barWidth = Math.min(slot * 0.3f, 20f);

        for (int i = 0; i < n; i++) {
            Line l = lines.get(i);
            // IMPORTANT : l.average() est la moyenne DÉJÀ multipliée par le coefficient de la matière
            // (même convention que le bulletin officiel, voir SubjectSemesterAverage — "gonflée", pas
            // une note sur 20). Le diagramme et le seuil de réussite (10/20) ont besoin de la note
            // brute : on divise par le coefficient avant de calculer la hauteur de la barre — bug réel
            // rencontré en test (coefficient 4 → barre de 390pt de haut, débordait toute la page).
            BigDecimal avg = normalized(l.average(), l.coefficient());
            float x = left + i * slot + (slot - barWidth) / 2f;
            float h = avg == null ? 0 : avg.floatValue() / 20f * chartHeight;
            boolean passing = avg != null && avg.compareTo(BigDecimal.TEN) >= 0;

            cb.setColorFill(passing ? BAR_PALETTE[i % BAR_PALETTE.length] : BAR_FAIL_COLOR);
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

    /** Appréciation automatique (barème existant) + commentaire manuel du censeur, côte à côte avec
     * la signature — même disposition que le bulletin officiel (§retour utilisateur : "le même
     * bulletin que le bulletin normal"), voir {@link BulletinPremiumChrome#appreciationAndSignature}.
     * Toujours affiché (même sans appréciation/commentaire) pour garder le bloc signature visible. */
    private static void appreciationAndSignatureBlock(Document doc, MonthlyReportCardDocument d, TenantPublicInfo info) {
        List<Paragraph> lines = new ArrayList<>();
        if (d.autoAppreciation() != null && !d.autoAppreciation().isBlank()) {
            lines.add(new Paragraph(d.autoAppreciation(),
                    font(gn.lacaeducation.common.pdf.DesignFonts.SERIF_SEMIBOLD_ITALIC, FontFactory.TIMES_BOLDITALIC, 12, DARK_TEXT)));
        }
        if (d.comment() != null && !d.comment().isBlank()) {
            lines.add(new Paragraph(d.comment(), font(null, FontFactory.HELVETICA_OBLIQUE, 9.5f, TEXT_GREY)));
        }
        if (lines.isEmpty()) {
            lines.add(new Paragraph("—", font(null, FontFactory.HELVETICA_OBLIQUE, 9.5f, TEXT_GREY)));
        }
        BulletinPremiumChrome.appreciationAndSignature(doc, "Appréciation du censeur", lines, "Le Censeur", info);
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
