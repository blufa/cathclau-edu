package gn.lacaeducation.payments.adapters.out.pdf;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
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
import gn.lacaeducation.common.pdf.SchoolPdfHeader;
import gn.lacaeducation.common.pdf.SchoolPdfPageFooter;
import gn.lacaeducation.payments.PayerDirectory;
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
 * Rend un reçu de paiement officiel en PDF A5, sobre (noir sur blanc) façon reçu de caisse
 * administratif : en-tête établissement (logo + adresse + téléphone), titre encadré, bloc
 * identification (année scolaire, n° reçu, date, mode de paiement, opérateur), bloc élève
 * (matricule, nom, classe), tableau des frais réglés, total et cadre de signature/cachet.
 */
@Component
@RequiredArgsConstructor
class OpenPdfReceiptGenerator implements ReceiptPdfGenerator {

    private static final Color GREY = new Color(90, 90, 90);
    private static final Color DARK = new Color(20, 20, 20);
    private static final Color LINE = new Color(60, 60, 60);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH'h'mm");

    private final TenantInfoProvider tenantInfoProvider;
    private final PayerDirectory payerDirectory;
    private final ObjectMapper objectMapper;

    /** Une part d'un paiement groupé (miroir minimal de {@code GroupedInvoiceSettlementEvent.Share}). */
    private record GroupLine(Long invoiceId, BigDecimal amount) {}

    @Override
    public byte[] generate(Receipt receipt, Payment payment) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A5, 28, 28, 24, 36);
        try {
            TenantPublicInfo info = payment.getTenantId() == null ? null
                    : tenantInfoProvider.findTenantInfo(payment.getTenantId()).orElse(null);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new SchoolPdfPageFooter(info == null ? null : info.schoolName(),
                    SchoolPdfHeader.parseColor(info == null ? null : info.brandColor())));
            doc.open();

            header(doc, info);
            titleBox(doc, "REÇU DE PAIEMENT OFFICIEL");

            PdfPTable idBlock = infoTable();
            row(idBlock, "Année scolaire", safe(payerDirectory.currentAcademicYearLabel(payment.getTenantId()).orElse(null)));
            row(idBlock, "N° reçu", safe(receipt.getReference()));
            row(idBlock, "Date", receipt.getGeneratedAt() == null ? "-" : receipt.getGeneratedAt().format(DATE));
            row(idBlock, "Mode de paiement", channelLabel(payment));
            String operator = receipt.getCollectedByName();
            if (operator != null && !operator.isBlank()) {
                row(idBlock, "Opérateur", operator);
            }
            doc.add(idBlock);

            PdfPTable studentBlock = infoTable();
            studentBlock.setSpacingBefore(10f);
            row(studentBlock, "Matricule", safe(payerDirectory.studentNumber(payment.getStudentId()).orElse(null)));
            row(studentBlock, "Nom & Prénom", safe(receipt.getStudentName()));
            row(studentBlock, "Classe", safe(payerDirectory.className(payment.getStudentId()).orElse(null)));
            doc.add(studentBlock);

            doc.add(feesTable(receipt, payment));
            totalRow(doc, money(receipt.getAmount()) + " GNF");
            if (receipt.getRemainingAfter() != null) {
                Paragraph rest = new Paragraph("Reste à payer : " + money(receipt.getRemainingAfter()) + " GNF",
                        SchoolPdfHeader.font(FontFactory.HELVETICA, 9, GREY));
                rest.setSpacingBefore(4f);
                doc.add(rest);
            }

            signatureBox(doc);

            doc.close();
            return out.toByteArray();
        } catch (RuntimeException e) {
            throw new IllegalStateException("Échec de génération du PDF du reçu " + receipt.getReference(), e);
        }
    }

    // ── En-tête sobre (logo + établissement), sans bandeau coloré ────────────

    private static void header(Document doc, TenantPublicInfo info) {
        PdfPTable band = new PdfPTable(new float[]{1f, 3.4f});
        band.setWidthPercentage(100);
        band.setSpacingAfter(10f);

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(0);
        logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Image logo = info == null ? null : SchoolPdfHeader.loadLogo(info.logo());
        if (logo != null) {
            logo.scaleToFit(48, 48);
            logoCell.addElement(logo);
        }
        band.addCell(logoCell);

        PdfPCell textCell = new PdfPCell();
        textCell.setBorder(0);
        textCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        textCell.addElement(new Paragraph(info == null || info.schoolName() == null ? "Établissement" : info.schoolName(),
                SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 13, DARK)));
        String address = join(", ", info == null ? null : info.address(), info == null ? null : info.city());
        if (!address.isBlank()) {
            textCell.addElement(new Paragraph(address, SchoolPdfHeader.font(FontFactory.HELVETICA, 9, GREY)));
        }
        String phone = info == null ? null : info.phone();
        if (phone != null && !phone.isBlank()) {
            textCell.addElement(new Paragraph("Tél. " + phone, SchoolPdfHeader.font(FontFactory.HELVETICA, 9, GREY)));
        }
        band.addCell(textCell);
        doc.add(band);
    }

    private static void titleBox(Document doc, String title) {
        PdfPCell c = new PdfPCell(new Phrase(title, SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 13, DARK)));
        c.setBorder(Rectangle.BOX);
        c.setBorderColor(LINE);
        c.setBorderWidth(1f);
        c.setPadding(8f);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingAfter(12f);
        t.addCell(c);
        doc.add(t);
    }

    private static PdfPTable infoTable() {
        PdfPTable t = new PdfPTable(new float[]{1.3f, 2f});
        t.setWidthPercentage(100);
        return t;
    }

    private static void row(PdfPTable t, String label, String value) {
        PdfPCell l = new PdfPCell(new Phrase(label, SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 9.5f, DARK)));
        l.setBorder(0);
        l.setPaddingBottom(4f);
        PdfPCell v = new PdfPCell(new Phrase(": " + value, SchoolPdfHeader.font(FontFactory.HELVETICA, 9.5f, DARK)));
        v.setBorder(0);
        v.setPaddingBottom(4f);
        t.addCell(l);
        t.addCell(v);
    }

    /**
     * Une ligne par frais réellement réglé (une par mois pour un paiement groupé, bien alignées dans
     * le tableau plutôt que résumées en une seule cellule) avec, en observation, le type de frais
     * (« Inscription », « Mensualité »…) — les deux apparaissent naturellement si le règlement couvre
     * à la fois une inscription et une mensualité (paiement groupé mêlant les deux).
     */
    private PdfPTable feesTable(Receipt receipt, Payment payment) {
        PdfPTable t = new PdfPTable(new float[]{1.2f, 1.3f, 1.5f});
        t.setWidthPercentage(100);
        t.setSpacingBefore(12f);
        header(t, "Réf.");
        header(t, "Montant payé");
        header(t, "Observation");

        List<GroupLine> groupLines = parseGroupLines(payment.getGroupLines());
        if (!groupLines.isEmpty()) {
            for (GroupLine line : groupLines) {
                cell(t, receipt.getReference(), Element.ALIGN_LEFT);
                cell(t, money(line.amount()) + " GNF", Element.ALIGN_RIGHT);
                cell(t, observationFor(line.invoiceId()), Element.ALIGN_LEFT);
            }
        } else {
            cell(t, receipt.getReference(), Element.ALIGN_LEFT);
            cell(t, money(receipt.getAmount()) + " GNF", Element.ALIGN_RIGHT);
            cell(t, observationFor(payment.getInvoiceId()), Element.ALIGN_LEFT);
        }
        return t;
    }

    /** « Inscription », « Mensualité — Octobre 2025 »… résolu depuis la facture ; repli neutre sinon. */
    private String observationFor(Long invoiceId) {
        return payerDirectory.invoiceFeeInfo(invoiceId)
                .map(info -> info.periodLabel() == null || info.periodLabel().isBlank()
                        ? info.feeTypeLabel()
                        : info.feeTypeLabel() + " — " + info.periodLabel())
                .orElse("Frais soldé");
    }

    private List<GroupLine> parseGroupLines(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<GroupLine>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private static void header(PdfPTable t, String label) {
        PdfPCell c = new PdfPCell(new Phrase(label, SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 9, Color.WHITE)));
        c.setBackgroundColor(DARK);
        c.setPadding(6f);
        c.setBorderColor(LINE);
        t.addCell(c);
    }

    private static void cell(PdfPTable t, String value, int align) {
        PdfPCell c = new PdfPCell(new Phrase(value == null ? "-" : value, SchoolPdfHeader.font(FontFactory.HELVETICA, 9, DARK)));
        c.setPadding(6f);
        c.setBorderColor(LINE);
        c.setHorizontalAlignment(align);
        t.addCell(c);
    }

    private static void totalRow(Document doc, String amount) {
        PdfPTable t = new PdfPTable(new float[]{2f, 2f});
        t.setWidthPercentage(100);
        t.setSpacingBefore(0f);
        PdfPCell l = new PdfPCell(new Phrase("Total payé", SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 10, DARK)));
        l.setBorder(Rectangle.TOP);
        l.setBorderColor(LINE);
        l.setPadding(7f);
        PdfPCell v = new PdfPCell(new Phrase(amount, SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 11, DARK)));
        v.setBorder(Rectangle.TOP);
        v.setBorderColor(LINE);
        v.setPadding(7f);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(l);
        t.addCell(v);
        doc.add(t);
    }

    private static void signatureBox(Document doc) {
        Font f = SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 9.5f, DARK);
        PdfPCell c = new PdfPCell(new Phrase("Signature et Cachet du Caissier", f));
        c.setBorder(Rectangle.BOX);
        c.setBorderColor(LINE);
        c.setFixedHeight(70f);
        c.setPadding(8f);
        c.setVerticalAlignment(Element.ALIGN_TOP);
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(60);
        t.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.setSpacingBefore(22f);
        t.addCell(c);
        doc.add(t);
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
        };
    }

    private static String money(BigDecimal a) {
        if (a == null) return "0";
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.FRANCE);
        sym.setGroupingSeparator(' ');
        return new DecimalFormat("#,##0", sym).format(a);
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

    private static String safe(String s) { return (s == null || s.isBlank()) ? "-" : s; }
}
