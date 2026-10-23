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
import gn.lacaeducation.common.pdf.RoundedCellBorder;
import gn.lacaeducation.common.pdf.SchoolPdfHeader;
import gn.lacaeducation.grading.application.port.out.HonorRollPdfGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/**
 * Tableau d'honneur (§document-school, "Tableau d'honneur.dc.html") : affiche cérémoniale paysage vert
 * foncé, cadre doré, jusqu'à 5 lauréats (les mieux classés DE TOUTE L'ÉCOLE, toutes classes confondues)
 * en cartes blanches avec photo, médaille de rang, moyenne, classe d'origine et mention — reproduit
 * fidèlement la maquette de référence. N'inclut PAS les pages "Satisfecit" individuelles de la maquette
 * (certificats par lauréat) — hors scope de cette passe.
 */
@Component
@RequiredArgsConstructor
class OpenPdfHonorRollGenerator implements HonorRollPdfGenerator {

    private static final int MAX_LAUREATES = 5;

    private static final Color DARK_GREEN = new Color(14, 77, 58);
    private static final Color GOLD = new Color(184, 137, 43);
    private static final Color GOLD_TEXT = new Color(233, 215, 158);
    private static final Color SILVER_MEDAL = new Color(124, 138, 132);
    private static final Color CREAM = new Color(255, 253, 248);
    private static final Color LABEL_GREY = new Color(138, 122, 85);
    private static final Color TEXT_GREY = new Color(94, 107, 101);
    private static final Color DARK_TEXT = new Color(19, 38, 31);
    private static final Color BORDER_LIGHT = new Color(226, 220, 205);
    private static final Color WHITE = Color.WHITE;

    private static final com.lowagie.text.pdf.BaseFont SANS = gn.lacaeducation.common.pdf.DesignFonts.SANS;
    private static final com.lowagie.text.pdf.BaseFont SANS_BOLD = gn.lacaeducation.common.pdf.DesignFonts.SANS_SEMIBOLD;
    private static final com.lowagie.text.pdf.BaseFont SERIF_BOLD = gn.lacaeducation.common.pdf.DesignFonts.SERIF_BOLD;
    private static final com.lowagie.text.pdf.BaseFont SERIF_ITALIC = gn.lacaeducation.common.pdf.DesignFonts.SERIF_SEMIBOLD_ITALIC;

    private final TenantInfoProvider tenantInfoProvider;

    @Override
    public byte[] generate(HonorRoll roll) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), mm(12), mm(12), mm(9), mm(8));
        try {
            TenantPublicInfo info = roll.tenantId() == null ? null
                    : tenantInfoProvider.findTenantInfo(roll.tenantId()).orElse(null);

            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new HonorRollPageBackground());
            doc.open();

            header(doc, info);
            title(doc, roll);

            List<Row> laureates = roll.rows().stream()
                    .filter(r -> r.rank() != null)
                    .sorted((a, b) -> Integer.compare(a.rank(), b.rank()))
                    .limit(MAX_LAUREATES)
                    .toList();
            cards(doc, roll, laureates);
            footer(doc, info);

            doc.close();
            return out.toByteArray();
        } catch (RuntimeException e) {
            if (doc.isOpen()) doc.close();
            throw e;
        }
    }

    private static void header(Document doc, TenantPublicInfo info) {
        PdfPTable t = new PdfPTable(new float[]{1f, 1f});
        t.setWidthPercentage(100);
        t.setSpacingAfter(0);

        PdfPCell left = new PdfPCell();
        left.setBorder(0);
        PdfPTable inner = new PdfPTable(new float[]{1f, 4f});
        inner.setWidthPercentage(100);
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(0);
        logoCell.setFixedHeight(mm(15));
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logoCell.setBackgroundColor(WHITE);
        Image logo = info != null ? SchoolPdfHeader.loadLogo(info.logo()) : null;
        if (logo != null) {
            logo.scaleToFit(mm(12), mm(12));
            logo.setAlignment(Element.ALIGN_MIDDLE);
            logoCell.addElement(logo);
        }
        inner.addCell(logoCell);
        PdfPCell nameCell = new PdfPCell();
        nameCell.setBorder(0);
        nameCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        nameCell.setPaddingLeft(mm(3));
        nameCell.addElement(new Paragraph(safe(info != null ? info.schoolName() : null, "École"),
                font(SERIF_BOLD, FontFactory.TIMES_BOLD, 14, WHITE)));
        Paragraph addr = new Paragraph(safe(join(" · ", info != null ? info.city() : null, "Guinée"), ""),
                font(SANS, FontFactory.HELVETICA, 7.5f, GOLD_TEXT));
        addr.setSpacingBefore(0.5f);
        nameCell.addElement(addr);
        inner.addCell(nameCell);
        left.addElement(inner);
        t.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(0);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph republic = new Paragraph("RÉPUBLIQUE DE GUINÉE", font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 8, WHITE));
        republic.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(republic);
        Paragraph motto = new Paragraph("Travail — Justice — Solidarité", font(SERIF_ITALIC, FontFactory.TIMES_ITALIC, 9, GOLD_TEXT));
        motto.setAlignment(Element.ALIGN_RIGHT);
        motto.setSpacingBefore(1f);
        right.addElement(motto);
        t.addCell(right);
        doc.add(t);
    }

    private static void title(Document doc, HonorRoll roll) {
        Paragraph kicker = new Paragraph(safe(roll.periodLabel(), "").toUpperCase(Locale.FRENCH),
                font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 8.5f, GOLD_TEXT));
        kicker.setAlignment(Element.ALIGN_CENTER);
        kicker.setSpacingBefore(mm(2));
        doc.add(kicker);

        Paragraph title = new Paragraph("Tableau d'honneur", font(SERIF_BOLD, FontFactory.TIMES_BOLD, 28, WHITE));
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingBefore(1f);
        doc.add(title);

        PdfPTable rule = new PdfPTable(new float[]{1f, 0f, 1f});
        rule.setWidthPercentage(60);
        rule.setHorizontalAlignment(Element.ALIGN_CENTER);
        rule.setSpacingBefore(2f);
        rule.setSpacingAfter(mm(2.5f));
        PdfPCell l1 = new PdfPCell();
        l1.setBorder(Rectangle.BOTTOM);
        l1.setBorderColorBottom(GOLD);
        l1.setBorderWidthBottom(0.6f);
        l1.setFixedHeight(mm(5));
        rule.addCell(l1);
        String sub = join(" · ", "TOUTES CLASSES",
                roll.schoolYear() != null ? "ANNÉE SCOLAIRE " + roll.schoolYear() : null);
        PdfPCell mid = new PdfPCell(new Phrase(sub, font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 9, WHITE)));
        mid.setBorder(0);
        mid.setHorizontalAlignment(Element.ALIGN_CENTER);
        mid.setPadding(0);
        mid.setPaddingLeft(mm(3));
        mid.setPaddingRight(mm(3));
        mid.setNoWrap(true);
        rule.addCell(mid);
        PdfPCell l2 = new PdfPCell();
        l2.setBorder(Rectangle.BOTTOM);
        l2.setBorderColorBottom(GOLD);
        l2.setBorderWidthBottom(0.6f);
        l2.setFixedHeight(mm(5));
        rule.addCell(l2);
        doc.add(rule);
    }

    private static void cards(Document doc, HonorRoll roll, List<Row> laureates) {
        if (laureates.isEmpty()) {
            Paragraph empty = new Paragraph("Aucun bulletin généré pour cette période.",
                    font(SANS, FontFactory.HELVETICA_OBLIQUE, 10, GOLD_TEXT));
            empty.setAlignment(Element.ALIGN_CENTER);
            empty.setSpacingBefore(mm(10));
            doc.add(empty);
            return;
        }
        float[] widths = new float[laureates.size()];
        java.util.Arrays.fill(widths, 1f);
        PdfPTable row = new PdfPTable(widths);
        row.setWidthPercentage(laureates.size() * 18f);
        row.setHorizontalAlignment(Element.ALIGN_CENTER);
        row.setSpacingBefore(mm(2));
        for (Row l : laureates) {
            row.addCell(card(l));
        }
        doc.add(row);
    }

    private static PdfPCell card(Row l) {
        PdfPCell outer = new PdfPCell();
        outer.setBorder(0);
        outer.setPadding(1f);
        outer.setVerticalAlignment(Element.ALIGN_TOP);

        PdfPTable inner = new PdfPTable(1);
        inner.setWidthPercentage(100);

        PdfPCell content = new PdfPCell();
        content.setBorder(0);
        content.setPadding(mm(1.8f));
        content.setCellEvent(RoundedCellBorder.fillOnly(mm(3.2f), CREAM));

        // Rang en texte coloré (pas de badge circulaire chevauchant la photo, trop fragile à obtenir
        // de façon fiable avec PdfPTable — la référence CSS utilise un margin-top négatif qu'OpenPDF
        // ne reproduit pas) : une couleur par palier (or/argent/vert) reste un repère visuel clair.
        Color medalColor = l.rank() != null && l.rank() == 1 ? GOLD : l.rank() != null && l.rank() <= 3 ? SILVER_MEDAL : DARK_GREEN;
        Paragraph medal = new Paragraph((l.rank() != null ? l.rank() : 0) + (l.rank() != null && l.rank() == 1 ? "er" : "e"),
                font(SERIF_BOLD, FontFactory.TIMES_BOLD, 18, medalColor));
        medal.setAlignment(Element.ALIGN_CENTER);
        content.addElement(medal);

        PdfPCell photoCell = new PdfPCell();
        photoCell.setBorder(0);
        photoCell.setFixedHeight(mm(24));
        photoCell.setPadding(0);
        photoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        photoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        photoCell.setCellEvent(RoundedCellBorder.fillOnly(mm(2.2f), new Color(241, 236, 223)));
        Image photo = SchoolPdfHeader.loadLogo(l.studentPhoto());
        if (photo != null) {
            photo.scaleToFit(mm(40), mm(23));
            photo.setAlignment(Element.ALIGN_MIDDLE);
            photoCell.addElement(photo);
        }
        PdfPTable photoWrap = new PdfPTable(1);
        photoWrap.setWidthPercentage(100);
        photoWrap.setSpacingBefore(1.5f);
        photoWrap.addCell(photoCell);
        content.addElement(photoWrap);

        Paragraph first = new Paragraph(safe(l.studentFirstName(), ""), font(SANS, FontFactory.HELVETICA, 7.5f, TEXT_GREY));
        first.setAlignment(Element.ALIGN_CENTER);
        first.setSpacingBefore(1f);
        content.addElement(first);
        Paragraph last = new Paragraph(safe(l.studentLastName(), "—").toUpperCase(Locale.FRENCH), font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 11, DARK_TEXT));
        last.setAlignment(Element.ALIGN_CENTER);
        content.addElement(last);

        PdfPTable statRow = new PdfPTable(new float[]{1f, 1f});
        statRow.setWidthPercentage(100);
        statRow.setSpacingBefore(1.5f);
        PdfPCell avgCell = new PdfPCell();
        avgCell.setBorder(Rectangle.TOP);
        avgCell.setBorderColorTop(BORDER_LIGHT);
        avgCell.setBorderWidthTop(0.6f);
        avgCell.setPadding(0);
        avgCell.setPaddingTop(1f);
        avgCell.addElement(new Paragraph("MOYENNE", font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 5.5f, LABEL_GREY)));
        Phrase avgPhrase = new Phrase();
        avgPhrase.add(new com.lowagie.text.Chunk(dec(l.average()), font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 12, DARK_GREEN)));
        avgPhrase.add(new com.lowagie.text.Chunk(" /20", font(SANS, FontFactory.HELVETICA, 6.5f, TEXT_GREY)));
        Paragraph avgP = new Paragraph(avgPhrase);
        avgCell.addElement(avgP);
        statRow.addCell(avgCell);
        PdfPCell classCell = new PdfPCell();
        classCell.setBorder(Rectangle.TOP);
        classCell.setBorderColorTop(BORDER_LIGHT);
        classCell.setBorderWidthTop(0.6f);
        classCell.setPadding(0);
        classCell.setPaddingTop(1f);
        classCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph classLabel = new Paragraph("CLASSE", font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 5.5f, LABEL_GREY));
        classLabel.setAlignment(Element.ALIGN_RIGHT);
        classCell.addElement(classLabel);
        Paragraph classValue = new Paragraph(safe(l.className(), "—"), font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 7.5f, DARK_TEXT));
        classValue.setAlignment(Element.ALIGN_RIGHT);
        classCell.addElement(classValue);
        statRow.addCell(classCell);
        content.addElement(statRow);

        Paragraph mention = new Paragraph(safe(l.honor(), "—"), font(SERIF_ITALIC, FontFactory.TIMES_BOLDITALIC, 9.5f, GOLD));
        mention.setAlignment(Element.ALIGN_CENTER);
        mention.setSpacingBefore(1.5f);
        content.addElement(mention);

        inner.addCell(content);
        outer.addElement(inner);
        return outer;
    }

    private static void footer(Document doc, TenantPublicInfo info) {
        PdfPTable t = new PdfPTable(new float[]{1f, 1f});
        t.setWidthPercentage(100);
        t.setSpacingBefore(mm(2.5f));

        PdfPCell left = new PdfPCell(new Phrase(safe(info != null ? join(" / ", info.phone(), info.phone2()) : null, ""),
                font(SANS, FontFactory.HELVETICA, 7.5f, GOLD_TEXT)));
        left.setBorder(0);
        left.setVerticalAlignment(Element.ALIGN_BOTTOM);
        t.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(0);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph role = new Paragraph("Le Principal", font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 8, WHITE));
        role.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(role);
        if (info != null && info.director() != null && !info.director().isBlank()) {
            Paragraph director = new Paragraph(info.director(), font(SERIF_BOLD, FontFactory.TIMES_BOLD, 10.5f, WHITE));
            director.setAlignment(Element.ALIGN_RIGHT);
            director.setSpacingBefore(1f);
            right.addElement(director);
        }
        t.addCell(right);
        doc.add(t);
    }

    private static com.lowagie.text.Font font(com.lowagie.text.pdf.BaseFont base, String fallbackFamily, float size, Color color) {
        if (base != null) {
            com.lowagie.text.Font f = new com.lowagie.text.Font(base, size);
            f.setColor(color);
            return f;
        }
        return FontFactory.getFont(fallbackFamily, size, color);
    }

    private static float mm(float v) { return v * 72f / 25.4f; }

    private static String safe(String s) { return s == null || s.isBlank() ? "—" : s; }

    private static String safe(String s, String fallback) { return s == null || s.isBlank() ? fallback : s; }

    private static String dec(BigDecimal v) { return v == null ? "—" : v.toPlainString(); }

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
