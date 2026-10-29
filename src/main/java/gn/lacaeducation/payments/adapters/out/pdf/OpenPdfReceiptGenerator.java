package gn.lacaeducation.payments.adapters.out.pdf;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.common.TenantPublicInfo;
import gn.lacaeducation.common.pdf.DesignFonts;
import gn.lacaeducation.common.pdf.PremiumDocumentHeader;
import gn.lacaeducation.common.pdf.PremiumPageBackground;
import gn.lacaeducation.common.pdf.QrCodeImage;
import gn.lacaeducation.common.pdf.RoundedCellBorder;
import gn.lacaeducation.common.pdf.SchoolPdfHeader;
import gn.lacaeducation.common.pdf.SchoolPdfPageFooter;
import gn.lacaeducation.payments.PayerDirectory;
import gn.lacaeducation.payments.application.port.out.PaymentRepository;
import gn.lacaeducation.payments.application.port.out.ReceiptPdfGenerator;
import gn.lacaeducation.payments.domain.Payment;
import gn.lacaeducation.payments.domain.Receipt;
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
 * Reçu de paiement officiel — reproduit fidèlement le design premium de référence (§retour fondateur,
 * /features/document-school/Reçu de paiement.dc.html, demande utilisateur) : fond crème, liseré
 * tricolore guinéen, en-tête logo/école/République, badge N° reçu, carte élève + carte date/mode de
 * paiement avec QR code de vérification, tableau des factures à coins arrondis, bloc de totaux (Montant
 * réglé/Total payé/Reste à payer), cachet "PAYÉ" façon sceau de cire, signature du comptable avec son
 * nom imprimé. Couleur d'accent personnalisable (couleur de marque du tenant), comme la fiche de paie —
 * voir {@code OpenPdfPayslipGenerator} pour le même principe et {@code PremiumDocumentHeader}/
 * {@code RoundedCellBorder}/{@code PremiumPageBackground} pour l'infrastructure partagée.
 *
 * <p>Toute la logique métier (fratrie/paiement groupé, remises, crédits reportés, surplus à rembourser,
 * frais d'usage plateforme, cumul "Total payé" distinct de "Montant réglé"...) est reprise à l'identique
 * de l'ancienne version sobre de ce générateur — seule la mise en page change.</p>
 */
@Component
@RequiredArgsConstructor
class OpenPdfReceiptGenerator implements ReceiptPdfGenerator {

    // ── Palette neutre (fixe) ──────────────────────────────────────────────────
    private static final Color GOLD = new Color(184, 137, 43);
    private static final Color BORDER = new Color(217, 210, 192);
    private static final Color BORDER_LIGHT = new Color(234, 228, 214);
    private static final Color LABEL_GREY = new Color(138, 122, 85);
    private static final Color TEXT_GREY = new Color(94, 107, 101);
    private static final Color DARK_TEXT = new Color(19, 38, 31);
    private static final Color WHITE = Color.WHITE;
    private static final Color ROW_STRIPE = new Color(252, 250, 244);
    private static final Color PAID_BG = new Color(227, 242, 233);
    private static final Color PAID_TEXT = new Color(0, 122, 79);
    private static final Color UNPAID_BG = new Color(248, 225, 223);
    private static final Color UNPAID_TEXT = new Color(179, 38, 30);
    private static final Color REMAINING_BG = new Color(251, 239, 237);
    private static final Color REMAINING_TEXT = new Color(179, 38, 30);
    private static final Color SIGNATURE_LINE = new Color(184, 176, 156);

    private static final com.lowagie.text.pdf.BaseFont SANS = DesignFonts.SANS;
    private static final com.lowagie.text.pdf.BaseFont SANS_BOLD = DesignFonts.SANS_SEMIBOLD;
    private static final com.lowagie.text.pdf.BaseFont SERIF_BOLD = DesignFonts.SERIF_BOLD;
    private static final com.lowagie.text.pdf.BaseFont MONO = DesignFonts.MONO;
    private static final com.lowagie.text.pdf.BaseFont MONO_BOLD = DesignFonts.MONO_SEMIBOLD;

    private final TenantInfoProvider tenantInfoProvider;
    private final PayerDirectory payerDirectory;
    private final ObjectMapper objectMapper;
    private final PaymentRepository paymentRepository;

    /** Une part d'un paiement groupé (miroir minimal de {@code GroupedInvoiceSettlementEvent.Share}). */
    private record GroupLine(Long invoiceId, Long studentId, BigDecimal amount) {}

    @Override
    public byte[] generate(Receipt receipt, Payment payment) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, mm(15), mm(12), mm(10), mm(16));
        try {
            TenantPublicInfo info = payment.getTenantId() == null ? null
                    : tenantInfoProvider.findTenantInfo(payment.getTenantId()).orElse(null);
            Color accent = SchoolPdfHeader.parseColor(info == null ? null : info.brandColor());

            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new PremiumPageBackground());
            writer.setPageEvent(new SchoolPdfPageFooter(info, accent));
            doc.open();

            String academicYearLabel = payerDirectory.currentAcademicYearLabel(payment.getTenantId()).orElse(null);

            PremiumDocumentHeader.render(doc, info);
            titleRow(doc, accent, academicYearLabel, receipt.getReference());

            java.util.LinkedHashSet<Long> studentIds = new java.util.LinkedHashSet<>();
            for (GroupLine line : parseGroupLines(payment.getGroupLines())) {
                if (line.studentId() != null) studentIds.add(line.studentId());
            }
            if (studentIds.isEmpty() && payment.getStudentId() != null) studentIds.add(payment.getStudentId());
            boolean single = studentIds.size() <= 1;
            Long soloStudentId = single ? (studentIds.isEmpty() ? payment.getStudentId() : studentIds.iterator().next()) : null;

            String qrContent = receipt.getReference()
                    + "|" + (single ? safe(payerDirectory.studentNumber(soloStudentId).orElse(null)) : "GROUPE")
                    + "|" + money(receipt.getAmount()) + " GNF"
                    + "|" + (payment.getPaidAt() != null ? payment.getPaidAt().toLocalDate().format(DATE) : "-");

            if (single) {
                studentAndPaymentRow(doc, accent, soloStudentId, receipt, payment, qrContent);
            } else {
                paymentInfoCard(doc, accent, receipt, payment, qrContent, true);
            }

            List<String> creditNotes = new java.util.ArrayList<>();
            List<String> discountNotes = new java.util.ArrayList<>();
            List<String> refundNotes = new java.util.ArrayList<>();

            if (single) {
                List<PayerDirectory.InvoiceRowInfo> invoices = payerDirectory.studentInvoices(soloStudentId);
                doc.add(invoicesTable(invoices, paidAtByInvoiceId(soloStudentId)));
                creditNotes.addAll(creditNotes(invoices));
                discountNotes.addAll(discountNotes(invoices));
                refundNotes.addAll(refundNotes(soloStudentId));
                totalsBox(doc, accent, receipt, sumPaidToDate(invoices), totalRemaining(soloStudentId));
            } else {
                BigDecimal totalPaidToDate = BigDecimal.ZERO;
                BigDecimal totalRemainingAll = BigDecimal.ZERO;
                for (Long studentId : studentIds) {
                    studentCard(doc, payerDirectory.studentName(studentId).orElse(null), studentId);
                    List<PayerDirectory.InvoiceRowInfo> invoices = payerDirectory.studentInvoices(studentId);
                    doc.add(invoicesTable(invoices, paidAtByInvoiceId(studentId)));
                    creditNotes.addAll(creditNotes(invoices));
                    discountNotes.addAll(discountNotes(invoices));
                    refundNotes.addAll(refundNotes(studentId));
                    totalPaidToDate = totalPaidToDate.add(sumPaidToDate(invoices));
                    totalRemainingAll = totalRemainingAll.add(totalRemaining(studentId));
                }
                totalsBox(doc, accent, receipt, totalPaidToDate, totalRemainingAll);
            }

            for (String line : creditNotes) noteLine(doc, line, TEXT_GREY, false);
            for (String line : discountNotes) noteLine(doc, line, TEXT_GREY, false);
            for (String line : refundNotes) noteLine(doc, line, REMAINING_TEXT, true);
            String platformFeeNote = platformFeeNote(payment);
            if (platformFeeNote != null) noteLine(doc, platformFeeNote, TEXT_GREY, false);

            stampAndSignature(doc, writer, accent, academicYearLabel, receipt, payment.getCollectedByUserId());

            doc.close();
            return out.toByteArray();
        } catch (RuntimeException e) {
            if (doc.isOpen()) doc.close();
            throw new IllegalStateException("Échec de génération du PDF du reçu " + receipt.getReference(), e);
        }
    }

    // ── Eyebrow (année scolaire) + titre + badge N° reçu, filet or en bas ─────────
    private static void titleRow(Document doc, Color accent, String academicYearLabel, String reference) {
        PdfPTable t = new PdfPTable(new float[]{3f, 1.3f});
        t.setWidthPercentage(100);
        t.setSpacingAfter(mm(2.2f));

        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.BOTTOM);
        left.setBorderColorBottom(GOLD);
        left.setBorderWidthBottom(0.8f);
        left.setVerticalAlignment(Element.ALIGN_BOTTOM);
        left.setPaddingBottom(mm(2.5f));
        String eyebrow = academicYearLabel != null && !academicYearLabel.isBlank()
                ? "ANNÉE SCOLAIRE " + academicYearLabel.toUpperCase(Locale.FRENCH) : " ";
        left.addElement(new Paragraph(eyebrow, font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 7, LABEL_GREY)));
        Paragraph title = new Paragraph("Reçu de paiement officiel", font(SERIF_BOLD, FontFactory.TIMES_BOLD, 21, accent));
        title.setSpacingBefore(1f);
        left.addElement(title);
        t.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.BOTTOM);
        right.setBorderColorBottom(GOLD);
        right.setBorderWidthBottom(0.8f);
        right.setVerticalAlignment(Element.ALIGN_BOTTOM);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        right.setPaddingBottom(mm(2.2f));
        right.addElement(new Paragraph("N° REÇU", font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 6.5f, LABEL_GREY)));
        PdfPCell badge = new PdfPCell(new Phrase(safe(reference, "-"), font(MONO_BOLD, FontFactory.COURIER_BOLD, 9.5f, WHITE)));
        badge.setBorder(0);
        badge.setBackgroundColor(accent);
        badge.setPadding(mm(1.6f));
        badge.setPaddingLeft(mm(3));
        badge.setPaddingRight(mm(3));
        badge.setHorizontalAlignment(Element.ALIGN_CENTER);
        badge.setCellEvent(RoundedCellBorder.fillOnly(mm(1.8f), accent));
        PdfPTable badgeWrap = new PdfPTable(1);
        badgeWrap.setWidthPercentage(100);
        badgeWrap.setSpacingBefore(1f);
        badgeWrap.addCell(badge);
        right.addElement(badgeWrap);
        t.addCell(right);

        doc.add(t);
    }

    // ── Carte élève (gauche) + carte date/mode de paiement + QR (droite) ─────────
    private void studentAndPaymentRow(Document doc, Color accent, Long studentId, Receipt receipt, Payment payment, String qrContent) {
        PdfPTable t = new PdfPTable(new float[]{1f, 1f});
        t.setWidthPercentage(100);
        t.setSpacingAfter(mm(2.6f));
        // Un écart visible entre les deux cartes (padding sur une cellule ENVELOPPANTE, pas sur la
        // cellule à fond arrondi elle-même — son fond remplit tout son rectangle de cellule quel que
        // soit son padding, donc un padding posé directement dessus ne créerait aucun écart visuel).
        t.addCell(gapWrap(studentCardCell(receipt.getStudentName(), studentId), 0, mm(2.5f)));
        t.addCell(gapWrap(paymentInfoCell(payment, qrContent), mm(2.5f), 0));
        doc.add(t);
    }

    private static PdfPCell gapWrap(PdfPCell inner, float paddingLeft, float paddingRight) {
        PdfPCell c = new PdfPCell();
        c.setBorder(0);
        c.setPadding(0);
        c.setPaddingLeft(paddingLeft);
        c.setPaddingRight(paddingRight);
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.addCell(inner);
        c.addElement(t);
        return c;
    }

    private void studentCard(Document doc, String studentName, Long studentId) {
        PdfPTable wrapper = new PdfPTable(1);
        wrapper.setWidthPercentage(100);
        wrapper.setSpacingAfter(mm(2));
        wrapper.addCell(studentCardCell(studentName, studentId));
        doc.add(wrapper);
    }

    private PdfPCell studentCardCell(String studentName, Long studentId) {
        PdfPTable inner = new PdfPTable(1);
        inner.setWidthPercentage(100);
        PdfPCell eyebrow = new PdfPCell(new Phrase("ÉLÈVE", font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 6.5f, LABEL_GREY)));
        eyebrow.setBorder(0);
        eyebrow.setPadding(mm(2.4f));
        eyebrow.setPaddingBottom(mm(1));
        inner.addCell(eyebrow);
        PdfPCell name = new PdfPCell(new Phrase(safe(studentName, "-"), font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 13, DARK_TEXT)));
        name.setBorder(0);
        name.setPadding(mm(2.4f));
        name.setPaddingTop(0);
        name.setPaddingBottom(mm(1.5f));
        inner.addCell(name);
        PdfPTable fields = new PdfPTable(new float[]{1f, 1f});
        fields.setWidthPercentage(100);
        smallField(fields, "MATRICULE", safe(payerDirectory.studentNumber(studentId).orElse(null), "-"), true);
        smallField(fields, "CLASSE", safe(payerDirectory.className(studentId).orElse(null), "-"), false);
        PdfPCell fieldsCell = new PdfPCell();
        fieldsCell.setBorder(0);
        fieldsCell.setPadding(mm(2.4f));
        fieldsCell.setPaddingTop(0);
        fieldsCell.addElement(fields);
        inner.addCell(fieldsCell);

        PdfPCell outer = new PdfPCell();
        outer.setBorder(0);
        outer.setPadding(0);
        outer.setCellEvent(RoundedCellBorder.fillAndBorder(mm(3), WHITE, BORDER, 0.8f));
        outer.addElement(inner);
        return outer;
    }

    private void smallField(PdfPTable t, String label, String value, boolean mono) {
        PdfPCell c = new PdfPCell();
        c.setBorder(0);
        c.addElement(new Paragraph(label, font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 6.5f, LABEL_GREY)));
        Paragraph v = new Paragraph(value, mono
                ? font(MONO_BOLD, FontFactory.COURIER_BOLD, 9.5f, DARK_TEXT)
                : font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 9.5f, DARK_TEXT));
        v.setSpacingBefore(0.8f);
        c.addElement(v);
        t.addCell(c);
    }

    private void paymentInfoCard(Document doc, Color accent, Receipt receipt, Payment payment, String qrContent, boolean fullWidth) {
        PdfPTable wrapper = new PdfPTable(1);
        wrapper.setWidthPercentage(fullWidth ? 50f : 100f);
        if (fullWidth) wrapper.setHorizontalAlignment(Element.ALIGN_RIGHT);
        wrapper.setSpacingAfter(mm(2.2f));
        wrapper.addCell(paymentInfoCell(payment, qrContent));
        doc.add(wrapper);
    }

    private PdfPCell paymentInfoCell(Payment payment, String qrContent) {
        PdfPTable row = new PdfPTable(new float[]{1.6f, 1f});
        row.setWidthPercentage(100);
        PdfPCell fieldsCell = new PdfPCell();
        fieldsCell.setBorder(0);
        fieldsCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        fieldsCell.addElement(fieldStack("DATE DE PAIEMENT", payment.getPaidAt() != null
                ? payment.getPaidAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH'h'mm")) : "-"));
        Paragraph mode = fieldStack("MODE DE PAIEMENT", channelLabel(payment));
        mode.setSpacingBefore(mm(2.2f));
        fieldsCell.addElement(mode);
        row.addCell(fieldsCell);

        PdfPCell qrCell = new PdfPCell();
        qrCell.setBorder(0);
        qrCell.setFixedHeight(mm(20));
        qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        qrCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        qrCell.setCellEvent(RoundedCellBorder.fillAndBorder(mm(2), WHITE, new Color(234, 228, 214), 0.8f));
        Image qr = QrCodeImage.render(qrContent, 240);
        if (qr != null) {
            qr.scaleToFit(mm(17), mm(17));
            qr.setAlignment(Element.ALIGN_MIDDLE);
            qrCell.addElement(qr);
        }
        row.addCell(qrCell);

        PdfPCell outer = new PdfPCell();
        outer.setBorder(0);
        outer.setPadding(0);
        outer.setCellEvent(RoundedCellBorder.fillAndBorder(mm(3), WHITE, BORDER, 0.8f));
        outer.addElement(padded(row, mm(2.4f)));
        return outer;
    }

    private static Paragraph fieldStack(String label, String value) {
        Paragraph p = new Paragraph(label, font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 6.5f, LABEL_GREY));
        Phrase v = new Phrase("\n" + value, font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 9.5f, DARK_TEXT));
        p.add(v);
        return p;
    }

    /** Enveloppe une table dans une cellule de padding donné, un seul niveau d'imbrication (voir le
     * même avertissement dans OpenPdfPayslipGenerator sur les cellules à fond arrondi imbriquées). */
    private static PdfPTable padded(PdfPTable inner, float pad) {
        PdfPCell c = new PdfPCell();
        c.setBorder(0);
        c.setPadding(pad);
        c.addElement(inner);
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.addCell(c);
        return t;
    }

    // ── Tableau des factures (coins arrondis) : Réf. / Observation / Montant / Date & heure / Payé / Reste ──
    private PdfPTable invoicesTable(List<PayerDirectory.InvoiceRowInfo> invoices,
                                     java.util.Map<Long, java.time.LocalDateTime> paidAtByInvoiceId) {
        // Lignes confortables par défaut (demande utilisateur : "trop condensé") — ne se resserrent
        // (padding ET taille de police) que si le nombre de factures menace réellement le budget d'une
        // page (reçu avec plusieurs années de mensualités + frais annexes, §retour comptable,
        // contrainte historique "jamais de 2e page"). La grande majorité des reçus (quelques factures)
        // gardent donc des lignes larges. La police compte presque autant que le padding ici : la
        // colonne "Observation" passe souvent sur 2 lignes (libellés longs type "Mensualité — Tranche
        // 2 (décembre, janvier, février)"), où même +0.4pt se répercute sur CHAQUE ligne du libellé.
        boolean tight = invoices.size() > 18;
        boolean medium = !tight && invoices.size() > 10;
        float pad = tight ? 1.5f : medium ? 2.2f : 3.2f;
        float bodyFont = tight ? 7.6f : medium ? 7.9f : 8.3f;
        float smallFont = tight ? 7f : medium ? 7.1f : 7.3f;

        PdfPTable table = new PdfPTable(new float[]{1f, 1.8f, 0.9f, 1f, 0.6f, 0.9f});
        table.setWidthPercentage(100);
        headerCell(table, "Réf.", Element.ALIGN_LEFT, pad);
        headerCell(table, "Observation", Element.ALIGN_LEFT, pad);
        headerCell(table, "Montant", Element.ALIGN_RIGHT, pad);
        headerCell(table, "Date & heure", Element.ALIGN_CENTER, pad);
        headerCell(table, "Payé", Element.ALIGN_CENTER, pad);
        headerCell(table, "Reste", Element.ALIGN_RIGHT, pad);

        DateTimeFormatter paidAtFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH'h'mm");
        boolean zebra = false;
        for (PayerDirectory.InvoiceRowInfo inv : invoices) {
            Color bg = zebra ? ROW_STRIPE : WHITE;
            zebra = !zebra;
            bodyCellMono(table, inv.reference(), Element.ALIGN_LEFT, bg, smallFont, TEXT_GREY, pad);
            bodyCell(table, inv.label(), Element.ALIGN_LEFT, bg, pad, bodyFont);
            bodyCellMono(table, money(inv.amount()) + " GNF", Element.ALIGN_RIGHT, bg, bodyFont, DARK_TEXT, pad);
            java.time.LocalDateTime paidAt = paidAtByInvoiceId.get(inv.id());
            java.time.LocalDateTime shown = paidAt != null ? paidAt : inv.creditedAt();
            PdfPCell dateCell = new PdfPCell(new Phrase(shown != null ? shown.format(paidAtFormat) : "-",
                    font(SANS, FontFactory.HELVETICA, smallFont, TEXT_GREY)));
            dateCell.setBackgroundColor(bg);
            dateCell.setBorderColor(BORDER_LIGHT);
            dateCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            dateCell.setPadding(pad);
            table.addCell(dateCell);
            table.addCell(paidPill(inv.paid(), bg, pad));
            remainingCell(table, inv.remainingBalance(), bg, pad, bodyFont);
        }
        if (invoices.isEmpty()) {
            PdfPCell empty = new PdfPCell(new Phrase("Aucune facture.", font(SANS, FontFactory.HELVETICA_OBLIQUE, 9, TEXT_GREY)));
            empty.setColspan(6);
            empty.setHorizontalAlignment(Element.ALIGN_CENTER);
            empty.setPadding(mm(5));
            empty.setBorderColor(BORDER_LIGHT);
            table.addCell(empty);
        }

        PdfPTable wrapper = new PdfPTable(1);
        wrapper.setWidthPercentage(100);
        wrapper.setSpacingAfter(mm(2.2f));
        PdfPCell outer = new PdfPCell();
        outer.setBorder(0);
        outer.setPadding(0);
        outer.setCellEvent(RoundedCellBorder.borderOnly(mm(3), BORDER, 0.8f));
        outer.addElement(table);
        wrapper.addCell(outer);
        return wrapper;
    }

    private static void headerCell(PdfPTable t, String label, int align, float pad) {
        PdfPCell c = new PdfPCell(new Phrase(label, font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 8, WHITE)));
        c.setBackgroundColor(DARK_TEXT);
        c.setBorderColor(DARK_TEXT);
        c.setHorizontalAlignment(align);
        c.setPadding(pad);
        t.addCell(c);
    }

    private static void bodyCell(PdfPTable t, String value, int align, Color bg, float pad, float size) {
        PdfPCell c = new PdfPCell(new Phrase(value == null ? "-" : value, font(SANS_BOLD, FontFactory.HELVETICA_BOLD, size, DARK_TEXT)));
        c.setBackgroundColor(bg);
        c.setBorderColor(BORDER_LIGHT);
        c.setHorizontalAlignment(align);
        c.setPadding(pad);
        t.addCell(c);
    }

    private static void bodyCellMono(PdfPTable t, String value, int align, Color bg, float size, Color color, float pad) {
        PdfPCell c = new PdfPCell(new Phrase(value == null ? "-" : value, font(MONO, FontFactory.COURIER, size, color)));
        c.setBackgroundColor(bg);
        c.setBorderColor(BORDER_LIGHT);
        c.setHorizontalAlignment(align);
        c.setPadding(pad);
        t.addCell(c);
    }

    /** Pastille OUI (vert)/NON (rouge) — même teintes que le tableau de bord (statut de règlement). */
    private static PdfPCell paidPill(boolean paid, Color rowBg, float pad) {
        Color bg = paid ? PAID_BG : UNPAID_BG;
        Color text = paid ? PAID_TEXT : UNPAID_TEXT;
        PdfPCell pill = new PdfPCell(new Phrase(paid ? "OUI" : "NON", font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 6.8f, text)));
        pill.setCellEvent(RoundedCellBorder.fillOnly(mm(1.6f), bg));
        pill.setHorizontalAlignment(Element.ALIGN_CENTER);
        pill.setPadding(pad);
        pill.setBackgroundColor(rowBg);
        pill.setBorderColor(BORDER_LIGHT);
        return pill;
    }

    private static void remainingCell(PdfPTable t, BigDecimal remaining, Color bg, float pad, float size) {
        boolean due = remaining != null && remaining.signum() > 0;
        PdfPCell c = new PdfPCell(new Phrase(due ? money(remaining) + " GNF" : "-",
                font(due ? MONO_BOLD : MONO, due ? FontFactory.COURIER_BOLD : FontFactory.COURIER, size,
                        due ? REMAINING_TEXT : TEXT_GREY)));
        c.setBackgroundColor(bg);
        c.setBorderColor(BORDER_LIGHT);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c.setPadding(pad);
        t.addCell(c);
    }

    // ── Bloc de totaux : Montant réglé / TOTAL PAYÉ / RESTE À PAYER ────────────────
    private void totalsBox(Document doc, Color accent, Receipt receipt, BigDecimal totalPaidToDate, BigDecimal remaining) {
        // Table à 2 colonnes "à plat" (pas un 1-colonne où chaque ligne serait elle-même une table
        // imbriquée) : au-delà d'un niveau d'imbrication table-dans-cellule sous une cellule à fond
        // arrondi, le contenu de la table la plus profonde cesse de s'afficher (même bug observé et
        // contourné dans OpenPdfPayslipGenerator — voir son commentaire sur statusPill/hoursBlock).
        PdfPTable inner = new PdfPTable(new float[]{1f, 1f});
        inner.setWidthPercentage(100);
        totalsRow(inner, "Montant réglé", money(receipt.getAmount()) + " GNF", WHITE, TEXT_GREY, DARK_TEXT, false, false);
        totalsRow(inner, "TOTAL PAYÉ", money(totalPaidToDate) + " GNF", accent, new Color(233, 215, 158), WHITE, true, false);
        if (remaining.signum() > 0) {
            totalsRow(inner, "RESTE À PAYER", money(remaining) + " GNF", REMAINING_BG, REMAINING_TEXT, REMAINING_TEXT, true, true);
        }

        PdfPCell outer = new PdfPCell();
        outer.setBorder(0);
        outer.setPadding(0);
        // borderOnly (pas fillAndBorder) : chaque ligne porte déjà son propre fond (voir totalsRow) —
        // un fond posé EN PLUS sur cette cellule conteneur fait disparaître le fond ET le texte d'une
        // des lignes imbriquées (bug OpenPDF observé empiriquement, même motif que invoicesTable qui,
        // lui, fonctionne correctement avec cette même technique borderOnly).
        outer.setCellEvent(RoundedCellBorder.borderOnly(mm(3), BORDER, 0.8f));
        outer.addElement(inner);
        PdfPTable wrapper = new PdfPTable(1);
        wrapper.setWidthPercentage(60f);
        wrapper.setHorizontalAlignment(Element.ALIGN_RIGHT);
        wrapper.setSpacingAfter(mm(2.2f));
        wrapper.addCell(outer);
        doc.add(wrapper);
    }

    private static void totalsRow(PdfPTable t, String label, String amount, Color bg, Color labelColor, Color amountColor,
                                   boolean emphasis, boolean topBorder) {
        // Border TOUJOURS posée (jamais Rectangle.NO_BORDER/0) — une cellule totalement sans bordure,
        // imbriquée sous la cellule à fond arrondi du bloc de totaux, perd son fond ET son texte à
        // l'affichage (bug OpenPDF observé empiriquement) ; une bordure de la même couleur que le fond
        // reste invisible tout en évitant ce bug.
        Color invisibleBorder = (!topBorder && emphasis) ? bg : BORDER_LIGHT;
        PdfPCell l = new PdfPCell(new Phrase(label, font(SANS_BOLD, FontFactory.HELVETICA_BOLD, emphasis ? 7.5f : 9, labelColor)));
        l.setBackgroundColor(bg);
        l.setBorder(topBorder ? Rectangle.TOP : Rectangle.BOTTOM);
        l.setBorderColor(invisibleBorder);
        l.setVerticalAlignment(Element.ALIGN_MIDDLE);
        l.setPadding(emphasis ? mm(2.0f) : mm(1.5f));
        PdfPCell v = new PdfPCell(new Phrase(amount, font(MONO_BOLD, FontFactory.COURIER_BOLD, emphasis ? 14 : 10, amountColor)));
        v.setBackgroundColor(bg);
        v.setBorder(topBorder ? Rectangle.TOP : Rectangle.BOTTOM);
        v.setBorderColor(invisibleBorder);
        v.setVerticalAlignment(Element.ALIGN_MIDDLE);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        v.setPadding(emphasis ? mm(2.0f) : mm(1.5f));
        t.addCell(l);
        t.addCell(v);
    }

    private void noteLine(Document doc, String text, Color color, boolean bold) {
        Paragraph p = new Paragraph(text, font(bold ? SANS_BOLD : SANS, bold ? FontFactory.HELVETICA_BOLD : FontFactory.HELVETICA, 8.5f, color));
        p.setSpacingAfter(mm(1.1f));
        doc.add(p);
    }

    /** Total encore dû par l'élève, toutes factures confondues (même portée que
     * {@code PayerDirectory#unpaidInvoices}) — affiché dans "RESTE À PAYER" du bloc de totaux. */
    private BigDecimal totalRemaining(Long studentId) {
        return payerDirectory.unpaidInvoices(studentId).stream()
                .map(PayerDirectory.UnpaidInvoiceInfo::remaining)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Cumul de tout ce qui a été réellement encaissé à ce jour (voir Javadoc équivalente de l'ancienne
     * version : jamais {@code amount}, toujours {@code paidAmount} — net après remise). */
    private BigDecimal sumPaidToDate(List<PayerDirectory.InvoiceRowInfo> invoices) {
        return invoices.stream()
                .map(PayerDirectory.InvoiceRowInfo::paidAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private java.util.Map<Long, java.time.LocalDateTime> paidAtByInvoiceId(Long studentId) {
        java.util.Map<Long, java.time.LocalDateTime> result = new java.util.HashMap<>();
        if (studentId == null) return result;
        for (Payment p : paymentRepository.findByStudentId(studentId)) {
            if (p.getStatus() != gn.lacaeducation.payments.domain.PaymentStatus.SUCCEEDED || p.getPaidAt() == null) continue;
            List<Long> invoiceIds = p.getInvoiceId() != null
                    ? List.of(p.getInvoiceId())
                    : parseGroupLines(p.getGroupLines()).stream().map(GroupLine::invoiceId).filter(java.util.Objects::nonNull).toList();
            for (Long invoiceId : invoiceIds) {
                result.merge(invoiceId, p.getPaidAt(), (a, b) -> a.isAfter(b) ? a : b);
            }
        }
        return result;
    }

    private List<String> discountNotes(List<PayerDirectory.InvoiceRowInfo> invoices) {
        java.util.Map<String, BigDecimal> totalByReason = new java.util.LinkedHashMap<>();
        for (PayerDirectory.InvoiceRowInfo inv : invoices) {
            var info = payerDirectory.invoiceFeeInfo(inv.id()).orElse(null);
            if (info == null || info.discountAmount() == null || info.discountAmount().signum() <= 0) continue;
            String reason = info.discountReason() != null && !info.discountReason().isBlank()
                    ? info.discountReason() : "Remise";
            totalByReason.merge(reason, info.discountAmount(), BigDecimal::add);
        }
        return totalByReason.entrySet().stream()
                .map(e -> "Remise appliquée : -" + money(e.getValue()) + " GNF (" + e.getKey() + ")")
                .toList();
    }

    private List<String> creditNotes(List<PayerDirectory.InvoiceRowInfo> invoices) {
        return invoices.stream()
                .filter(i -> i.creditedAmount() != null && i.creditedAmount().signum() > 0)
                .map(i -> i.label() + " : " + money(i.creditedAmount()) + " GNF déjà réglés dans l'ancienne classe"
                        + (i.creditReason() != null && !i.creditReason().isBlank() ? " (" + i.creditReason().toLowerCase() + ")" : "") + ".")
                .toList();
    }

    private List<String> refundNotes(Long studentId) {
        return payerDirectory.pendingRefunds(studentId).stream()
                .map(r -> "À REMBOURSER au parent : " + money(r.amount()) + " GNF déjà réglés en trop"
                        + (r.reason() != null && !r.reason().isBlank() ? " (" + r.reason().toLowerCase() + ")" : "") + ".")
                .toList();
    }

    private String platformFeeNote(Payment payment) {
        List<GroupLine> groupLines = parseGroupLines(payment.getGroupLines());
        List<Long> invoiceIds = !groupLines.isEmpty()
                ? groupLines.stream().map(GroupLine::invoiceId).toList()
                : (payment.getInvoiceId() != null ? List.of(payment.getInvoiceId()) : List.of());
        BigDecimal total = BigDecimal.ZERO;
        for (Long id : invoiceIds) {
            var info = payerDirectory.invoiceFeeInfo(id).orElse(null);
            if (info == null || info.platformFeeAmount() == null || info.platformFeeAmount().signum() <= 0) continue;
            total = total.add(info.platformFeeAmount());
        }
        if (total.signum() <= 0) return null;
        return "Dont frais d'usage plateforme : " + money(total) + " GNF";
    }

    // ── Cachet "PAYÉ" façon sceau de cire (gauche) + signature du comptable (droite) ──
    private void stampAndSignature(Document doc, PdfWriter writer, Color accent, String academicYearLabel,
                                    Receipt receipt, Long collectedByUserId) {
        String operator = receipt.getCollectedByName();
        String phone = payerDirectory.operatorPhone(collectedByUserId).orElse(null);

        float availableBeforeRow = writer.getVerticalPosition(true) - doc.bottom();
        // Bloc sceau+signature COMPACT, hauteur fixe (pas proportionnelle à la place restante) —
        // fidèle à la référence, où le sceau et la signature sont poussés en bas de page ("flex:1" en
        // CSS avant ce bloc), jamais étirés pour occuper tout l'espace restant (laissait un grand vide
        // entre le tableau de totaux et la signature sur un reçu court, §retour fondateur : "le logo
        // de cercle est en bas avec la partie signature" dans l'original).
        float spacerHeight = mm(14f);

        PdfPTable row = new PdfPTable(new float[]{1f, 2f});
        row.setWidthPercentage(100);
        row.setSpacingBefore(2f);

        // Rayon borné par la place réellement disponible pour toute la ligne (mesurée avant de
        // l'ajouter) — jamais une taille fixe : un bloc signature déjà réduit au minimum (voir
        // spacerHeight ci-dessus) aurait sinon un sceau plus haut que lui. En dessous de 7mm, mieux
        // vaut omettre ce flourish décoratif que de le dessiner minuscule/déformé.
        float stampRadius = Math.min(mm(11), availableBeforeRow / 2f - mm(4));
        boolean showStamp = stampRadius >= mm(7);

        PdfPCell stampCell = new PdfPCell();
        stampCell.setBorder(0);
        if (showStamp) {
            stampCell.setFixedHeight(stampRadius * 2);
            // Dessine le sceau une fois la position RÉELLE de cette cellule connue (après pagination) —
            // plus robuste qu'un positionnement manuel par getVerticalPosition() après coup, qui
            // ignorait la hauteur propre de la ligne et faisait déborder le bas du sceau sous la marge
            // dans les cas déjà serrés (reçu avec beaucoup de factures + en-tête complet).
            stampCell.setCellEvent(stampCellEvent(accent, academicYearLabel, stampRadius));
        }
        row.addCell(stampCell);

        PdfPCell sigCell = new PdfPCell();
        sigCell.setBorder(0);
        sigCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph label = new Paragraph("Signature et Cachet du Comptable", font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 8.5f, DARK_TEXT));
        label.setAlignment(Element.ALIGN_CENTER);
        sigCell.addElement(label);
        Paragraph spacer = new Paragraph(" ", font(SANS, FontFactory.HELVETICA, 8, WHITE));
        spacer.setSpacingBefore(spacerHeight);
        sigCell.addElement(spacer);
        PdfPCell lineCell = new PdfPCell();
        lineCell.setBorder(Rectangle.TOP);
        lineCell.setBorderColorTop(SIGNATURE_LINE);
        lineCell.setBorderWidthTop(0.6f);
        lineCell.setFixedHeight(1f);
        PdfPTable lineTable = new PdfPTable(1);
        lineTable.setWidthPercentage(92f);
        lineTable.addCell(lineCell);
        sigCell.addElement(lineTable);
        if (operator != null && !operator.isBlank()) {
            Paragraph name = new Paragraph(operator, font(SERIF_BOLD, FontFactory.TIMES_BOLD, 11, DARK_TEXT));
            name.setAlignment(Element.ALIGN_CENTER);
            name.setSpacingBefore(1.5f);
            sigCell.addElement(name);
        }
        if (phone != null && !phone.isBlank()) {
            Paragraph phoneLine = new Paragraph("Tél : " + phone, font(SANS, FontFactory.HELVETICA, 7.5f, TEXT_GREY));
            phoneLine.setAlignment(Element.ALIGN_CENTER);
            sigCell.addElement(phoneLine);
        }
        row.addCell(sigCell);

        // Pousse le bloc en bas de page via la marge de la TABLE elle-même (spacingBefore), jamais un
        // Paragraphe séparé : un Paragraphe ajoute la hauteur de sa propre ligne de texte en plus du
        // spacingBefore demandé, ce qui faisait déborder la ligne sur une 2e page dans les cas déjà
        // serrés (beaucoup de factures — couverts par OpenPdfReceiptGeneratorTest, "stays on single
        // page"). Hauteur mesurée via getTotalHeight(), mais cette estimation reste approximative :
        // une marge de sécurité de 15mm (pas 1-2mm) absorbe l'écart entre cette estimation et la
        // disposition réelle d'OpenPDF — sans quoi un reçu déjà serré bascule sur une 2e page.
        row.setTotalWidth(doc.right() - doc.left());
        float pushDown = availableBeforeRow - row.getTotalHeight() - mm(15f);
        if (pushDown > 2f) {
            row.setSpacingBefore(pushDown);
        }
        doc.add(row);
    }

    /** Sceau circulaire "PAYÉ" (doré, pointillé, légèrement incliné) — dessiné directement sur le
     * canevas à la position actuelle (sous le tableau de totaux, à gauche du bloc signature) ; aucune
     * exception ne doit faire échouer le reçu si le dessin échoue (document déjà presque terminé). */
    /** Sceau circulaire "PAYÉ" (doré, pointillé, légèrement incliné) dessiné via un
     * {@link com.lowagie.text.pdf.PdfPCellEvent}, attaché à la cellule qui lui réserve sa place dans la
     * ligne signature/sceau — reçoit la position RÉELLE de cette cellule une fois la pagination
     * tranchée par iText (jamais une coordonnée calculée à la main après coup, qui ignorait la hauteur
     * propre de la ligne et faisait déborder le bas du sceau sous la marge dans les cas déjà serrés :
     * reçu avec beaucoup de factures + en-tête complet). Aucune exception ne doit faire échouer le
     * reçu pour un sceau manqué — cosmétique uniquement. */
    private static com.lowagie.text.pdf.PdfPCellEvent stampCellEvent(Color accent, String academicYearLabel, float radius) {
        return (cell, position, canvases) -> {
            try {
                float cx = position.getLeft() + radius + 2f;
                float cy = position.getBottom() + radius;
                PdfContentByte cb = canvases[PdfPTable.LINECANVAS];
                cb.saveState();
                double angle = Math.toRadians(-8);
                float cos = (float) Math.cos(angle);
                float sin = (float) Math.sin(angle);
                cb.transform(new java.awt.geom.AffineTransform(cos, sin, -sin, cos,
                        cx - cx * cos + cy * sin, cy - cx * sin - cy * cos));

                cb.setColorStroke(GOLD);
                cb.setLineWidth(1.7f);
                cb.ellipse(cx - radius, cy - radius, cx + radius, cy + radius);
                cb.stroke();
                cb.setLineDash(1.2f, 1.2f);
                cb.setLineWidth(0.6f);
                float innerRadius = radius * 0.81f;
                cb.ellipse(cx - innerRadius, cy - innerRadius, cx + innerRadius, cy + innerRadius);
                cb.stroke();

                float scale = radius / mm(13);
                ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                        new Phrase("NOTRE ÉCOLE", font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 4.3f * scale, new Color(138, 106, 31))),
                        cx, cy + mm(4) * scale, 0);
                ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                        new Phrase("PAYÉ", font(SERIF_BOLD, FontFactory.TIMES_BOLD, 11 * scale, accent)),
                        cx, cy - mm(1.5f) * scale, 0);
                if (academicYearLabel != null && !academicYearLabel.isBlank()) {
                    ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                            new Phrase(academicYearLabel, font(SANS_BOLD, FontFactory.HELVETICA_BOLD, 4.3f * scale, new Color(138, 106, 31))),
                            cx, cy - mm(6) * scale, 0);
                }
                cb.restoreState();
            } catch (RuntimeException e) {
                // Cosmétique uniquement — jamais faire échouer la génération du reçu pour un sceau manqué.
            }
        };
    }

    private List<GroupLine> parseGroupLines(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<GroupLine>>() {});
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    private static String channelLabel(Payment p) {
        if (p.getChannel() == null) return "-";
        return switch (p.getChannel()) {
            case CASH -> "Espèces";
            case ORANGE_MONEY -> "Orange Money";
            case BANK_CARD -> "Virement bancaire";
            case MTN_MONEY -> "MTN Money";
            case WAVE -> "Wave";
            case DJOMY -> "En ligne (Djomy)";
            case PAYROLL_DEDUCTION -> "Retenue sur salaire";
        };
    }

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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

    private static String safe(String s) { return s == null || s.isBlank() ? "-" : s; }
    private static String safe(String s, String fallback) { return s == null || s.isBlank() ? fallback : s; }
}
