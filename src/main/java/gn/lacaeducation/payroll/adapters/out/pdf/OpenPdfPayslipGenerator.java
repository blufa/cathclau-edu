package gn.lacaeducation.payroll.adapters.out.pdf;

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
import gn.lacaeducation.payroll.application.port.out.PayslipPdfGenerator;
import gn.lacaeducation.payroll.application.port.out.PayslipPdfGenerator.TeacherHours;
import gn.lacaeducation.payroll.domain.EmployeeType;
import gn.lacaeducation.payroll.domain.Payslip;
import gn.lacaeducation.payroll.domain.PayslipLine;
import gn.lacaeducation.payroll.domain.PayslipLineType;
import gn.lacaeducation.payroll.domain.PayslipStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Fiche de paie — reproduit fidèlement le design premium de référence (§retour fondateur,
 * /features/document-school/Fiche de paie.dc.html, demande utilisateur) : fond crème, liseré tricolore
 * guinéen sur le bord gauche, en-tête logo/école/République, titre encadré d'un filet or, badge de
 * statut, carte d'identité du salarié (photo + nom + rôle) et carte période/mode de paiement, volume
 * horaire du mois en 4 cartes + barre de progression (enseignants horaires uniquement), tableau de
 * rémunération à coins arrondis, bloc NET À PAYER en couleur pleine, signatures bénéficiaire/
 * établissement avec les noms imprimés dessous.
 *
 * <p>Contrairement au bulletin/tableau d'honneur (couleur fixe, §grading), la couleur d'accent ICI suit
 * la couleur de marque du tenant ({@link TenantPublicInfo#brandColor()}, via
 * {@link SchoolPdfHeader#parseColor}) — demande explicite de l'utilisateur ("la couleur sur ces
 * documents doit être personnalisable"). Seules les teintes neutres (gris/or des libellés, bordures de
 * carte) restent fixes, comme sur les autres documents "design premium".</p>
 */
@Component
@RequiredArgsConstructor
class OpenPdfPayslipGenerator implements PayslipPdfGenerator {

    // ── Palette neutre (fixe, comme les autres documents "design premium") ────────
    private static final Color GOLD = new Color(184, 137, 43);
    private static final Color BORDER = new Color(217, 210, 192);
    private static final Color BORDER_LIGHT = new Color(234, 228, 214);
    private static final Color LABEL_GREY = new Color(138, 122, 85);
    private static final Color TEXT_GREY = new Color(94, 107, 101);
    private static final Color DARK_TEXT = new Color(19, 38, 31);
    private static final Color TOTAL_BG = new Color(244, 241, 234);
    private static final Color WHITE = Color.WHITE;
    // "Heures non réalisées" / retenues : rouge d'alerte, jamais dérivé de la couleur de marque (reste
    // lisible comme une alerte quelle que soit la couleur choisie par l'école).
    private static final Color ALERT_BG = new Color(251, 239, 237);
    private static final Color ALERT_BORDER = new Color(240, 207, 203);
    private static final Color ALERT_TEXT = new Color(179, 38, 30);
    // Pastilles de statut (DRAFT/VALIDATED) : neutres, seule PAYÉE suit la couleur de marque (voir
    // statusPill) — cohérent avec STATUS_TONE du frontend (payroll/page.tsx).
    private static final Color DRAFT_BG = new Color(241, 245, 249);
    private static final Color DRAFT_TEXT = new Color(71, 85, 105);
    private static final Color VALIDATED_BG = new Color(224, 231, 255);
    private static final Color VALIDATED_TEXT = new Color(67, 56, 202);
    private static final Color SIGNATURE_LINE = new Color(184, 176, 156);

    private static final com.lowagie.text.pdf.BaseFont SANS = gn.lacaeducation.common.pdf.DesignFonts.SANS;
    private static final com.lowagie.text.pdf.BaseFont SANS_BOLD = gn.lacaeducation.common.pdf.DesignFonts.SANS_SEMIBOLD;
    private static final com.lowagie.text.pdf.BaseFont SANS_EXTRABOLD = gn.lacaeducation.common.pdf.DesignFonts.SANS_EXTRABOLD;
    private static final com.lowagie.text.pdf.BaseFont SERIF_BOLD = gn.lacaeducation.common.pdf.DesignFonts.SERIF_BOLD;
    private static final com.lowagie.text.pdf.BaseFont SERIF_ITALIC = gn.lacaeducation.common.pdf.DesignFonts.SERIF_SEMIBOLD_ITALIC;
    private static final com.lowagie.text.pdf.BaseFont MONO = gn.lacaeducation.common.pdf.DesignFonts.MONO;
    private static final com.lowagie.text.pdf.BaseFont MONO_BOLD = gn.lacaeducation.common.pdf.DesignFonts.MONO_SEMIBOLD;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final TenantInfoProvider tenantInfoProvider;

    @Override
    public byte[] generate(Payslip p, List<PayslipLine> lines, TeacherHours hours, Presentation presentation) {
        if (presentation == null) presentation = Presentation.EMPTY;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, mm(15), mm(12), mm(10), mm(16));
        try {
            TenantPublicInfo info = p.getTenantId() == null ? null
                    : tenantInfoProvider.findTenantInfo(p.getTenantId()).orElse(null);
            Color accent = SchoolPdfHeader.parseColor(info == null ? null : info.brandColor());
            Color accentTint = SchoolPdfHeader.tint(accent, 0.12);

            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new PremiumPageBackground());
            writer.setPageEvent(new SchoolPdfPageFooter(info, accent));
            doc.open();

            boolean isTeacher = p.getEmployeeType() == null || p.getEmployeeType() == EmployeeType.TEACHER;
            // Signal déjà utilisé côté service pour décider d'afficher le volume horaire — repris ici
            // tel quel pour le libellé "rémunération à l'heure"/"salaire fixe" (même limite connue :
            // computeMonthlyPay calcule des heures même pour un enseignant en mode FIXE, voir
            // PayrollService#pdfBytes — comportement hérité, pas une régression de cette refonte).
            boolean hourly = hours != null;

            header(doc, info);
            titleRow(doc, accent, accentTint, p, isTeacher, hourly);
            identityCard(doc, p, presentation, isTeacher, hourly);
            if (hours != null) hoursBlock(doc, accent, hours);
            remunerationTable(doc, accent, p, lines, hourly, hours);
            netBox(doc, accent, p);
            signatures(doc, p, presentation);

            doc.close();
            return out.toByteArray();
        } catch (RuntimeException e) {
            if (doc.isOpen()) doc.close();
            throw new IllegalStateException("Échec de génération du PDF de la fiche de paie " + p.getId(), e);
        }
    }

    // ── En-tête : logo/école/adresse (gauche) + République/ministère (droite) ─────
    private static void header(Document doc, TenantPublicInfo info) {
        PdfPTable t = new PdfPTable(new float[]{1.3f, 1f});
        t.setWidthPercentage(100);
        t.setSpacingAfter(mm(4));

        PdfPCell left = new PdfPCell();
        left.setBorder(0);
        left.setVerticalAlignment(Element.ALIGN_TOP);
        PdfPTable leftInner = new PdfPTable(new float[]{1f, 3f});
        leftInner.setWidthPercentage(100);
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(0);
        logoCell.setFixedHeight(mm(19));
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logoCell.setCellEvent(RoundedCellBorder.fillAndBorder(mm(4), WHITE, BORDER, 0.8f));
        Image logo = info != null ? SchoolPdfHeader.loadLogo(info.logo()) : null;
        if (logo != null) {
            logo.scaleToFit(mm(16), mm(16));
            logo.setAlignment(Element.ALIGN_MIDDLE);
            logoCell.addElement(logo);
        }
        leftInner.addCell(logoCell);
        PdfPCell nameCell = new PdfPCell();
        nameCell.setBorder(0);
        nameCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        nameCell.setPaddingLeft(mm(4));
        Paragraph name = new Paragraph(safe(info != null ? info.schoolName() : null, "École"),
                font(SERIF_BOLD, FontFactory.TIMES_BOLD, 18, SchoolPdfHeader.DARK));
        name.setLeading(16f);
        nameCell.addElement(name);
        String addr = info != null ? join(" · ", info.address(), info.city(), "Guinée") : "";
        if (!addr.isBlank()) {
            Paragraph a = new Paragraph(addr, font(SANS, FontFactory.HELVETICA, 8, TEXT_GREY));
            a.setSpacingBefore(1.5f);
            nameCell.addElement(a);
        }
        String phones = info != null ? join(" / ", info.phone(), info.phone2()) : "";
        if (!phones.isBlank()) {
            Paragraph ph = new Paragraph("Tél : " + phones, font(SANS, FontFactory.HELVETICA, 8, TEXT_GREY));
            ph.setSpacingBefore(0.5f);
            nameCell.addElement(ph);
        }
        leftInner.addCell(nameCell);
        left.addElement(leftInner);
        t.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(0);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        right.setVerticalAlignment(Element.ALIGN_TOP);
        Paragraph republic = new Paragraph("RÉPUBLIQUE DE GUINÉE", font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 8.5f, DARK_TEXT));
        republic.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(republic);
        Paragraph motto = new Paragraph("Travail — Justice — Solidarité", font(SERIF_ITALIC, FontFactory.TIMES_BOLDITALIC, 10, TEXT_GREY));
        motto.setAlignment(Element.ALIGN_RIGHT);
        motto.setSpacingBefore(1f);
        right.addElement(motto);
        String ireDpe = join(" · ", prefix("IRE : ", info != null ? info.ire() : null), prefix("DPE/DCE : ", info != null ? info.dpeLabel() : null));
        String sousPref = prefix("DSSEE : ", info != null ? info.sousPrefectureLabel() : null);
        Paragraph adminP = new Paragraph("MEPU-A", font(SANS, FontFactory.HELVETICA, 7, TEXT_GREY));
        adminP.setAlignment(Element.ALIGN_RIGHT);
        adminP.setLeading(9.5f);
        adminP.setSpacingBefore(2f);
        if (!ireDpe.isBlank()) adminP.add("\n" + ireDpe);
        if (!sousPref.isBlank()) adminP.add("\n" + sousPref);
        right.addElement(adminP);
        t.addCell(right);

        doc.add(t);
    }

    // ── Eyebrow + titre + pastille de statut, filet or en bas ──────────────────────
    private static void titleRow(Document doc, Color accent, Color accentTint, Payslip p, boolean isTeacher, boolean hourly) {
        PdfPTable t = new PdfPTable(new float[]{3f, 1f});
        t.setWidthPercentage(100);
        t.setSpacingAfter(mm(4));

        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.BOTTOM);
        left.setBorderColorBottom(GOLD);
        left.setBorderWidthBottom(0.8f);
        left.setVerticalAlignment(Element.ALIGN_BOTTOM);
        left.setPaddingBottom(mm(2.5f));
        String kind = (isTeacher ? "ENSEIGNANT" : "PERSONNEL") + " · " + (hourly ? "RÉMUNÉRATION À L'HEURE" : "SALAIRE MENSUEL FIXE");
        left.addElement(new Paragraph(kind, font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 7, LABEL_GREY)));
        Paragraph title = new Paragraph("Fiche de paie", font(SERIF_BOLD, FontFactory.TIMES_BOLD, 26, accent));
        title.setSpacingBefore(1f);
        left.addElement(title);
        t.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.BOTTOM);
        right.setBorderColorBottom(GOLD);
        right.setBorderWidthBottom(0.8f);
        right.setVerticalAlignment(Element.ALIGN_BOTTOM);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        right.setPaddingBottom(mm(4));
        right.addElement(statusPill(p.getStatus(), accent, accentTint));
        t.addCell(right);

        doc.add(t);
    }

    private static PdfPTable statusPill(PayslipStatus status, Color accent, Color accentTint) {
        Color bg; Color text; Color dot; String label;
        switch (status == null ? PayslipStatus.DRAFT : status) {
            case PAID -> { bg = accentTint; text = accent; dot = accent; label = "PAYÉE"; }
            case VALIDATED -> { bg = VALIDATED_BG; text = VALIDATED_TEXT; dot = VALIDATED_TEXT; label = "VALIDÉE"; }
            default -> { bg = DRAFT_BG; text = DRAFT_TEXT; dot = DRAFT_TEXT; label = "BROUILLON"; }
        }
        // Un seul niveau d'imbrication table-dans-cellule (pas de PdfPTable intermédiaire type padded())
        // sous la cellule à fond arrondi : au-delà d'un niveau, le contenu (texte ET fond) de la table
        // imbriquée la plus profonde cesse de s'afficher — même bug observé et contourné pour la barre
        // de progression (voir hoursBlock) et la cellule photo (voir identityCard). Le padding du
        // "pilule" est donc posé directement sur les cellules dot/label plutôt que via un wrapper.
        PdfPTable inner = new PdfPTable(new float[]{0.14f, 1f});
        inner.setWidthPercentage(100);
        PdfPCell dotCell = new PdfPCell();
        dotCell.setBorder(0);
        dotCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        dotCell.setPaddingLeft(mm(3.2f));
        dotCell.setPaddingTop(mm(1.4f));
        dotCell.setPaddingBottom(mm(1.4f));
        dotCell.setBackgroundColor(dot);
        dotCell.setFixedHeight(mm(2));
        inner.addCell(dotCell);
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 8, text)));
        labelCell.setBorder(0);
        labelCell.setNoWrap(true);
        labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        labelCell.setPaddingLeft(mm(1.5f));
        labelCell.setPaddingRight(mm(3.2f));
        labelCell.setPaddingTop(mm(1.4f));
        labelCell.setPaddingBottom(mm(1.4f));
        inner.addCell(labelCell);

        PdfPCell outer = new PdfPCell();
        outer.setBorder(0);
        outer.setPadding(0);
        outer.setCellEvent(RoundedCellBorder.fillOnly(mm(4), bg));
        outer.addElement(inner);
        PdfPTable wrapper = new PdfPTable(1);
        wrapper.setWidthPercentage(60f);
        wrapper.setHorizontalAlignment(Element.ALIGN_RIGHT);
        wrapper.addCell(outer);
        return wrapper;
    }

    /** Enveloppe une table dans une cellule avec un padding vertical/horizontal donné (pour composer la
     * pastille de statut à l'intérieur de son fond arrondi sans toucher directement au padding de
     * {@code inner}, qui a déjà sa propre mise en page interne). */
    private static PdfPTable padded(PdfPTable inner, float vPad, float hPad) {
        PdfPCell c = new PdfPCell();
        c.setBorder(0);
        c.setPadding(vPad);
        c.setPaddingLeft(hPad);
        c.setPaddingRight(hPad);
        c.addElement(inner);
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.addCell(c);
        return t;
    }

    // ── Carte identité (photo + nom + rôle) + carte période/mode de paiement ──────
    private static void identityCard(Document doc, Payslip p, Presentation presentation, boolean isTeacher, boolean hourly) {
        PdfPTable t = new PdfPTable(new float[]{1.3f, 1f});
        t.setWidthPercentage(100);
        t.setSpacingAfter(mm(4));

        PdfPTable left = new PdfPTable(new float[]{1f, 2.2f});
        left.setWidthPercentage(100);
        PdfPCell photoCell = new PdfPCell();
        photoCell.setBorder(0);
        photoCell.setFixedHeight(mm(22));
        photoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        photoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        // setBackgroundColor (pas RoundedCellBorder) : cette cellule est imbriquée dans le fond déjà
        // arrondi de la carte identité (leftOuter, plus bas) — voir le commentaire équivalent dans
        // statusPill sur le même bug de rendu.
        photoCell.setBackgroundColor(new Color(241, 236, 223));
        Image photo = SchoolPdfHeader.loadLogo(presentation.beneficiaryPhotoUrl());
        if (photo != null) {
            photo.scaleToFit(mm(18), mm(22));
            photo.setAlignment(Element.ALIGN_MIDDLE);
            photoCell.addElement(photo);
        }
        left.addCell(photoCell);
        PdfPCell infoCell = new PdfPCell();
        infoCell.setBorder(0);
        infoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        infoCell.setPaddingLeft(mm(4));
        infoCell.addElement(new Paragraph(whoLabel(p.getEmployeeType(), isTeacher),
                font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 6.5f, LABEL_GREY)));
        Paragraph name = new Paragraph(safe(p.getTeacherName(), "#" + p.getTeacherId()),
                font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 13, DARK_TEXT));
        name.setSpacingBefore(1.2f);
        infoCell.addElement(name);
        String role = join(" · ", presentation.beneficiaryRole(), hourly ? "payé à l'heure" : "salaire fixe");
        Paragraph roleP = new Paragraph(safe(role, isTeacher ? "Enseignant" : "Personnel"),
                font(SANS, FontFactory.HELVETICA, 8, TEXT_GREY));
        roleP.setSpacingBefore(0.8f);
        infoCell.addElement(roleP);
        left.addCell(infoCell);

        PdfPCell leftOuter = new PdfPCell();
        leftOuter.setBorder(0);
        leftOuter.setPadding(0);
        leftOuter.setPaddingRight(mm(2));
        leftOuter.setCellEvent(RoundedCellBorder.fillAndBorder(mm(3), WHITE, BORDER, 0.8f));
        leftOuter.addElement(padded(left, mm(4), mm(4)));
        t.addCell(leftOuter);

        PdfPTable right = new PdfPTable(new float[]{1f, 1.2f});
        right.setWidthPercentage(100);
        fieldRow(right, "Période", safe(p.getPeriodLabel(), "—"), false);
        fieldRow(right, "Du / Au",
                (p.getPeriodStart() == null ? "—" : p.getPeriodStart().format(DATE)) + " – "
                        + (p.getPeriodEnd() == null ? "—" : p.getPeriodEnd().format(DATE)), true);
        fieldRow(right, "Mode de paiement", paymentMethodLabel(p.getPaymentMethod()), false);

        PdfPCell rightOuter = new PdfPCell();
        rightOuter.setBorder(0);
        rightOuter.setPadding(0);
        rightOuter.setPaddingLeft(mm(2));
        rightOuter.setCellEvent(RoundedCellBorder.fillAndBorder(mm(3), WHITE, BORDER, 0.8f));
        rightOuter.addElement(padded(right, mm(3.5f), mm(4)));
        t.addCell(rightOuter);

        doc.add(t);
    }

    private static void fieldRow(PdfPTable t, String label, String value, boolean mono) {
        PdfPCell l = new PdfPCell(new Phrase(label, font(SANS, FontFactory.HELVETICA, 9, TEXT_GREY)));
        l.setBorder(0);
        l.setPaddingBottom(mm(1.5f));
        t.addCell(l);
        PdfPCell v = new PdfPCell(new Phrase(value, mono
                ? font(MONO, FontFactory.COURIER, 8.5f, DARK_TEXT)
                : font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 9, DARK_TEXT)));
        v.setBorder(0);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        v.setPaddingBottom(mm(1.5f));
        t.addCell(v);
    }

    private static String whoLabel(EmployeeType type, boolean isTeacher) {
        if (isTeacher) return "ENSEIGNANT";
        return switch (type) {
            case DIRECTION -> "MEMBRE DE LA DIRECTION";
            case ADMINISTRATIVE -> "PERSONNEL ADMINISTRATIF";
            case SUPPORT -> "PERSONNEL DE SOUTIEN";
            default -> "PERSONNEL";
        };
    }

    private static String paymentMethodLabel(String raw) {
        if (raw == null || raw.isBlank()) return "—";
        return switch (raw) {
            case "BANK_TRANSFER" -> "Virement bancaire";
            case "CASH" -> "Espèces";
            case "MOBILE_MONEY" -> "Mobile Money";
            default -> raw;
        };
    }

    // ── Volume horaire du mois : 4 cartes + barre de progression ──────────────────
    private static void hoursBlock(Document doc, Color accent, TeacherHours hours) {
        Paragraph label = new Paragraph("VOLUME HORAIRE DU MOIS", font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 7, accent));
        label.setSpacingAfter(mm(1.8f));
        doc.add(label);

        PdfPTable t = new PdfPTable(new float[]{1f, 1f, 1f, 1f});
        t.setWidthPercentage(100);
        t.setSpacingAfter(mm(2));
        hourCard(t, "HEURES DUES (MOIS)", hoursLabel(hours.dueMinutes()), WHITE, BORDER, LABEL_GREY, DARK_TEXT);
        hourCard(t, "HEURES RÉALISÉES", hoursLabel(hours.realizedMinutes()), WHITE, BORDER, LABEL_GREY, DARK_TEXT);
        hourCard(t, "HEURES NON RÉALISÉES", hoursLabel(hours.missingMinutes()), ALERT_BG, ALERT_BORDER, ALERT_TEXT, ALERT_TEXT);
        hourCard(t, "HEURES EFFECTIVES", hoursLabel(hours.effectiveMinutes()), accent, accent, WHITE, WHITE);
        doc.add(t);

        int due = Math.max(hours.dueMinutes(), 1);
        float doneFraction = Math.max(0.001f, Math.min(1f, hours.realizedMinutes() / (float) due));
        float missingFraction = Math.max(0.001f, 1f - doneFraction);
        // Les deux segments remplissent toute la largeur (pas de piste visible en dessous, fidèle à la
        // référence où les deux blocs totalisent déjà 100 %) — un seul niveau d'imbrication table/cellule
        // pour éviter un bug d'affichage observé avec plusieurs niveaux de PdfPCell contenant chacun sa
        // propre PdfPTable à coins arrondis (le remplissage de couleur n'apparaissait plus du tout).
        PdfPTable bar = new PdfPTable(new float[]{doneFraction, missingFraction});
        bar.setWidthPercentage(100);
        PdfPCell done = new PdfPCell();
        done.setBorder(0);
        done.setFixedHeight(mm(2.2f));
        done.setCellEvent(RoundedCellBorder.fillOnly(mm(1.1f), accent));
        bar.addCell(done);
        PdfPCell missing = new PdfPCell();
        missing.setBorder(0);
        missing.setFixedHeight(mm(2.2f));
        missing.setCellEvent(RoundedCellBorder.fillOnly(mm(1.1f), ALERT_BORDER));
        bar.addCell(missing);
        PdfPTable barRow = new PdfPTable(new float[]{4f, 2f});
        barRow.setWidthPercentage(100);
        barRow.setSpacingAfter(mm(4));
        PdfPCell barCell = new PdfPCell();
        barCell.setBorder(0);
        barCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        barCell.addElement(bar);
        barRow.addCell(barCell);
        double pct = hours.dueMinutes() > 0 ? (hours.realizedMinutes() * 100.0 / hours.dueMinutes()) : 0;
        PdfPCell caption = new PdfPCell(new Phrase(
                hoursLabel(hours.realizedMinutes()) + " réalisées sur " + hoursLabel(hours.dueMinutes()) + " dues · "
                        + new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(Locale.FRANCE)).format(pct) + " %",
                font(SANS, FontFactory.HELVETICA, 7.5f, TEXT_GREY)));
        caption.setBorder(0);
        caption.setVerticalAlignment(Element.ALIGN_MIDDLE);
        caption.setHorizontalAlignment(Element.ALIGN_RIGHT);
        barRow.addCell(caption);
        doc.add(barRow);
    }

    private static void hourCard(PdfPTable t, String label, String value, Color bg, Color border, Color labelColor, Color valueColor) {
        PdfPCell c = new PdfPCell();
        c.setBorder(0);
        c.setPadding(0);
        c.setCellEvent(RoundedCellBorder.fillAndBorder(mm(3), bg, border, 0.8f));
        PdfPTable inner = new PdfPTable(1);
        inner.setWidthPercentage(100);
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 6.5f, labelColor)));
        labelCell.setBorder(0);
        labelCell.setPadding(mm(3));
        labelCell.setPaddingBottom(mm(1));
        inner.addCell(labelCell);
        PdfPCell valueCell = new PdfPCell(new Phrase(value, font(MONO_BOLD, FontFactory.COURIER_BOLD, 14, valueColor)));
        valueCell.setBorder(0);
        valueCell.setPadding(mm(3));
        valueCell.setPaddingTop(0);
        inner.addCell(valueCell);
        c.addElement(inner);
        t.addCell(c);
    }

    // ── Tableau de rémunération (coins arrondis) ───────────────────────────────────
    private static void remunerationTable(Document doc, Color accent, Payslip p, List<PayslipLine> lines, boolean hourly, TeacherHours hours) {
        Paragraph label = new Paragraph("DÉTAIL DE LA RÉMUNÉRATION", font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 7, accent));
        label.setSpacingAfter(mm(1.8f));
        doc.add(label);

        PdfPTable table = new PdfPTable(new float[]{2.4f, 1f});
        table.setWidthPercentage(100);
        PdfPCell h1 = new PdfPCell(new Phrase("Élément", font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 9, WHITE)));
        h1.setBackgroundColor(accent);
        h1.setBorderColor(accent);
        h1.setPadding(mm(2.6f));
        table.addCell(h1);
        PdfPCell h2 = new PdfPCell(new Phrase("Montant", font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 9, WHITE)));
        h2.setBackgroundColor(accent);
        h2.setBorderColor(accent);
        h2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        h2.setPadding(mm(2.6f));
        table.addCell(h2);

        remRow(table, "Salaire de base", baseNote(hourly, hours), money(p.getBaseSalary()) + " GNF", WHITE, false);
        for (PayslipLine l : lines) {
            if (l.getLineType() == PayslipLineType.EARNING) {
                remRow(table, l.getLabel(), null, "+ " + money(l.getAmount()) + " GNF", WHITE, false);
            }
        }
        remRow(table, "Salaire brut", null, money(p.getGrossSalary()) + " GNF", TOTAL_BG, true);
        for (PayslipLine l : lines) {
            if (l.getLineType() == PayslipLineType.DEDUCTION) {
                remRow(table, l.getLabel(), null, "− " + money(l.getAmount()) + " GNF", WHITE, false);
            }
        }

        roundedTable(doc, table, mm(4));
    }

    private static String baseNote(boolean hourly, TeacherHours hours) {
        if (hourly && hours != null) return "Sur la base de " + hoursLabel(hours.effectiveMinutes()) + " effectives";
        return "Salaire mensuel forfaitaire";
    }

    private static void remRow(PdfPTable table, String label, String note, String amount, Color bg, boolean bold) {
        PdfPCell l = new PdfPCell();
        l.setBackgroundColor(bg);
        l.setBorderColor(BORDER_LIGHT);
        l.setPadding(mm(2.8f));
        l.addElement(new Phrase(label, font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 9.5f, DARK_TEXT)));
        if (note != null && !note.isBlank()) {
            Paragraph n = new Paragraph(note, font(SANS, FontFactory.HELVETICA, 7.5f, TEXT_GREY));
            n.setSpacingBefore(0.8f);
            l.addElement(n);
        }
        table.addCell(l);
        PdfPCell v = new PdfPCell(new Phrase(amount, font(bold ? MONO_BOLD : MONO, bold ? FontFactory.COURIER_BOLD : FontFactory.COURIER, 9.5f, DARK_TEXT)));
        v.setBackgroundColor(bg);
        v.setBorderColor(BORDER_LIGHT);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        v.setPadding(mm(2.8f));
        table.addCell(v);
    }

    /** Enveloppe une table à bords droits dans une cellule à coins arrondis — OpenPDF ne permet pas de
     * coins arrondis directement sur une {@link PdfPTable} (même technique que le bulletin). */
    private static void roundedTable(Document doc, PdfPTable inner, float spacingAfter) {
        PdfPCell outer = new PdfPCell();
        outer.setBorder(0);
        outer.setPadding(0);
        outer.setCellEvent(RoundedCellBorder.borderOnly(mm(3), BORDER, 0.8f));
        outer.addElement(inner);
        PdfPTable wrapper = new PdfPTable(1);
        wrapper.setWidthPercentage(100);
        wrapper.setSpacingAfter(spacingAfter);
        wrapper.addCell(outer);
        doc.add(wrapper);
    }

    // ── NET À PAYER : bloc plein, couleur de marque ────────────────────────────────
    private static void netBox(Document doc, Color accent, Payslip p) {
        Phrase amountPhrase = new Phrase();
        amountPhrase.add(new com.lowagie.text.Chunk(money(p.getNetSalary()), font(SANS_EXTRABOLD, FontFactory.HELVETICA_BOLD, 22, WHITE)));
        amountPhrase.add(new com.lowagie.text.Chunk(" GNF", font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 10, new Color(233, 215, 158))));

        // Un seul bloc continu (pas deux cellules à fonds séparés) : table 2 colonnes partageant le
        // même fond de couleur de marque, chacune avec son padding propre.
        PdfPTable inner = new PdfPTable(new float[]{1f, 1f});
        inner.setWidthPercentage(100);
        PdfPCell left = new PdfPCell();
        left.setBorder(0);
        left.setVerticalAlignment(Element.ALIGN_MIDDLE);
        left.setPadding(mm(4.5f));
        left.addElement(new Paragraph("NET À PAYER", font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 7, new Color(233, 215, 158))));
        Paragraph words = new Paragraph(safe(amountInWords(p.getNetSalary()), ""), font(SANS, FontFactory.HELVETICA, 8, tintLight(accent)));
        words.setSpacingBefore(0.8f);
        left.addElement(words);
        inner.addCell(left);
        PdfPCell right = new PdfPCell(new Paragraph(amountPhrase));
        right.setBorder(0);
        right.setVerticalAlignment(Element.ALIGN_MIDDLE);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        right.setPadding(mm(4.5f));
        inner.addCell(right);

        PdfPCell outer = new PdfPCell();
        outer.setBorder(0);
        outer.setPadding(0);
        outer.setCellEvent(RoundedCellBorder.fillOnly(mm(3), accent));
        outer.addElement(inner);
        PdfPTable wrapper = new PdfPTable(1);
        wrapper.setWidthPercentage(100);
        wrapper.setSpacingAfter(mm(6));
        wrapper.addCell(outer);
        doc.add(wrapper);
    }

    private static Color tintLight(Color accent) {
        // Approxime l'opacité ~85% du texte secondaire blanc de la référence sur fond de couleur pleine
        // — mélange simple vers le blanc, lisible quelle que soit la couleur de marque choisie.
        return new Color(
                (int) Math.round(accent.getRed() * 0.35 + 255 * 0.65),
                (int) Math.round(accent.getGreen() * 0.35 + 255 * 0.65),
                (int) Math.round(accent.getBlue() * 0.35 + 255 * 0.65));
    }

    // ── Signatures bénéficiaire / établissement, avec les noms imprimés dessous ───
    private static void signatures(Document doc, Payslip p, Presentation presentation) {
        PdfPTable t = new PdfPTable(new float[]{1f, 1f});
        t.setWidthPercentage(100);
        signatureCell(t, "Signature du bénéficiaire", p.getTeacherName());
        signatureCell(t, "Signature et Cachet de l'Établissement", presentation.accountantName());
        doc.add(t);
    }

    private static void signatureCell(PdfPTable t, String label, String name) {
        PdfPCell c = new PdfPCell();
        c.setBorder(0);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPaddingBottom(mm(2));
        Paragraph l = new Paragraph(label, font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 8.5f, DARK_TEXT));
        l.setAlignment(Element.ALIGN_CENTER);
        c.addElement(l);
        Paragraph spacer = new Paragraph(" ", font(SANS, FontFactory.HELVETICA, 8, WHITE));
        spacer.setSpacingBefore(mm(12));
        c.addElement(spacer);
        PdfPCell lineCell = new PdfPCell();
        lineCell.setBorder(Rectangle.TOP);
        lineCell.setBorderColorTop(SIGNATURE_LINE);
        lineCell.setBorderWidthTop(0.6f);
        lineCell.setFixedHeight(1f);
        PdfPTable lineTable = new PdfPTable(1);
        lineTable.setWidthPercentage(78f);
        lineTable.addCell(lineCell);
        c.addElement(lineTable);
        if (name != null && !name.isBlank()) {
            Paragraph n = new Paragraph(name, font(SANS, FontFactory.HELVETICA, 8, TEXT_GREY));
            n.setAlignment(Element.ALIGN_CENTER);
            n.setSpacingBefore(1.5f);
            c.addElement(n);
        }
        t.addCell(c);
    }

    // ── Utilitaires ─────────────────────────────────────────────────────────────
    private static String hoursLabel(int minutes) {
        return (minutes / 60) + "h" + String.format("%02d", minutes % 60);
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

    private static String money(BigDecimal a) {
        if (a == null) return "0";
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.FRANCE);
        sym.setGroupingSeparator(' ');
        return new DecimalFormat("#,##0", sym).format(a);
    }

    /** Montant en toutes lettres (ex. "Quatre cent mille francs guinéens"), fidèle à la référence —
     * null si le montant est inconnu ou nul (une fiche en brouillon n'a pas encore de net calculé). */
    private static String amountInWords(BigDecimal amount) {
        if (amount == null) return null;
        String words = AmountInWords.convert(amount.longValue());
        if (words == null) return null;
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
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
