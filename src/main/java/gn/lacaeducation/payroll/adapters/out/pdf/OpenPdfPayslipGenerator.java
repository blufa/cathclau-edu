package gn.lacaeducation.payroll.adapters.out.pdf;

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
import gn.lacaeducation.payroll.application.port.out.PayslipPdfGenerator;
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

/** Rend une fiche de paie en PDF A5 soigné, aux couleurs de l'école (logo + couleur de marque). */
@Component
@RequiredArgsConstructor
class OpenPdfPayslipGenerator implements PayslipPdfGenerator {

    private static final Color GREY = new Color(110, 110, 110);
    private static final Color DARK = new Color(33, 37, 41);
    private static final Color LINE = new Color(232, 232, 232);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final TenantInfoProvider tenantInfoProvider;

    @Override
    public byte[] generate(Payslip p, List<PayslipLine> lines) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A5, 30, 30, 28, 40);
        try {
            TenantPublicInfo info = p.getTenantId() == null ? null
                    : tenantInfoProvider.findTenantInfo(p.getTenantId()).orElse(null);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new SchoolPdfPageFooter(info == null ? null : info.schoolName(),
                    SchoolPdfHeader.parseColor(info == null ? null : info.brandColor())));
            doc.open();

            Color brand = SchoolPdfHeader.render(doc, tenantInfoProvider, p.getTenantId());

            titleCard(doc, brand, "FICHE DE PAIE", "Période : " + safe(p.getPeriodLabel()));

            PdfPTable head = card();
            row(head, "Enseignant", p.getTeacherName() != null ? p.getTeacherName() : "#" + p.getTeacherId());
            if (p.getPeriodStart() != null || p.getPeriodEnd() != null) {
                row(head, "Du / Au", (p.getPeriodStart() == null ? "-" : p.getPeriodStart().format(DATE))
                        + "  →  " + (p.getPeriodEnd() == null ? "-" : p.getPeriodEnd().format(DATE)));
            }
            row(head, "Statut", statusLabel(p.getStatus()));
            if (p.getStatus() == PayslipStatus.PAID && p.getPaymentMethod() != null) {
                row(head, "Mode de paiement", "Virement".equalsIgnoreCase(p.getPaymentMethod()) ? "Virement" : p.getPaymentMethod());
            }
            doc.add(head);

            // Détails : base, gains (+), brut, retenues (−).
            PdfPTable det = new PdfPTable(new float[]{2.2f, 1f});
            det.setWidthPercentage(100);
            det.setSpacingBefore(12f);
            amountRow(det, "Salaire de base", money(p.getBaseSalary()), false, false);
            for (PayslipLine l : lines) {
                if (l.getLineType() == PayslipLineType.EARNING) amountRow(det, "+ " + l.getLabel(), money(l.getAmount()), false, false);
            }
            amountRow(det, "Salaire brut", money(p.getGrossSalary()), true, false);
            for (PayslipLine l : lines) {
                if (l.getLineType() == PayslipLineType.DEDUCTION) amountRow(det, "− " + l.getLabel(), money(l.getAmount()), false, true);
            }
            doc.add(det);

            amountBox(doc, brand, "NET À PAYER", money(p.getNetSalary()) + " GNF");

            footer(doc, "Document indicatif généré électroniquement par l'établissement.");

            doc.close();
            return out.toByteArray();
        } catch (RuntimeException e) {
            throw new IllegalStateException("Échec de génération du PDF de la fiche de paie " + p.getId(), e);
        }
    }

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
        l.setBorder(Rectangle.BOTTOM); l.setBorderColor(LINE); l.setBorderWidth(0.6f); l.setPadding(6f);
        PdfPCell v = new PdfPCell(new Phrase(value == null ? "-" : value, SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 9.5f, DARK)));
        v.setBorder(Rectangle.BOTTOM); v.setBorderColor(LINE); v.setBorderWidth(0.6f); v.setPadding(6f);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(l); t.addCell(v);
    }

    private static void amountRow(PdfPTable t, String label, String value, boolean strong, boolean deduction) {
        Color valueColor = deduction ? new Color(176, 0, 32) : DARK;
        PdfPCell l = new PdfPCell(new Phrase(label, SchoolPdfHeader.font(strong ? FontFactory.HELVETICA_BOLD : FontFactory.HELVETICA, 9.5f, strong ? DARK : GREY)));
        l.setBorder(Rectangle.BOTTOM); l.setBorderColor(LINE); l.setBorderWidth(0.6f); l.setPadding(6f);
        PdfPCell v = new PdfPCell(new Phrase(value, SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 9.5f, valueColor)));
        v.setBorder(Rectangle.BOTTOM); v.setBorderColor(LINE); v.setBorderWidth(0.6f); v.setPadding(6f);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(l); t.addCell(v);
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

    private static String statusLabel(PayslipStatus s) {
        if (s == null) return "-";
        return switch (s) {
            case DRAFT -> "Brouillon";
            case VALIDATED -> "Validée";
            case PAID -> "Payée";
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
