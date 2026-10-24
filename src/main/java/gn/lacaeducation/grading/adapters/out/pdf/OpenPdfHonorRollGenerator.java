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

            List<Row> ranked = roll.rows().stream()
                    .filter(r -> r.rank() != null)
                    .sorted((a, b) -> Integer.compare(a.rank(), b.rank()))
                    .toList();
            // Coupure par RANG (pas par nombre de lignes) : un groupe ex æquo à cheval sur la limite
            // reste entier (ex. 2 élèves à égalité en 5e position sont tous les deux affichés), plutôt
            // que de couper arbitrairement l'un des deux et laisser l'autre de côté.
            int cutoffRank = ranked.stream().map(Row::rank).distinct().sorted()
                    .limit(MAX_LAUREATES).reduce((a, b) -> b).orElse(0);
            List<Row> laureates = ranked.stream().filter(r -> r.rank() <= cutoffRank).toList();
            cards(doc, roll, laureates);
            footer(doc, info);

            for (Row laureate : laureates) {
                satisfecitPage(doc, roll, laureate, info, ranked.size());
            }

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

        // Médaille circulaire CHEVAUCHANT le bas de la photo (fidèle à la maquette de référence,
        // "margin-top:-6mm" en CSS) : dessinée via un PdfPCellEvent attaché à une cellule-espaceur
        // placée juste après la photo — son centre vertical est la jonction photo/espaceur (moitié du
        // cercle dans chacune), connue seulement une fois la pagination tranchée par iText, jamais
        // positionnée à la main après coup.
        Color medalColor = l.rank() != null && l.rank() == 1 ? GOLD : l.rank() != null && l.rank() <= 3 ? SILVER_MEDAL : DARK_GREEN;
        float medalRadius = mm(6);
        PdfPCell medalSpacer = new PdfPCell();
        medalSpacer.setBorder(0);
        medalSpacer.setFixedHeight(medalRadius);
        medalSpacer.setCellEvent(medalCellEvent(l, medalColor, medalRadius));
        PdfPTable medalWrap = new PdfPTable(1);
        medalWrap.setWidthPercentage(100);
        medalWrap.addCell(medalSpacer);
        content.addElement(medalWrap);

        if (l.exAequo()) {
            Paragraph exAequo = new Paragraph("EX ÆQUO", font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 6, LABEL_GREY));
            exAequo.setAlignment(Element.ALIGN_CENTER);
            exAequo.setSpacingBefore(1f);
            content.addElement(exAequo);
        }

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

    /** Médaille circulaire (rang + suffixe, fond or/argent/vert, liseré crème) — voir {@link #card}. */
    private static com.lowagie.text.pdf.PdfPCellEvent medalCellEvent(Row l, Color medalColor, float radius) {
        return (cell, position, canvases) -> {
            try {
                float cx = (position.getLeft() + position.getRight()) / 2f;
                float cy = position.getTop();
                com.lowagie.text.pdf.PdfContentByte cb = canvases[PdfPTable.LINECANVAS];
                cb.saveState();
                cb.setColorFill(medalColor);
                cb.ellipse(cx - radius, cy - radius, cx + radius, cy + radius);
                cb.fill();
                cb.setColorStroke(CREAM);
                cb.setLineWidth(mm(0.8f));
                cb.ellipse(cx - radius, cy - radius, cx + radius, cy + radius);
                cb.stroke();
                int rank = l.rank() != null ? l.rank() : 0;
                String rankText = rank + (rank == 1 ? "er" : "e");
                com.lowagie.text.pdf.ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                        new Phrase(rankText, font(SERIF_BOLD, FontFactory.TIMES_BOLD, 12.5f, WHITE)), cx, cy - radius * 0.32f, 0);
                cb.restoreState();
            } catch (RuntimeException e) {
                // Cosmétique uniquement — jamais faire échouer le tableau d'honneur pour une médaille manquée.
            }
        };
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

        // Bloc "Le Principal" + nom : positionné à droite de la ligne (comme la référence), mais le
        // texte est CENTRÉ par rapport à lui-même (align-items:center en CSS), pas aligné sur le bord
        // droit de toute la colonne — une table imbriquée étroite, alignée à droite, reproduit ce
        // comportement (un simple "right" sur tout le texte collerait les deux lignes au bord droit
        // de la page au lieu de les centrer l'une sur l'autre).
        PdfPCell right = new PdfPCell();
        right.setBorder(0);
        PdfPTable rightInner = new PdfPTable(1);
        rightInner.setWidthPercentage(46);
        rightInner.setHorizontalAlignment(Element.ALIGN_RIGHT);
        PdfPCell roleCell = new PdfPCell(new Phrase("Le Principal", font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 8, WHITE)));
        roleCell.setBorder(0);
        roleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        rightInner.addCell(roleCell);
        if (info != null && info.director() != null && !info.director().isBlank()) {
            PdfPCell directorCell = new PdfPCell(new Phrase(info.director(), font(SERIF_BOLD, FontFactory.TIMES_BOLD, 10.5f, WHITE)));
            directorCell.setBorder(0);
            directorCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            directorCell.setPaddingTop(1f);
            rightInner.addCell(directorCell);
        }
        right.addElement(rightInner);
        t.addCell(right);
        doc.add(t);
    }

    /** Certificat "Satisfecit" individuel d'un lauréat (§document-school, "satifaicit-tableau-
     * honneur.pdf") : une page par élève du tableau d'honneur, sur le fond crème à cadre décoratif
     * (voir {@link HonorRollPageBackground}). "Intègre les satisfecits" au même document que le
     * tableau d'honneur (demande utilisateur) plutôt qu'un document séparé à générer à part. */
    private static void satisfecitPage(Document doc, HonorRoll roll, Row row, TenantPublicInfo info, int totalRanked) {
        doc.newPage();

        PdfPTable head = new PdfPTable(new float[]{1f, 1f});
        head.setWidthPercentage(100);
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(0);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Image logo = info != null ? SchoolPdfHeader.loadLogo(info.logo()) : null;
        PdfPTable logoInner = new PdfPTable(new float[]{1f, 4f});
        logoInner.setWidthPercentage(100);
        PdfPCell logoImgCell = new PdfPCell();
        logoImgCell.setBorder(0);
        logoImgCell.setFixedHeight(mm(12));
        logoImgCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        if (logo != null) {
            logo.scaleToFit(mm(11), mm(11));
            logo.setAlignment(Element.ALIGN_MIDDLE);
            logoImgCell.addElement(logo);
        }
        logoInner.addCell(logoImgCell);
        PdfPCell schoolCell = new PdfPCell();
        schoolCell.setBorder(0);
        schoolCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        schoolCell.setPaddingLeft(mm(2.5f));
        schoolCell.addElement(new Paragraph(safe(info != null ? info.schoolName() : null, "École"),
                font(SERIF_BOLD, FontFactory.TIMES_BOLD, 11, DARK_GREEN)));
        Paragraph addr = new Paragraph(safe(join(" · ", info != null ? info.city() : null, "République de Guinée"), ""),
                font(SANS, FontFactory.HELVETICA, 7, TEXT_GREY));
        addr.setSpacingBefore(0.5f);
        schoolCell.addElement(addr);
        logoInner.addCell(schoolCell);
        logoCell.addElement(logoInner);
        head.addCell(logoCell);
        PdfPCell blank = new PdfPCell();
        blank.setBorder(0);
        head.addCell(blank);
        doc.add(head);

        Paragraph title = new Paragraph("Satisfecit", font(SERIF_ITALIC, FontFactory.TIMES_BOLDITALIC, 34, DARK_GREEN));
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingBefore(mm(6));
        doc.add(title);

        PdfPTable tricolor = new PdfPTable(new float[]{1f, 1f, 1f});
        tricolor.setWidthPercentage(14);
        tricolor.setHorizontalAlignment(Element.ALIGN_CENTER);
        tricolor.setSpacingBefore(1.5f);
        tricolor.setSpacingAfter(mm(5));
        tricolor.addCell(flagBar(new Color(206, 17, 38)));
        tricolor.addCell(flagBar(new Color(252, 209, 22)));
        tricolor.addCell(flagBar(new Color(0, 148, 96)));
        doc.add(tricolor);

        Paragraph kicker = new Paragraph("DÉCERNÉ À", font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 8.5f, LABEL_GREY));
        kicker.setAlignment(Element.ALIGN_CENTER);
        doc.add(kicker);

        String fullName = join(" ", row.studentFirstName(), row.studentLastName() == null ? null : row.studentLastName().toUpperCase(Locale.FRENCH));
        Paragraph name = new Paragraph(safe(fullName.isBlank() ? row.studentName() : fullName, "—"),
                font(SERIF_BOLD, FontFactory.TIMES_BOLD, 24, DARK_TEXT));
        name.setAlignment(Element.ALIGN_CENTER);
        name.setSpacingBefore(2f);
        doc.add(name);

        PdfPTable rule = new PdfPTable(1);
        rule.setWidthPercentage(16);
        rule.setHorizontalAlignment(Element.ALIGN_CENTER);
        rule.setSpacingBefore(2f);
        rule.setSpacingAfter(mm(5));
        PdfPCell ruleCell = new PdfPCell();
        ruleCell.setBorder(Rectangle.BOTTOM);
        ruleCell.setBorderColorBottom(GOLD);
        ruleCell.setBorderWidthBottom(0.8f);
        ruleCell.setFixedHeight(mm(2));
        rule.addCell(ruleCell);
        doc.add(rule);

        String description = "Élève de la classe de " + safe(row.className(), "—") + ", en reconnaissance de son excellent "
                + "travail, de sa régularité et de sa conduite exemplaire. " + safe(roll.periodLabel(), "")
                + " de l'année scolaire " + safe(roll.schoolYear(), "") + ".";
        Paragraph desc = new Paragraph(description, font(SANS, FontFactory.HELVETICA, 10, TEXT_GREY));
        desc.setAlignment(Element.ALIGN_CENTER);
        PdfPTable descWrap = new PdfPTable(1);
        descWrap.setWidthPercentage(62);
        descWrap.setHorizontalAlignment(Element.ALIGN_CENTER);
        PdfPCell descCell = new PdfPCell(desc);
        descCell.setBorder(0);
        descWrap.addCell(descCell);
        doc.add(descWrap);

        PdfPTable stats = new PdfPTable(new float[]{1f, 1f, 1f});
        stats.setWidthPercentage(46);
        stats.setHorizontalAlignment(Element.ALIGN_CENTER);
        stats.setSpacingBefore(mm(6));
        stats.addCell(statBox("MOYENNE", dec(row.average()) + " / 20", DARK_GREEN));
        String rangValue = row.rank() + (row.exAequo() ? "e ex æquo" : (row.rank() == 1 ? "er" : "e")) + " / " + totalRanked;
        stats.addCell(statBox("RANG", rangValue, DARK_TEXT));
        stats.addCell(statBox("MENTION", safe(row.honor(), "—"), GOLD));
        doc.add(stats);

        // Bloc signatures ancré visuellement en bas de page via un grand espaceur — document à contenu
        // fixe (toujours une seule page), comme le reste des documents de ce générateur.
        Paragraph spacer = new Paragraph(" ", font(SANS, FontFactory.HELVETICA, 6, WHITE));
        spacer.setSpacingBefore(mm(42));
        doc.add(spacer);

        PdfPTable sign = new PdfPTable(new float[]{1f, 1f, 1f});
        sign.setWidthPercentage(100);
        PdfPCell leftSign = new PdfPCell();
        leftSign.setBorder(0);
        leftSign.setVerticalAlignment(Element.ALIGN_BOTTOM);
        Paragraph leftLabel = new Paragraph("Le Professeur principal", font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 8.5f, DARK_TEXT));
        leftSign.addElement(leftLabel);
        leftSign.addElement(new Paragraph(" ", font(SANS, FontFactory.HELVETICA, 9, WHITE)));
        PdfPCell leftLine = new PdfPCell();
        leftLine.setBorder(Rectangle.TOP);
        leftLine.setBorderColorTop(BORDER_LIGHT);
        leftLine.setPadding(0);
        PdfPTable leftLineWrap = new PdfPTable(1);
        leftLineWrap.setWidthPercentage(70);
        leftLineWrap.addCell(leftLine);
        leftSign.addElement(leftLineWrap);
        sign.addCell(leftSign);

        PdfPCell seal = new PdfPCell();
        seal.setBorder(0);
        seal.setFixedHeight(mm(22));
        seal.setCellEvent(sealCellEvent(info, roll.schoolYear()));
        sign.addCell(seal);

        PdfPCell rightSign = new PdfPCell();
        rightSign.setBorder(0);
        rightSign.setHorizontalAlignment(Element.ALIGN_RIGHT);
        rightSign.setVerticalAlignment(Element.ALIGN_BOTTOM);
        Paragraph rightLabel = new Paragraph("Le Principal", font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 8.5f, DARK_TEXT));
        rightLabel.setAlignment(Element.ALIGN_RIGHT);
        rightSign.addElement(rightLabel);
        if (info != null && info.director() != null && !info.director().isBlank()) {
            Paragraph director = new Paragraph(info.director(), font(SERIF_BOLD, FontFactory.TIMES_BOLD, 11, DARK_TEXT));
            director.setAlignment(Element.ALIGN_RIGHT);
            director.setSpacingBefore(2f);
            rightSign.addElement(director);
        } else {
            rightSign.addElement(new Paragraph(" ", font(SANS, FontFactory.HELVETICA, 9, WHITE)));
        }
        PdfPCell rightLine = new PdfPCell();
        rightLine.setBorder(Rectangle.TOP);
        rightLine.setBorderColorTop(BORDER_LIGHT);
        rightLine.setPadding(0);
        PdfPTable rightLineWrap = new PdfPTable(1);
        rightLineWrap.setWidthPercentage(70);
        rightLineWrap.setHorizontalAlignment(Element.ALIGN_RIGHT);
        rightLineWrap.addCell(rightLine);
        rightSign.addElement(rightLineWrap);
        sign.addCell(rightSign);
        doc.add(sign);

        String city = info != null ? info.city() : null;
        String date = java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH).format(java.time.LocalDate.now());
        Paragraph faitA = new Paragraph((city != null && !city.isBlank() ? "Fait à " + city + ", " : "Fait ") + "le " + date,
                font(SANS, FontFactory.HELVETICA, 7.5f, TEXT_GREY));
        faitA.setAlignment(Element.ALIGN_CENTER);
        faitA.setSpacingBefore(1.5f);
        doc.add(faitA);
    }

    private static PdfPCell flagBar(Color color) {
        PdfPCell c = new PdfPCell();
        c.setBorder(0);
        c.setFixedHeight(mm(1));
        c.setBackgroundColor(color);
        return c;
    }

    private static PdfPCell statBox(String label, String value, Color valueColor) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.BOX);
        c.setBorderColor(BORDER_LIGHT);
        c.setBorderWidth(0.8f);
        c.setPadding(mm(2.5f));
        c.setBackgroundColor(WHITE);
        Paragraph l = new Paragraph(label, font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 6.5f, LABEL_GREY));
        l.setAlignment(Element.ALIGN_CENTER);
        c.addElement(l);
        Paragraph v = new Paragraph(value, font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 12, valueColor));
        v.setAlignment(Element.ALIGN_CENTER);
        v.setSpacingBefore(1.5f);
        c.addElement(v);
        return c;
    }

    /** Sceau circulaire "PNM [année]" (simple, sans rotation ni pointillés) — cosmétique uniquement,
     * ne doit jamais faire échouer la génération du certificat. */
    private static com.lowagie.text.pdf.PdfPCellEvent sealCellEvent(TenantPublicInfo info, String schoolYear) {
        return (cell, position, canvases) -> {
            try {
                float radius = mm(10);
                float cx = (position.getLeft() + position.getRight()) / 2f;
                float cy = (position.getBottom() + position.getTop()) / 2f;
                com.lowagie.text.pdf.PdfContentByte cb = canvases[PdfPTable.LINECANVAS];
                cb.saveState();
                cb.setColorStroke(GOLD);
                cb.setLineWidth(1f);
                cb.ellipse(cx - radius, cy - radius, cx + radius, cy + radius);
                cb.stroke();
                String code = info != null && info.schoolName() != null && !info.schoolName().isBlank()
                        ? initials(info.schoolName()) : "ÉC";
                com.lowagie.text.pdf.ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                        new Phrase(code, font(SERIF_BOLD, FontFactory.TIMES_BOLD, 11, GOLD)), cx, cy + mm(1.5f), 0);
                if (schoolYear != null && !schoolYear.isBlank()) {
                    com.lowagie.text.pdf.ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                            new Phrase(schoolYear, font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 5.5f, LABEL_GREY)), cx, cy - mm(2.5f), 0);
                }
                cb.restoreState();
            } catch (RuntimeException e) {
                // Cosmétique uniquement — jamais faire échouer le certificat pour un sceau manqué.
            }
        };
    }

    private static String initials(String schoolName) {
        StringBuilder sb = new StringBuilder();
        for (String word : schoolName.trim().split("\\s+")) {
            if (!word.isEmpty() && Character.isLetter(word.charAt(0)) && sb.length() < 3) {
                sb.append(Character.toUpperCase(word.charAt(0)));
            }
        }
        return sb.length() == 0 ? "ÉC" : sb.toString();
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
