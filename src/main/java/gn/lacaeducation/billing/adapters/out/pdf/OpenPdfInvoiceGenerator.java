package gn.lacaeducation.billing.adapters.out.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import gn.lacaeducation.billing.application.port.out.InvoicePdfGenerator;
import gn.lacaeducation.billing.application.port.out.SchoolFeeRepository;
import gn.lacaeducation.billing.domain.FeeType;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.MonthLabel;
import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.common.TenantPublicInfo;
import gn.lacaeducation.common.pdf.SchoolPdfHeader;
import gn.lacaeducation.common.pdf.SchoolPdfPageFooter;
import gn.lacaeducation.iam.UserPublicInfoProvider;
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

    private static final java.util.Map<FeeType, String> FEE_TYPE_LABEL = java.util.Map.of(
            FeeType.ENROLLMENT, "Inscription",
            FeeType.RE_ENROLLMENT, "Réinscription",
            FeeType.MONTHLY, "Mensualité",
            FeeType.TRANSPORT, "Transport",
            FeeType.CANTEEN, "Cantine",
            FeeType.EXAM, "Examen",
            FeeType.OTHER, "Autre",
            FeeType.PLATFORM, "Frais plateforme");

    private final TenantInfoProvider tenantInfoProvider;
    private final StudentDataProvider studentDataProvider;
    private final SchoolFeeRepository schoolFeeRepository;
    private final UserPublicInfoProvider userPublicInfoProvider;

    @Override
    public byte[] generate(Invoice inv) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // A4 (au lieu d'A5) : garantit que la facture tient sur une seule page quel que soit le
        // nombre de mois réglés listés (§retour comptable) — A5 laissait trop peu de marge.
        Document doc = new Document(PageSize.A4, 36, 36, 28, 40);
        try {
            TenantPublicInfo info = inv.getTenantId() == null ? null
                    : tenantInfoProvider.findTenantInfo(inv.getTenantId()).orElse(null);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new SchoolPdfPageFooter(info,
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
            row(studentBlock, "Type de frais", feeTypeLabel(inv));
            row(studentBlock, "Objet", inv.getPeriodLabel() != null && !inv.getPeriodLabel().isBlank()
                    ? inv.getPeriodLabel() : "Frais de scolarité");
            // Facture multi-mois (tranche) réglée en plusieurs fois : préciser quels mois sont
            // effectivement couverts par ce qui a été payé, plutôt qu'un solde global qui ne dit rien
            // du détail (§Correction-hier.md).
            if (inv.getPaidMonths() != null && !inv.getPaidMonths().isEmpty()) {
                row(studentBlock, "Mois réglés", inv.getPaidMonths().stream()
                        .sorted().map(MonthLabel::shortLabel)
                        .collect(java.util.stream.Collectors.joining(", ")));
            }
            doc.add(studentBlock);

            PdfPTable amounts = dataTable(new float[]{2f, 1.5f});
            headerCell(amounts, "Détail");
            headerCell(amounts, "Montant");
            cell(amounts, "Montant total", Element.ALIGN_LEFT);
            cell(amounts, money(inv.getTotalAmount()) + " GNF", Element.ALIGN_RIGHT);
            if (inv.getPlatformFeeAmount() != null && inv.getPlatformFeeAmount().signum() > 0) {
                cell(amounts, "  dont frais de service plateforme", Element.ALIGN_LEFT);
                cell(amounts, money(inv.getPlatformFeeAmount()) + " GNF", Element.ALIGN_RIGHT);
            }
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

            collectorLine(doc, inv);

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

    /** Nom réel du frais annexe (ex. "Tenue scolaire") si configuré, sinon le libellé générique du
     * type — jamais le seul mot "Autre" pour un frais annexe nommé (§retour comptable, point 22). */
    private String feeTypeLabel(Invoice inv) {
        if (inv.getFeeId() == null) return "-";
        var fee = schoolFeeRepository.findById(inv.getFeeId()).orElse(null);
        if (fee == null || fee.getType() == null) return "-";
        if (fee.getType() == FeeType.OTHER && fee.getLabel() != null && !fee.getLabel().isBlank()) {
            return fee.getLabel();
        }
        return FEE_TYPE_LABEL.getOrDefault(fee.getType(), fee.getType().name());
    }

    /** Nom et téléphone du comptable/caissier ayant physiquement encaissé le dernier paiement guichet
     * appliqué à cette facture — remplace l'ancien cadre de signature vide (§retour comptable, point
     * 5). Rien n'est affiché tant qu'aucun encaissement guichet n'a encore eu lieu. */
    private void collectorLine(Document doc, Invoice inv) {
        if (inv.getCollectedByUserId() == null) return;
        userPublicInfoProvider.findUser(inv.getCollectedByUserId()).ifPresent(u -> {
            String name = ((u.firstName() == null ? "" : u.firstName()) + " "
                    + (u.lastName() == null ? "" : u.lastName())).trim();
            if (name.isBlank()) return;
            String text = "Encaissé par : " + name + (u.phone() != null && !u.phone().isBlank() ? " — " + u.phone() : "");
            Paragraph p = new Paragraph(text, font(com.lowagie.text.FontFactory.HELVETICA_OBLIQUE, 9f, GREY));
            p.setAlignment(Element.ALIGN_RIGHT);
            p.setSpacingBefore(18f);
            doc.add(p);
        });
    }
}
