package gn.lacaeducation.expenses.adapters.out.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
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
import gn.lacaeducation.expenses.application.port.out.AnnualBudgetPdfGenerator;
import gn.lacaeducation.expenses.domain.AnnualBudget;
import gn.lacaeducation.expenses.domain.BudgetLine;
import gn.lacaeducation.expenses.domain.BudgetLineType;
import gn.lacaeducation.expenses.domain.FinancingMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

/**
 * Récapitulatif PDF d'un budget (ex. budget d'événement — kermesse, sortie…) : lignes détaillées
 * (quantité × prix unitaire quand renseignés), total, et mode de financement — document soumis à la
 * direction/au fondateur pour validation.
 */
@Component
@RequiredArgsConstructor
class OpenPdfAnnualBudgetGenerator implements AnnualBudgetPdfGenerator {

    private static final Color GREY = SchoolPdfHeader.GREY;
    private static final Color DARK = SchoolPdfHeader.DARK;
    private static final Color ZEBRA = new Color(247, 248, 250);

    private final TenantInfoProvider tenantInfoProvider;

    @Override
    public byte[] generate(AnnualBudget budget, List<BudgetLine> lines) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 40, 54);
        try {
            TenantPublicInfo info = budget.getTenantId() == null ? null
                    : tenantInfoProvider.findTenantInfo(budget.getTenantId()).orElse(null);
            Color brand = SchoolPdfHeader.parseColor(info == null ? null : info.brandColor());
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new SchoolPdfPageFooter(info == null ? null : info.schoolName(), brand));
            doc.open();

            SchoolPdfHeader.render(doc, tenantInfoProvider, budget.getTenantId());

            Paragraph title = new Paragraph("BUDGET — " + safe(budget.getLabel()).toUpperCase(Locale.ROOT),
                    SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 16, brand));
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(8f);
            doc.add(title);

            Paragraph subtitle = new Paragraph("Année scolaire " + safe(budget.getAcademicYear())
                    + " · Statut : " + statusLabel(budget.getStatus()),
                    SchoolPdfHeader.font(FontFactory.HELVETICA, 10, GREY));
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(14f);
            doc.add(subtitle);

            List<BudgetLine> expenseLines = lines.stream()
                    .filter(l -> l.getLineType() == BudgetLineType.EXPENSE).toList();
            List<BudgetLine> revenueLines = lines.stream()
                    .filter(l -> l.getLineType() == BudgetLineType.REVENUE).toList();

            if (!revenueLines.isEmpty()) {
                sectionTitle(doc, "Recettes prévues", brand);
                doc.add(linesTable(revenueLines, brand));
            }

            sectionTitle(doc, "Dépenses prévues", brand);
            doc.add(linesTable(expenseLines, brand));

            BigDecimal totalExpense = sum(expenseLines);
            BigDecimal totalRevenue = sum(revenueLines);
            amountBox(doc, brand, "MONTANT TOTAL DU BUDGET", money(totalExpense) + " GNF");

            if (!revenueLines.isEmpty()) {
                Paragraph balance = new Paragraph(
                        "Solde (recettes − dépenses) : " + money(totalRevenue.subtract(totalExpense)) + " GNF",
                        SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 10, DARK));
                balance.setSpacingBefore(6f);
                doc.add(balance);
            }

            if (budget.getFinancingMode() != null && budget.getFinancingMode() != FinancingMode.NONE) {
                Paragraph financing = new Paragraph(
                        "Financement : " + financingLabel(budget.getFinancingMode())
                                + (budget.getFinancingAmount() != null
                                        ? " — " + money(budget.getFinancingAmount()) + " GNF" : ""),
                        SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 10, DARK));
                financing.setSpacingBefore(10f);
                doc.add(financing);
            }

            if (budget.getNotes() != null && !budget.getNotes().isBlank()) {
                Paragraph notes = new Paragraph("Notes : " + budget.getNotes(),
                        SchoolPdfHeader.font(FontFactory.HELVETICA, 9, GREY));
                notes.setSpacingBefore(10f);
                doc.add(notes);
            }

            Paragraph footer = new Paragraph(
                    "Document généré pour soumission à la direction / au fondateur pour validation.",
                    SchoolPdfHeader.font(FontFactory.HELVETICA_OBLIQUE, 8, GREY));
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(22f);
            doc.add(footer);

            doc.close();
            return out.toByteArray();
        } catch (RuntimeException e) {
            if (doc.isOpen()) doc.close();
            throw new IllegalStateException("Échec de génération du PDF du budget " + budget.getLabel(), e);
        }
    }

    private static PdfPTable linesTable(List<BudgetLine> lines, Color brand) {
        PdfPTable table = new PdfPTable(new float[]{3f, 1f, 1.4f, 1.6f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(10f);
        headerCell(table, "Article", brand);
        headerCell(table, "Qté", brand);
        headerCell(table, "Prix unitaire", brand);
        headerCell(table, "Montant", brand);
        if (lines.isEmpty()) {
            PdfPCell empty = new PdfPCell(new Phrase("Aucune ligne.", SchoolPdfHeader.font(FontFactory.HELVETICA_OBLIQUE, 9.5f, GREY)));
            empty.setColspan(4);
            empty.setHorizontalAlignment(Element.ALIGN_CENTER);
            empty.setPadding(10f);
            table.addCell(empty);
            return table;
        }
        boolean zebra = false;
        for (BudgetLine l : lines) {
            Color bg = zebra ? ZEBRA : Color.WHITE; zebra = !zebra;
            bodyCell(table, safe(l.getCategory()), Element.ALIGN_LEFT, bg);
            bodyCell(table, l.getQuantity() != null ? String.valueOf(l.getQuantity()) : "—", Element.ALIGN_CENTER, bg);
            bodyCell(table, l.getUnitPrice() != null ? money(l.getUnitPrice()) : "—", Element.ALIGN_RIGHT, bg);
            bodyCell(table, money(l.getPlannedAmount()) + " GNF", Element.ALIGN_RIGHT, bg);
        }
        return table;
    }

    private static void sectionTitle(Document doc, String text, Color brand) {
        Paragraph p = new Paragraph(text, SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 11, brand));
        p.setSpacingBefore(6f);
        p.setSpacingAfter(4f);
        doc.add(p);
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
        t.setSpacingBefore(8f);
        t.addCell(c);
        doc.add(t);
    }

    private static void headerCell(PdfPTable table, String text, Color brand) {
        PdfPCell cell = new PdfPCell(new Phrase(text, SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 9.5f, Color.WHITE)));
        cell.setBackgroundColor(brand);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(6f);
        cell.setBorderColor(brand);
        table.addCell(cell);
    }

    private static void bodyCell(PdfPTable table, String text, int alignment, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, SchoolPdfHeader.font(FontFactory.HELVETICA, 9.5f, DARK)));
        cell.setHorizontalAlignment(alignment);
        cell.setBackgroundColor(bg);
        cell.setPadding(5f);
        cell.setBorderColor(new Color(232, 234, 237));
        table.addCell(cell);
    }

    private static BigDecimal sum(List<BudgetLine> lines) {
        return lines.stream().map(l -> l.getPlannedAmount() != null ? l.getPlannedAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String statusLabel(gn.lacaeducation.expenses.domain.BudgetStatus status) {
        if (status == null) return "-";
        return switch (status) {
            case DRAFT -> "Brouillon";
            case SUBMITTED -> "Soumis";
            case APPROVED -> "Approuvé";
            case REJECTED -> "Rejeté";
            case CLOSED -> "Clôturé";
        };
    }

    private static String financingLabel(FinancingMode mode) {
        return switch (mode) {
            case COLLECTE -> "Collecte de fonds";
            case PRET -> "Prêt / avance de trésorerie";
            case NONE -> "Aucun";
        };
    }

    private static String money(BigDecimal a) {
        if (a == null) return "0";
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.FRANCE);
        sym.setGroupingSeparator(' ');
        return new DecimalFormat("#,##0", sym).format(a);
    }

    private static String safe(String s) { return s == null || s.isBlank() ? "—" : s; }
}
