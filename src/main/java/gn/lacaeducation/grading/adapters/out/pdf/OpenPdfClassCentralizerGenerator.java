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
import gn.lacaeducation.grading.application.port.out.ClassCentralizerPdfGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Centralisateur de classe — reproduit fidèlement la maquette officielle de référence (§retour
 * utilisateur, {@code features/design-pro-officiel-centralisateur.pdf}) : en-tête administratif
 * (ministère/République, comme les bulletins), "FICHE DE CENTRALISATION DES MOYENNES PAR MATIÈRE",
 * une matière par colonne (coefficient en première ligne), un bloc de 8 lignes par élève (coef / 1er
 * semestre [cours, comp, Moy 1] / 2ème semestre [cours, comp, Moy 2] / Annuelle), moyenne générale et
 * rang affichés une seule fois sur la ligne Annuelle. Paysage vu le nombre de colonnes.
 */
@Component
@RequiredArgsConstructor
class OpenPdfClassCentralizerGenerator implements ClassCentralizerPdfGenerator {

    private static final Color DARK = new Color(30, 41, 59);
    private static final Color GREY = new Color(110, 110, 110);
    private static final Color GOLD_BG = new Color(217, 168, 65);
    private static final Color GOLD_TINT = new Color(250, 240, 220);
    private static final Color HEADER_BG = new Color(30, 41, 59);
    private static final Color SUBHEADER_BG = new Color(71, 85, 105);
    private static final Color ZEBRA = new Color(247, 248, 250);
    private static final Color BORDER = new Color(225, 228, 232);

    private final TenantInfoProvider tenantInfoProvider;

    @Override
    public byte[] generate(Centralizer data) {
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

    private static void header(Document doc, TenantPublicInfo info, Centralizer data, Color brand) {
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

        String cycle = data.levelLabel() != null && !data.levelLabel().isBlank() ? " AU SECONDAIRE " : " ";
        String title = "FICHE DE CENTRALISATION DES MOYENNES PAR MATIÈRE" + cycle
                + safe(data.className()).toUpperCase(Locale.FRENCH);
        Paragraph titleP = new Paragraph(title, font(FontFactory.HELVETICA_BOLD, 12, DARK));
        titleP.setSpacingBefore(4f);
        titleP.setSpacingAfter(5f);
        doc.add(titleP);
    }

    private static void table(Document doc, Centralizer data) {
        List<SubjectColumn> subjects = data.subjects() != null ? data.subjects() : List.of();
        int fixedLeft = 5; // N°, Niveau, Prénoms et Nom, [semestre], [type]
        int fixedRight = 3; // TOTAL, Moyenne générale, RANG
        int nCols = fixedLeft + subjects.size() + fixedRight;
        float[] widths = new float[nCols];
        widths[0] = 0.4f; widths[1] = 0.75f; widths[2] = 2.1f; widths[3] = 0.75f; widths[4] = 0.75f;
        for (int i = 0; i < subjects.size(); i++) widths[fixedLeft + i] = 0.8f;
        widths[nCols - 3] = 0.75f; widths[nCols - 2] = 0.95f; widths[nCols - 1] = 0.55f;

        PdfPTable t = new PdfPTable(widths);
        t.setWidthPercentage(100);
        t.setHeaderRows(2);

        groupHeader(t, "N°", 1, 2);
        groupHeader(t, "Niveau", 1, 2);
        groupHeader(t, "Prénoms et Nom", 1, 2);
        groupHeader(t, "", 1, 2);
        groupHeader(t, "", 1, 2);
        groupHeader(t, "Moyenne des notes coefficiées par matière*", subjects.size() + 1, 1);
        groupHeader(t, "Moyenne générale**", 1, 2);
        groupHeader(t, "RANG", 1, 2);
        for (SubjectColumn s : subjects) subHeader(t, safe(s.subjectName()));
        subHeader(t, "TOTAL");

        if (data.rows() == null || data.rows().isEmpty()) {
            PdfPCell empty = new PdfPCell(new Phrase("Aucun bulletin semestriel généré pour cette classe et cette année scolaire.",
                    font(FontFactory.HELVETICA_OBLIQUE, 9.5f, GREY)));
            empty.setColspan(nCols);
            empty.setHorizontalAlignment(Element.ALIGN_CENTER);
            empty.setPadding(12f);
            t.addCell(empty);
        }

        int n = 1;
        boolean zebra = false;
        for (StudentBlock b : data.rows()) {
            Color bg = zebra ? ZEBRA : Color.WHITE;
            zebra = !zebra;

            identityCell(t, String.valueOf(n++), bg, Element.ALIGN_CENTER, 8);
            identityCell(t, safe(b.levelLabel()), bg, Element.ALIGN_CENTER, 8);
            identityCell(t, safe(b.studentName()), bg, Element.ALIGN_LEFT, 8);

            // coef
            semestreCell(t, "", bg, 1);
            typeCell(t, "coef", bg);
            BigDecimal coefTotal = subjectRow(t, subjects, s -> s.coefficient(), bg, false);
            totalCell(t, dec(coefTotal), bg, false);
            blankCell(t, bg); blankCell(t, bg);

            // 1er semestre
            semestreCell(t, "1er S.", bg, 3);
            typeCell(t, "cours", bg);
            BigDecimal s1CoursTotal = subjectRowFromMap(t, subjects, b.s1Cours(), bg, false);
            totalCell(t, dec(s1CoursTotal), bg, false);
            blankCell(t, bg); blankCell(t, bg);

            typeCell(t, "comp", bg);
            BigDecimal s1CompoTotal = subjectRowFromMap(t, subjects, b.s1Compo(), bg, false);
            totalCell(t, dec(s1CompoTotal), bg, false);
            blankCell(t, bg); blankCell(t, bg);

            typeCell(t, "Moy 1", bg);
            BigDecimal s1AvgTotal = subjectRowFromMap(t, subjects, b.s1Average(), bg, true);
            totalCell(t, dec(s1AvgTotal), bg, true);
            blankCell(t, bg); blankCell(t, bg);

            // 2ème semestre
            semestreCell(t, "2ème S.", bg, 3);
            typeCell(t, "cours", bg);
            BigDecimal s2CoursTotal = subjectRowFromMap(t, subjects, b.s2Cours(), bg, false);
            totalCell(t, dec(s2CoursTotal), bg, false);
            blankCell(t, bg); blankCell(t, bg);

            typeCell(t, "comp", bg);
            BigDecimal s2CompoTotal = subjectRowFromMap(t, subjects, b.s2Compo(), bg, false);
            totalCell(t, dec(s2CompoTotal), bg, false);
            blankCell(t, bg); blankCell(t, bg);

            typeCell(t, "Moy 2", bg);
            BigDecimal s2AvgTotal = subjectRowFromMap(t, subjects, b.s2Average(), bg, true);
            totalCell(t, dec(s2AvgTotal), bg, true);
            blankCell(t, bg); blankCell(t, bg);

            // Annuelle
            annuelleLabelCell(t, bg);
            BigDecimal annualTotal = subjectRowFromMap(t, subjects, b.annualBySubject(), bg, true);
            totalCell(t, dec(annualTotal), bg, true);
            tintedCell(t, dec(b.overallAverage()), Element.ALIGN_CENTER);
            tintedCell(t, b.rank() == null ? "—" : rankLabel(b.rank()), Element.ALIGN_CENTER);
        }

        t.setSpacingAfter(mm(3));
        doc.add(t);
    }

    private interface SubjectValue { BigDecimal get(SubjectColumn s); }

    private static BigDecimal subjectRow(PdfPTable t, List<SubjectColumn> subjects, SubjectValue fn, Color bg, boolean bold) {
        BigDecimal total = BigDecimal.ZERO;
        for (SubjectColumn s : subjects) {
            BigDecimal v = fn.get(s);
            if (v != null) total = total.add(v);
            bodyCell(t, dec(v), bg, bold);
        }
        return total;
    }

    private static BigDecimal subjectRowFromMap(PdfPTable t, List<SubjectColumn> subjects, Map<Long, BigDecimal> values, Color bg, boolean bold) {
        return subjectRow(t, subjects, s -> values == null ? null : values.get(s.subjectId()), bg, bold);
    }

    private static void footer(Document doc, TenantPublicInfo info) {
        String date = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH).format(java.time.LocalDate.now());
        String city = info != null ? info.city() : null;
        String note = "* Moyennes des notes coefficiées par matière · ** Moyenne générale · "
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

    private static void identityCell(PdfPTable table, String text, Color bg, int alignment, int rowspan) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(FontFactory.HELVETICA_BOLD, 7.5f, DARK)));
        cell.setRowspan(rowspan);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(bg);
        cell.setPadding(3f);
        cell.setBorderColor(BORDER);
        table.addCell(cell);
    }

    private static void semestreCell(PdfPTable table, String text, Color bg, int rowspan) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(FontFactory.HELVETICA_BOLD, 6.8f, GREY)));
        cell.setRowspan(rowspan);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(bg);
        cell.setPadding(3f);
        cell.setBorderColor(BORDER);
        table.addCell(cell);
    }

    private static void typeCell(PdfPTable table, String text, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(FontFactory.HELVETICA, 6.8f, GREY)));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(bg);
        cell.setPadding(3f);
        cell.setBorderColor(BORDER);
        table.addCell(cell);
    }

    private static void annuelleLabelCell(PdfPTable table, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase("Annuelle", font(FontFactory.HELVETICA_BOLD, 6.8f, DARK)));
        cell.setColspan(2);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
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
        cell.setBackgroundColor(bg == ZEBRA ? ZEBRA : new Color(240, 240, 240));
        cell.setPadding(2.8f);
        cell.setBorderColor(BORDER);
        table.addCell(cell);
    }

    private static void blankCell(PdfPTable table, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(""));
        cell.setBackgroundColor(bg);
        cell.setBorderColor(BORDER);
        cell.setPadding(2.8f);
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
