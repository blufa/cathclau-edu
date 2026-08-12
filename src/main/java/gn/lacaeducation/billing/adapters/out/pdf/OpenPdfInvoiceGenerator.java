package gn.lacaeducation.billing.adapters.out.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import gn.lacaeducation.billing.application.port.out.InvoicePdfGenerator;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.common.TenantPublicInfo;
import gn.lacaeducation.common.pdf.SchoolPdfHeader;
import gn.lacaeducation.common.pdf.SchoolPdfPageFooter;
import gn.lacaeducation.students.StudentDataProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

import static gn.lacaeducation.common.pdf.AdminDocumentLayout.*;

/**
 * Rend une facture scolaire en PDF A5, sobre (noir sur blanc) — même mise en page que le reçu de
 * paiement et la fiche de paie (voir {@code gn.lacaeducation.common.pdf.AdminDocumentLayout}) :
 * en-tête établissement, titre encadré, bloc élève, détail des montants, total et cadre de signature.
 */
@Component
@RequiredArgsConstructor
class OpenPdfInvoiceGenerator implements InvoicePdfGenerator {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final TenantInfoProvider tenantInfoProvider;
    private final StudentDataProvider studentDataProvider;

    @Override
    public byte[] generate(Invoice inv) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A5, 28, 28, 24, 36);
        try {
            TenantPublicInfo info = inv.getTenantId() == null ? null
                    : tenantInfoProvider.findTenantInfo(inv.getTenantId()).orElse(null);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new SchoolPdfPageFooter(info == null ? null : info.schoolName(),
                    SchoolPdfHeader.parseColor(info == null ? null : info.brandColor())));
            doc.open();

            header(doc, info);
            titleBox(doc, "FACTURE");

            PdfPTable idBlock = infoTable();
            row(idBlock, "N° facture", safe(inv.getReference()));
            row(idBlock, "Date d'émission", inv.getIssueDate() == null ? "-" : inv.getIssueDate().format(DATE));
            row(idBlock, "Statut", statusLabel(inv));
            doc.add(idBlock);

            PdfPTable studentBlock = infoTable();
            studentBlock.setSpacingBefore(10f);
            row(studentBlock, "Élève", studentName(inv.getStudentId()));
            row(studentBlock, "Objet", inv.getPeriodLabel() != null && !inv.getPeriodLabel().isBlank()
                    ? inv.getPeriodLabel() : "Frais de scolarité");
            doc.add(studentBlock);

            PdfPTable amounts = dataTable(new float[]{2f, 1.5f});
            headerCell(amounts, "Détail");
            headerCell(amounts, "Montant");
            cell(amounts, "Montant total", Element.ALIGN_LEFT);
            cell(amounts, money(inv.getTotalAmount()) + " GNF", Element.ALIGN_RIGHT);
            if (inv.getDiscountAmount() != null && inv.getDiscountAmount().signum() > 0) {
                String label = "Remise" + (inv.getDiscountReason() != null && !inv.getDiscountReason().isBlank()
                        ? " (" + inv.getDiscountReason() + ")" : "");
                cell(amounts, label, Element.ALIGN_LEFT);
                cell(amounts, "− " + money(inv.getDiscountAmount()) + " GNF", Element.ALIGN_RIGHT);
            }
            cell(amounts, "Déjà payé", Element.ALIGN_LEFT);
            cell(amounts, money(inv.getPaidAmount()) + " GNF", Element.ALIGN_RIGHT);
            doc.add(amounts);

            totalRow(doc, "Reste à payer", money(inv.getRemainingBalance()) + " GNF");

            signatureBox(doc, "Signature et Cachet de l'Établissement");

            doc.close();
            return out.toByteArray();
        } catch (RuntimeException e) {
            throw new IllegalStateException("Échec de génération du PDF de la facture " + inv.getReference(), e);
        }
    }

    private static String statusLabel(Invoice inv) {
        if (inv.getStatus() == null) return "-";
        return switch (inv.getStatus()) {
            case UNPAID -> "Impayée";
            case PARTIAL -> "Partielle";
            case PAID -> "Payée";
            default -> inv.getStatus().name();
        };
    }

    private String studentName(Long studentId) {
        if (studentId == null) return "-";
        return studentDataProvider.findStudent(studentId)
                .map(s -> ((s.firstName() == null ? "" : s.firstName()) + " "
                        + (s.lastName() == null ? "" : s.lastName())).trim())
                .filter(n -> !n.isBlank())
                .orElse("Élève #" + studentId);
    }
}
