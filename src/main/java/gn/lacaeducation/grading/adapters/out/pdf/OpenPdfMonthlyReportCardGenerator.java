package gn.lacaeducation.grading.adapters.out.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
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
import gn.lacaeducation.common.pdf.AdminDocumentLayout;
import gn.lacaeducation.common.pdf.SchoolPdfPageFooter;
import gn.lacaeducation.grading.application.port.out.MonthlyReportCardPdfGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

import static gn.lacaeducation.common.pdf.AdminDocumentLayout.*;

/**
 * Bulletin de contrôle mensuel (§censeur) : diagnostic de niveau en cours d'année, mise en page
 * sobre et allégée — contrairement au bulletin officiel ({@code OpenPdfReportCardGenerator}), pas de
 * section mention/décision/distinction, ce document n'a aucune valeur de passage en classe
 * supérieure. Même en-tête que les autres documents administratifs (demande utilisateur récurrente :
 * "toujours reproduire les mêmes entêtes"). Diagramme en bâtonnets des moyennes par matière + double
 * appréciation (automatique, calculée depuis le barème existant, et manuelle du censeur) — "pour que
 * son parent puisse savoir si il doit faire des révisions".
 */
@Component
@RequiredArgsConstructor
class OpenPdfMonthlyReportCardGenerator implements MonthlyReportCardPdfGenerator {

    private static final Color BAR_COLOR = new Color(14, 77, 58);
    private static final Color BAR_FAIL_COLOR = new Color(179, 38, 30);
    private static final Color AXIS_COLOR = new Color(180, 180, 180);
    private static final float CHART_HEIGHT = 130f;

    private final TenantInfoProvider tenantInfoProvider;

    @Override
    public byte[] generate(MonthlyReportCardDocument d) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 36, 36, 28, 40);
        try {
            TenantPublicInfo info = d.tenantId() == null ? null : tenantInfoProvider.findTenantInfo(d.tenantId()).orElse(null);
            java.awt.Color brand = gn.lacaeducation.common.pdf.SchoolPdfHeader.parseColor(info == null ? null : info.brandColor());
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new SchoolPdfPageFooter(info, brand));
            doc.open();

            AdminDocumentLayout.header(doc, info);
            titleBox(doc, "BULLETIN DE CONTRÔLE MENSUEL — " + safe(d.monthLabel()).toUpperCase());

            PdfPTable info2 = infoTable();
            row(info2, "Élève", d.studentName());
            row(info2, "Matricule", d.studentNumber());
            row(info2, "Classe", d.className());
            row(info2, "Mois", d.monthLabel());
            doc.add(info2);

            PdfPTable table = dataTable(new float[]{2.2f, 1f, 1f, 1.8f});
            headerCell(table, "Matière");
            headerCell(table, "Moyenne");
            headerCell(table, "Coef.");
            headerCell(table, "Enseignant");
            for (Line l : d.lines()) {
                cell(table, l.subjectName(), Element.ALIGN_LEFT);
                cell(table, format(l.average()), Element.ALIGN_RIGHT);
                cell(table, format(l.coefficient()), Element.ALIGN_RIGHT);
                cell(table, safe(l.teacherName()), Element.ALIGN_LEFT);
            }
            doc.add(table);

            totalRow(doc, "Moyenne générale du mois", format(d.overallAverage()) + " / 20");

            if (!d.lines().isEmpty()) {
                barChart(doc, writer, d.lines());
            }

            appreciations(doc, d);
            signatureBox(doc, "Signature et cachet de l'établissement");

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Échec de génération du PDF du bulletin mensuel", e);
        }
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

        cb.setFontAndSize(gn.lacaeducation.common.pdf.DesignFonts.SANS, 6.5f);
        for (int i = 0; i < n; i++) {
            Line l = lines.get(i);
            BigDecimal avg = l.average();
            float x = left + i * slot + (slot - barWidth) / 2f;
            float h = avg == null ? 0 : avg.floatValue() / 20f * CHART_HEIGHT;
            boolean passing = avg != null && avg.compareTo(BigDecimal.TEN) >= 0;

            cb.setColorFill(passing ? BAR_COLOR : BAR_FAIL_COLOR);
            cb.rectangle(x, baseline, barWidth, h);
            cb.fill();

            cb.beginText();
            cb.setFontAndSize(gn.lacaeducation.common.pdf.DesignFonts.SANS_SEMIBOLD, 7f);
            cb.showTextAligned(Element.ALIGN_CENTER, format(avg), x + barWidth / 2f, baseline + h + 3f, 0);
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
    private static void appreciations(Document doc, MonthlyReportCardDocument d) throws Exception {
        if (d.autoAppreciation() == null && (d.comment() == null || d.comment().isBlank())) return;
        PdfPCell box = new PdfPCell();
        box.setBorder(Rectangle.BOX);
        box.setBorderColor(AdminDocumentLayout.LINE);
        box.setPadding(8f);
        if (d.autoAppreciation() != null) {
            Paragraph auto = new Paragraph("Appréciation : " + d.autoAppreciation(),
                    AdminDocumentLayout.font(FontFactory.HELVETICA, 9.5f, AdminDocumentLayout.DARK));
            box.addElement(auto);
        }
        if (d.comment() != null && !d.comment().isBlank()) {
            Paragraph manual = new Paragraph("Commentaire du censeur : " + d.comment(),
                    AdminDocumentLayout.font(FontFactory.HELVETICA_OBLIQUE, 9.5f, AdminDocumentLayout.GREY));
            manual.setSpacingBefore(4f);
            box.addElement(manual);
        }
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(10f);
        t.setSpacingAfter(14f);
        t.addCell(box);
        doc.add(t);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private static String format(BigDecimal v) {
        return v == null ? "-" : v.toPlainString();
    }
}
