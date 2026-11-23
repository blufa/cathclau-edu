package gn.lacaeducation.grading.adapters.out.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
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
import gn.lacaeducation.grading.application.port.out.ClassCentralizerPdfGenerator.SubjectColumn;
import gn.lacaeducation.grading.application.port.out.MonthlyClassCentralizerPdfGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Centralisateur de classe pour un mois — même gabarit visuel que le centralisateur
 * semestriel/annuel ({@link OpenPdfClassCentralizerGenerator}), simplifié : une seule colonne de
 * moyenne par matière (pas de split cours/comp/1er-2ème semestre), un total des coefficients en bas
 * de tableau plutôt qu'une ligne "coef" par élève.
 */
@Component
@RequiredArgsConstructor
class OpenPdfMonthlyClassCentralizerGenerator implements MonthlyClassCentralizerPdfGenerator {

    private static final Color DARK = new Color(30, 41, 59);
    private static final Color GREY = new Color(110, 110, 110);
    private static final Color GOLD_BG = new Color(217, 168, 65);
    private static final Color GOLD_TINT = new Color(250, 240, 220);
    private static final Color HEADER_BG = new Color(30, 41, 59);
    private static final Color SUBHEADER_BG = new Color(71, 85, 105);
    private static final Color ZEBRA = new Color(247, 248, 250);
    private static final Color BORDER = new Color(225, 228, 232);
    private static final Color TOTAL_BG = new Color(240, 240, 240);

    private final TenantInfoProvider tenantInfoProvider;

    @Override
    public byte[] generate(MonthlyCentralizer data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 24, 24, 28, 44);
        try {
            TenantPublicInfo info = data.tenantId() == null ? null
                    : tenantInfoProvider.findTenantInfo(data.tenantId()).orElse(null);
            Color brand = SchoolPdfHeader.parseColor(info == null ? null : info.brandColor());

            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new SchoolPdfPageFooter(info, brand));
            doc.open();

            header(doc, info, data, brand);
            table(doc, data);
            footer(doc, info);

            doc.close();
            return out.toByteArray();
        } catch (RuntimeException e) {
            if (doc.isOpen()) doc.close();
            throw e;
        }
    }

    private static void header(Document doc, TenantPublicInfo info, MonthlyCentralizer data, Color brand) {
        PdfPTable top = new PdfPTable(new float[]{1.6f, 1f});
        top.setWidthPercentage(100);
        top.setSpacingAfter(3f);

        PdfPCell left = new PdfPCell();
        left.setBorder(0);
        PdfPTable inner = new PdfPTable(new float[]{1f, 3.4f});
        inner.setWidthPercentage(100);
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(0);
        logoCell.setVerticalAlignment(Element.ALIGN_TOP);
        Image logo = info != null ? SchoolPdfHeader.loadLogo(info.logo()) : null;
        if (logo != null) {
            logo.scaleToFit(40, 40);
            logoCell.addElement(logo);
        }
        inner.addCell(logoCell);
        PdfPCell nameCell = new PdfPCell();
        nameCell.setBorder(0);
        nameCell.addElement(new Paragraph(safe(info != null ? info.schoolName() : null, "École"),
                font(FontFactory.HELVETICA_BOLD, 12.5f, brand)));
        String contact = join(" · ", info != null ? info.address() : null, "Guinée", joinPhones(info));
        if (!contact.isBlank()) nameCell.addElement(new Paragraph(contact, font(FontFactory.HELVETICA, 8f, GREY)));
        if (data.schoolYear() != null && !data.schoolYear().isBlank()) {
            nameCell.addElement(new Paragraph("Année scolaire " + data.schoolYear(), font(FontFactory.HELVETICA, 8f, GREY)));
        }
        inner.addCell(nameCell);
        left.addElement(inner);
        top.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(0);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph republic = new Paragraph("RÉPUBLIQUE DE GUINÉE", font(FontFactory.HELVETICA_BOLD, 9, DARK));
        republic.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(republic);
        Paragraph motto = new Paragraph("Travail — Justice — Solidarité", font(FontFactory.HELVETICA_OBLIQUE, 8, GREY));
        motto.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(motto);
        String admin = join(" · ", "MEPU-A",
                prefix("IRE : ", info != null ? info.ire() : null),
                prefix("DPE/DCE : ", info != null ? info.dpeLabel() : null),
                prefix("DSSEE : ", info != null ? info.sousPrefectureLabel() : null));
        Paragraph adminP = new Paragraph(admin, font(FontFactory.HELVETICA, 7.5f, GREY));
        adminP.setAlignment(Element.ALIGN_RIGHT);
        adminP.setSpacingBefore(2f);
        right.addElement(adminP);
        top.addCell(right);
        doc.add(top);

        SchoolPdfHeader.renderSeparator(doc, brand);

        String cycle = data.levelLabel() != null && !data.levelLabel().isBlank() ? " " + data.levelLabel().toUpperCase(Locale.FRENCH) + " " : " ";
        String title = "FICHE DE CENTRALISATION DES MOYENNES —" + cycle
                + safe(data.className()).toUpperCase(Locale.FRENCH)
                + (data.monthLabel() != null ? " — " + data.monthLabel().toUpperCase(Locale.FRENCH) : "");
        Paragraph titleP = new Paragraph(title, font(FontFactory.HELVETICA_BOLD, 12, DARK));
        titleP.setSpacingBefore(4f);
        titleP.setSpacingAfter(5f);
        doc.add(titleP);
    }

    private static void table(Document doc, MonthlyCentralizer data) {
        List<SubjectColumn> subjects = data.subjects() != null ? data.subjects() : List.of();
        int fixedLeft = 3; // N°, Matricule, Prénoms et Nom
        int fixedRight = 2; // Moyenne générale, RANG
        int nCols = fixedLeft + subjects.size() + fixedRight;
        float[] widths = new float[nCols];
        widths[0] = 0.4f; widths[1] = 0.9f; widths[2] = 2.1f;
        for (int i = 0; i < subjects.size(); i++) widths[fixedLeft + i] = 0.9f;
        widths[nCols - 2] = 1f; widths[nCols - 1] = 0.6f;

        PdfPTable t = new PdfPTable(widths);
        t.setWidthPercentage(100);
        t.setHeaderRows(2);

        groupHeader(t, "N°", 1, 2);
        groupHeader(t, "Matricule", 1, 2);
        groupHeader(t, "Prénoms et Nom", 1, 2);
        groupHeader(t, "Moyenne par matière*", subjects.size(), 1);
        groupHeader(t, "Moyenne générale**", 1, 2);
        groupHeader(t, "RANG", 1, 2);
        for (SubjectColumn s : subjects) {
            String coefLabel = s.coefficient() == null ? "" : " (coef " + dec(s.coefficient()) + ")";
            subHeader(t, safe(s.subjectName()) + coefLabel);
        }

        if (data.rows() == null || data.rows().isEmpty()) {
            PdfPCell empty = new PdfPCell(new Phrase("Aucun bulletin mensuel généré pour cette classe et ce mois.",
                    font(FontFactory.HELVETICA_OBLIQUE, 9.5f, GREY)));
            empty.setColspan(nCols);
            empty.setHorizontalAlignment(Element.ALIGN_CENTER);
            empty.setPadding(12f);
            t.addCell(empty);
        }

        int n = 1;
        boolean zebra = false;
        for (MonthlyStudentBlock b : data.rows()) {
            Color bg = zebra ? ZEBRA : Color.WHITE;
            zebra = !zebra;

            identityCell(t, String.valueOf(n++), bg, Element.ALIGN_CENTER);
            identityCell(t, safe(b.studentNumber()), bg, Element.ALIGN_CENTER);
            identityCell(t, safe(b.studentName()), bg, Element.ALIGN_LEFT);

            for (SubjectColumn s : subjects) {
                BigDecimal v = b.averageBySubject() == null ? null : b.averageBySubject().get(s.subjectId());
                bodyCell(t, dec(v), bg, false);
            }

            tintedCell(t, dec(b.overallAverage()), Element.ALIGN_CENTER);
            tintedCell(t, b.rank() == null ? "—" : rankLabel(b.rank()), Element.ALIGN_CENTER);
        }

        // Ligne de pied : total des coefficients (§retour censeur, "les matieres et coefficient en
        // bas sur une ligne total des coefficients") — valeur unique par matière, jamais recalculée
        // par élève (le coefficient d'une matière ne varie pas d'un élève à l'autre).
        PdfPCell totalLabel = new PdfPCell(new Phrase("Total des coefficients", font(FontFactory.HELVETICA_BOLD, 7.5f, DARK)));
        totalLabel.setColspan(fixedLeft);
        totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalLabel.setBackgroundColor(TOTAL_BG);
        totalLabel.setPadding(3.2f);
        totalLabel.setBorderColor(BORDER);
        t.addCell(totalLabel);
        BigDecimal totalCoef = BigDecimal.ZERO;
        for (SubjectColumn s : subjects) {
            if (s.coefficient() != null) totalCoef = totalCoef.add(s.coefficient());
            totalCell(t, dec(s.coefficient()), TOTAL_BG, true);
        }
        PdfPCell blankEnd = new PdfPCell(new Phrase(dec(totalCoef), font(FontFactory.HELVETICA_BOLD, 7.5f, DARK)));
        blankEnd.setColspan(fixedRight);
        blankEnd.setHorizontalAlignment(Element.ALIGN_CENTER);
        blankEnd.setBackgroundColor(TOTAL_BG);
        blankEnd.setPadding(3.2f);
        blankEnd.setBorderColor(BORDER);
        t.addCell(blankEnd);

        t.setSpacingAfter(mm(3));
        doc.add(t);
    }

    private static void footer(Document doc, TenantPublicInfo info) {
        String date = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH).format(java.time.LocalDate.now());
        String city = info != null ? info.city() : null;
        String note = "* Moyenne mensuelle par matière · ** Moyenne générale · "
                + (city != null && !city.isBlank() ? "Fait à " + city + ", " : "Fait ") + "le " + date;

        PdfPTable t = new PdfPTable(new float[]{2f, 1f});
        t.setWidthPercentage(100);
        PdfPCell left = new PdfPCell(new Phrase(note, font(FontFactory.HELVETICA_OBLIQUE, 7.5f, GREY)));
        left.setBorder(0);
        left.setVerticalAlignment(Element.ALIGN_BOTTOM);
        t.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(0);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph role = new Paragraph("Le Principal", font(FontFactory.HELVETICA_BOLD, 9.5f, DARK));
        role.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(role);
        if (info != null && info.director() != null && !info.director().isBlank()) {
            Paragraph director = new Paragraph(info.director(), font(FontFactory.HELVETICA_BOLD, 10, DARK));
            director.setAlignment(Element.ALIGN_RIGHT);
            director.setSpacingBefore(14f);
            right.addElement(director);
        }
        t.addCell(right);
        doc.add(t);
    }

    private static String rankLabel(int rank) {
        return rank == 1 ? "1er" : rank + "e";
    }

    private static void groupHeader(PdfPTable table, String text, int colspan, int rowspan) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(FontFactory.HELVETICA_BOLD, 7.5f, Color.WHITE)));
        cell.setColspan(colspan);
        cell.setRowspan(rowspan);
        cell.setBackgroundColor(HEADER_BG);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4f);
        cell.setBorderColor(HEADER_BG);
        table.addCell(cell);
    }

    private static void subHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(FontFactory.HELVETICA_BOLD, 6.8f, Color.WHITE)));
        cell.setBackgroundColor(SUBHEADER_BG);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(3.2f);
        cell.setBorderColor(SUBHEADER_BG);
        table.addCell(cell);
    }

    private static void identityCell(PdfPTable table, String text, Color bg, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(FontFactory.HELVETICA_BOLD, 7.5f, DARK)));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(bg);
        cell.setPadding(3f);
        cell.setBorderColor(BORDER);
        table.addCell(cell);
    }

    private static void bodyCell(PdfPTable table, String text, Color bg, boolean bold) {
        PdfPCell cell = new PdfPCell(new Phrase(text, bold
                ? font(FontFactory.HELVETICA_BOLD, 7f, DARK)
                : font(FontFactory.HELVETICA, 7f, DARK)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(bg);
        cell.setPadding(2.8f);
        cell.setBorderColor(BORDER);
        table.addCell(cell);
    }

    private static void totalCell(PdfPTable table, String text, Color bg, boolean bold) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(bold ? FontFactory.HELVETICA_BOLD : FontFactory.HELVETICA, 7f, DARK)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(bg);
        cell.setPadding(2.8f);
        cell.setBorderColor(BORDER);
        table.addCell(cell);
    }

    private static void tintedCell(PdfPTable table, String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(FontFactory.HELVETICA_BOLD, 8.5f, DARK)));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(GOLD_TINT);
        cell.setPadding(3f);
        cell.setBorderColor(GOLD_BG);
        table.addCell(cell);
    }

    private static Font font(String family, float size, Color color) {
        return FontFactory.getFont(family, size, color);
    }

    private static float mm(float v) { return v * 72f / 25.4f; }

    private static String safe(String s) { return s == null ? "" : s; }

    private static String safe(String s, String fallback) { return s == null || s.isBlank() ? fallback : s; }

    private static String dec(BigDecimal v) { return v == null ? "—" : v.stripTrailingZeros().toPlainString(); }

    private static String prefix(String p, String v) { return v == null || v.isBlank() ? null : p + v; }

    private static String joinPhones(TenantPublicInfo info) {
        if (info == null) return null;
        return prefix("Tél : ", join(" / ", info.phone(), info.phone2()));
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
}
