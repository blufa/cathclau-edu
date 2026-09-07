package gn.lacaeducation.payments.adapters.out.pdf;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.common.TenantPublicInfo;
import gn.lacaeducation.common.pdf.AdminDocumentLayout;
import gn.lacaeducation.common.pdf.SchoolPdfHeader;
import gn.lacaeducation.common.pdf.SchoolPdfPageFooter;
import gn.lacaeducation.payments.PayerDirectory;
import gn.lacaeducation.payments.application.port.out.ReceiptPdfGenerator;
import gn.lacaeducation.payments.domain.Payment;
import gn.lacaeducation.payments.domain.Receipt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static gn.lacaeducation.common.pdf.AdminDocumentLayout.*;

/**
 * Rend un reçu de paiement officiel en PDF A5, sobre (noir sur blanc) façon reçu de caisse
 * administratif : en-tête établissement (logo + adresse + téléphone), titre encadré, bloc
 * identification (année scolaire, n° reçu, date, mode de paiement, opérateur), bloc élève
 * (matricule, nom, classe), tableau des frais réglés, total et cadre de signature/cachet.
 * Mise en page partagée avec la facture et la fiche de paie via {@link AdminDocumentLayout}.
 */
@Component
@RequiredArgsConstructor
class OpenPdfReceiptGenerator implements ReceiptPdfGenerator {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH'h'mm");

    private final TenantInfoProvider tenantInfoProvider;
    private final PayerDirectory payerDirectory;
    private final ObjectMapper objectMapper;

    /** Une part d'un paiement groupé (miroir minimal de {@code GroupedInvoiceSettlementEvent.Share}). */
    private record GroupLine(Long invoiceId, Long studentId, BigDecimal amount) {}

    @Override
    public byte[] generate(Receipt receipt, Payment payment) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A5, 28, 28, 24, 36);
        try {
            TenantPublicInfo info = payment.getTenantId() == null ? null
                    : tenantInfoProvider.findTenantInfo(payment.getTenantId()).orElse(null);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new SchoolPdfPageFooter(info,
                    SchoolPdfHeader.parseColor(info == null ? null : info.brandColor())));
            doc.open();

            String academicYearLabel = payerDirectory.currentAcademicYearLabel(payment.getTenantId()).orElse(null);
            header(doc, info, academicYearLabel);
            titleBox(doc, "REÇU DE PAIEMENT OFFICIEL");

            PdfPTable idBlock = infoTable();
            row(idBlock, "Année scolaire", safe(academicYearLabel));
            row(idBlock, "N° reçu", safe(receipt.getReference()));
            row(idBlock, "Date", receipt.getGeneratedAt() == null ? "-" : receipt.getGeneratedAt().format(DATE));
            row(idBlock, "Mode de paiement", channelLabel(payment));
            String operator = receipt.getCollectedByName();
            if (operator != null && !operator.isBlank()) {
                row(idBlock, "Opérateur", operator);
            }
            doc.add(idBlock);

            // Fratrie (§payments) : un paiement groupé peut couvrir plusieurs enfants — un bloc + un
            // sous-tableau de frais par enfant plutôt qu'un bloc élève unique, dans l'ordre où ils
            // apparaissent dans les parts du paiement.
            java.util.LinkedHashSet<Long> studentIds = new java.util.LinkedHashSet<>();
            for (GroupLine line : parseGroupLines(payment.getGroupLines())) {
                if (line.studentId() != null) studentIds.add(line.studentId());
            }
            if (studentIds.isEmpty() && payment.getStudentId() != null) studentIds.add(payment.getStudentId());

            if (studentIds.size() <= 1) {
                Long studentId = studentIds.isEmpty() ? payment.getStudentId() : studentIds.iterator().next();
                PdfPTable studentBlock = infoTable();
                studentBlock.setSpacingBefore(10f);
                row(studentBlock, "Matricule", safe(payerDirectory.studentNumber(studentId).orElse(null)));
                row(studentBlock, "Nom & Prénom", safe(receipt.getStudentName()));
                row(studentBlock, "Classe", safe(payerDirectory.className(studentId).orElse(null)));
                doc.add(studentBlock);
                doc.add(feesTable(receipt, payment, null));
            } else {
                for (Long studentId : studentIds) {
                    PdfPTable studentBlock = infoTable();
                    studentBlock.setSpacingBefore(10f);
                    row(studentBlock, "Élève", safe(payerDirectory.studentName(studentId).orElse(null)));
                    row(studentBlock, "Matricule", safe(payerDirectory.studentNumber(studentId).orElse(null)));
                    row(studentBlock, "Classe", safe(payerDirectory.className(studentId).orElse(null)));
                    doc.add(studentBlock);
                    doc.add(feesTable(receipt, payment, studentId));
                }
            }

            totalRow(doc, "Total payé", money(receipt.getAmount()) + " GNF");
            String discountNote = discountNote(payment);
            if (discountNote != null) {
                Paragraph discount = new Paragraph(discountNote, font(FontFactory.HELVETICA, 9, GREY));
                discount.setSpacingBefore(4f);
                doc.add(discount);
            }
            String platformFeeNote = platformFeeNote(payment);
            if (platformFeeNote != null) {
                Paragraph pf = new Paragraph(platformFeeNote, font(FontFactory.HELVETICA, 9, GREY));
                pf.setSpacingBefore(4f);
                doc.add(pf);
            }
            if (receipt.getRemainingAfter() != null) {
                Paragraph rest = new Paragraph("Reste à payer : " + money(receipt.getRemainingAfter()) + " GNF",
                        font(FontFactory.HELVETICA, 9, GREY));
                rest.setSpacingBefore(4f);
                doc.add(rest);
            }

            signatureBox(doc, "Signature et Cachet du Caissier");

            doc.close();
            return out.toByteArray();
        } catch (RuntimeException e) {
            throw new IllegalStateException("Échec de génération du PDF du reçu " + receipt.getReference(), e);
        }
    }

    /**
     * Une ligne par frais réellement réglé (une par mois pour un paiement groupé, bien alignées dans
     * le tableau plutôt que résumées en une seule cellule) avec, en observation, le type de frais
     * (« Inscription », « Mensualité »…) — les deux apparaissent naturellement si le règlement couvre
     * à la fois une inscription et une mensualité (paiement groupé mêlant les deux).
     */
    private PdfPTable feesTable(Receipt receipt, Payment payment, Long onlyStudentId) {
        PdfPTable t = dataTable(new float[]{1.2f, 1.3f, 1.5f});
        headerCell(t, "Réf.");
        headerCell(t, "Montant payé");
        headerCell(t, "Observation");

        List<GroupLine> groupLines = parseGroupLines(payment.getGroupLines());
        if (!groupLines.isEmpty()) {
            for (GroupLine line : groupLines) {
                if (onlyStudentId != null && !onlyStudentId.equals(line.studentId())) continue;
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

    /**
     * Remise totale (ex. paiement anticipé) portée par les factures réglées par ce paiement — visible
     * sur le reçu même si elle a été calculée et appliquée automatiquement avant l'encaissement.
     */
    private String discountNote(Payment payment) {
        List<GroupLine> groupLines = parseGroupLines(payment.getGroupLines());
        List<Long> invoiceIds = !groupLines.isEmpty()
                ? groupLines.stream().map(GroupLine::invoiceId).toList()
                : (payment.getInvoiceId() != null ? List.of(payment.getInvoiceId()) : List.of());
        BigDecimal total = BigDecimal.ZERO;
        String reason = null;
        for (Long id : invoiceIds) {
            var info = payerDirectory.invoiceFeeInfo(id).orElse(null);
            if (info == null || info.discountAmount() == null || info.discountAmount().signum() <= 0) continue;
            total = total.add(info.discountAmount());
            if (reason == null) reason = info.discountReason();
        }
        if (total.signum() <= 0) return null;
        return "Remise appliquée : -" + money(total) + " GNF" + (reason != null && !reason.isBlank() ? " (" + reason + ")" : "");
    }

    /**
     * Part du paiement due au frais d'usage plateforme (§billing) — affichée pour que la ligne ne
     * disparaisse jamais silencieusement du reçu même quand l'élève a bien accepté l'accès plateforme.
     */
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
        };
    }
}
