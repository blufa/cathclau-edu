package gn.lacaeducation.payments.adapters.out.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
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
import java.util.Locale;

/** Rend un reçu de paiement en PDF A5 soigné, aux couleurs de l'école (logo + couleur de marque). */
@Component
@RequiredArgsConstructor
class OpenPdfReceiptGenerator implements ReceiptPdfGenerator {

    private static final Color GREY = new Color(110, 110, 110);
    private static final Color DARK = new Color(33, 37, 41);
    private static final Color LINE = new Color(232, 232, 232);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final TenantInfoProvider tenantInfoProvider;

    @Override
    public byte[] generate(Receipt receipt, Payment payment) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A5, 30, 30, 28, 40);
        try {
            TenantPublicInfo info = payment.getTenantId() == null ? null
                    : tenantInfoProvider.findTenantInfo(payment.getTenantId()).orElse(null);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new SchoolPdfPageFooter(info == null ? null : info.schoolName(),
                    SchoolPdfHeader.parseColor(info == null ? null : info.brandColor())));
            doc.open();

            Color brand = SchoolPdfHeader.render(doc, tenantInfoProvider, payment.getTenantId());

            titleCard(doc, brand, "REÇU DE PAIEMENT",
                    "N° " + safe(receipt.getReference()) + "   ·   "
                            + (receipt.getGeneratedAt() == null ? "-" : receipt.getGeneratedAt().format(DATE)));

            PdfPTable det = card();
            if (receipt.getStudentName() != null && !receipt.getStudentName().isBlank()) {
                row(det, "Élève", receipt.getStudentName());
            }
            row(det, "Référence paiement", safe(payment.getReference()));
            row(det, "Mode de paiement", channelLabel(payment));
            row(det, "Type", payment.getType() == null ? "-" : typeLabel(payment));
            if (receipt.getCoveredMonths() != null && !receipt.getCoveredMonths().isBlank()) {
                String months = receipt.getCoveredMonths()
                        + (receipt.getMonthsCount() != null ? "  (" + receipt.getMonthsCount() + " mois)" : "");
                row(det, "Mois réglés", months);
                if (receipt.getRemainingAfter() != null) {
                    row(det, "Reste à payer", money(receipt.getRemainingAfter()) + " GNF");
                }
            }
            doc.add(det);

            amountBox(doc, brand, "MONTANT PAYÉ", money(receipt.getAmount()) + " GNF");

            footer(doc, "Reçu généré électroniquement — faisant foi du règlement enregistré.");

            doc.close();
            return out.toByteArray();
        } catch (RuntimeException e) {
            throw new IllegalStateException("Échec de génération du PDF du reçu " + receipt.getReference(), e);
        }
    }

    // ── Briques de mise en page partagées (look A5) ──────────────────────────

    private static void titleCard(Document doc, Color brand, String title, String subtitle) {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(SchoolPdfHeader.tint(brand, 0.10));
        c.setBorder(Rectangle.LEFT);
        c.setBorderColorLeft(brand);
        c.setBorderWidthLeft(3f);
        c.setPadding(10f);
        c.addElement(new Paragraph(title, SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 15, brand)));
        c.addElement(new Paragraph(subtitle, SchoolPdfHeader.font(FontFactory.HELVETICA, 9, GREY)));
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(12f);
        t.setSpacingAfter(4f);
        t.addCell(c);
        doc.add(t);
    }

    private static PdfPTable card() {
        PdfPTable t = new PdfPTable(new float[]{1.3f, 2f});
        t.setWidthPercentage(100);
        t.setSpacingBefore(10f);
        return t;
    }

    private static void row(PdfPTable t, String label, String value) {
        PdfPCell l = new PdfPCell(new Phrase(label, SchoolPdfHeader.font(FontFactory.HELVETICA, 9, GREY)));
        l.setBorder(Rectangle.BOTTOM);
        l.setBorderColor(LINE);
        l.setBorderWidth(0.6f);
        l.setPadding(6f);
        PdfPCell v = new PdfPCell(new Phrase(value == null ? "-" : value, SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 9.5f, DARK)));
        v.setBorder(Rectangle.BOTTOM);
        v.setBorderColor(LINE);
        v.setBorderWidth(0.6f);
        v.setPadding(6f);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(l);
        t.addCell(v);
    }

    private static void amountBox(Document doc, Color brand, String label, String amount) {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(brand);
        c.setBorder(0);
        c.setPadding(13f);
        Paragraph lab = new Paragraph(label, SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 9, Color.WHITE));
        lab.setSpacingAfter(3f);
        c.addElement(lab);
        c.addElement(new Paragraph(amount, SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 20, Color.WHITE)));
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(16f);
        t.addCell(c);
        doc.add(t);
    }

    private static void footer(Document doc, String text) {
        Paragraph p = new Paragraph(text, SchoolPdfHeader.font(FontFactory.HELVETICA_OBLIQUE, 8, GREY));
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingBefore(22f);
        doc.add(p);
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

    private static String typeLabel(Payment p) {
        return switch (p.getType()) {
            case PARTIAL -> "Partiel";
            case COMPLETE -> "Complet";
        };
    }

    private static String money(BigDecimal a) {
        if (a == null) return "0";
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.FRANCE);
        sym.setGroupingSeparator(' ');
        return new DecimalFormat("#,##0", sym).format(a);
    }


    private static String safe(String s) { return (s == null || s.isBlank()) ? "-" : s; }
}
