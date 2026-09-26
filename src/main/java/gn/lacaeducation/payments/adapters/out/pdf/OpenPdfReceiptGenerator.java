package gn.lacaeducation.payments.adapters.out.pdf;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
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
import java.util.List;

import static gn.lacaeducation.common.pdf.AdminDocumentLayout.*;

/**
 * Rend un reçu de paiement officiel en PDF A4, sobre (noir sur blanc) façon reçu de caisse
 * administratif : en-tête établissement (logo + adresse + téléphone), titre encadré, bloc
 * identification compact (année scolaire, n° reçu, mode de paiement, élève, matricule, classe — deux
 * champs par ligne, voir {@link #compactFields}), tableau de TOUTES les factures de l'élève, total et
 * bloc de signature du comptable (nom de l'encaisseur, sans cadre — voir
 * {@link #comptableSignatureBlock}). A4 (au lieu d'A5) et bloc d'identification compact : garantissent
 * que le reçu tient sur une seule page quel que soit le nombre de factures listées (§retour comptable —
 * même raisonnement que {@code OpenPdfInvoiceGenerator}). La date de règlement n'apparaît plus qu'au
 * pied de page ("Généré le ..."), retirée d'ici pour ne pas la dupliquer.
 * Mise en page partagée avec la facture et la fiche de paie via {@link AdminDocumentLayout}.
 */
@Component
@RequiredArgsConstructor
class OpenPdfReceiptGenerator implements ReceiptPdfGenerator {

    private final TenantInfoProvider tenantInfoProvider;
    private final PayerDirectory payerDirectory;
    private final ObjectMapper objectMapper;

    /** Une part d'un paiement groupé (miroir minimal de {@code GroupedInvoiceSettlementEvent.Share}). */
    private record GroupLine(Long invoiceId, Long studentId, BigDecimal amount) {}

    @Override
    public byte[] generate(Receipt receipt, Payment payment) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // A4 (au lieu d'A5) : garantit que le reçu tient sur une seule page quel que soit le nombre de
        // factures listées dans le tableau ci-dessous (§retour comptable) — A5 débordait dès qu'un
        // élève avait plus de quelques factures (mensualités + frais annexes sur l'année).
        Document doc = new Document(PageSize.A4, 36, 36, 28, 40);
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

            // Bloc compact (deux champs par ligne, pas de "Date" — déjà au pied de page "Généré le...")
            // au lieu d'un champ par ligne, pour libérer de la place verticale (§retour comptable).
            doc.add(compactFields(
                    new String[]{"Année scolaire", safe(academicYearLabel)},
                    new String[]{"N° reçu", safe(receipt.getReference())},
                    new String[]{"Mode de paiement", channelLabel(payment)}));

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
                PdfPTable studentBlock = compactFields(
                        new String[]{"Matricule", safe(payerDirectory.studentNumber(studentId).orElse(null))},
                        new String[]{"Nom & Prénom", safe(receipt.getStudentName())},
                        new String[]{"Classe", safe(payerDirectory.className(studentId).orElse(null))});
                studentBlock.setSpacingBefore(8f);
                doc.add(studentBlock);
                doc.add(invoicesTable(studentId));
                remainingLine(doc, studentId);
            } else {
                for (Long studentId : studentIds) {
                    PdfPTable studentBlock = compactFields(
                            new String[]{"Élève", safe(payerDirectory.studentName(studentId).orElse(null))},
                            new String[]{"Matricule", safe(payerDirectory.studentNumber(studentId).orElse(null))},
                            new String[]{"Classe", safe(payerDirectory.className(studentId).orElse(null))});
                    studentBlock.setSpacingBefore(8f);
                    doc.add(studentBlock);
                    doc.add(invoicesTable(studentId));
                    remainingLine(doc, studentId);
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

            comptableSignatureBlock(doc, receipt, payment.getCollectedByUserId());

            doc.close();
            return out.toByteArray();
        } catch (RuntimeException e) {
            throw new IllegalStateException("Échec de génération du PDF du reçu " + receipt.getReference(), e);
        }
    }

    /** Bloc "Libellé : valeur" compact, deux champs par ligne au lieu d'un seul (§retour comptable) —
     * libère assez de place verticale pour que le reçu tienne sur une seule page même avec un long
     * tableau de factures en dessous. Nombre de paires toujours pair ici (3 champs → complété par une
     * cellule vide), mais gère aussi un nombre impair au cas où. */
    private PdfPTable compactFields(String[]... pairs) {
        PdfPTable t = new PdfPTable(new float[]{0.9f, 1.6f, 0.9f, 1.6f});
        t.setWidthPercentage(100);
        for (int i = 0; i < pairs.length; i += 2) {
            addFieldCells(t, pairs[i][0], pairs[i][1]);
            if (i + 1 < pairs.length) {
                addFieldCells(t, pairs[i + 1][0], pairs[i + 1][1]);
            } else {
                blankCell(t);
                blankCell(t);
            }
        }
        return t;
    }

    private void addFieldCells(PdfPTable t, String label, String value) {
        PdfPCell l = new PdfPCell(new com.lowagie.text.Phrase(label, font(FontFactory.HELVETICA_BOLD, 9, DARK)));
        l.setBorder(0);
        l.setPaddingBottom(4f);
        t.addCell(l);
        PdfPCell v = new PdfPCell(new com.lowagie.text.Phrase(": " + (value == null ? "-" : value), font(FontFactory.HELVETICA, 9, DARK)));
        v.setBorder(0);
        v.setPaddingBottom(4f);
        t.addCell(v);
    }

    private void blankCell(PdfPTable t) {
        PdfPCell c = new PdfPCell(new com.lowagie.text.Phrase(""));
        c.setBorder(0);
        t.addCell(c);
    }

    /**
     * Toutes les factures (non annulées) de l'élève — pas seulement celle(s) couvertes par ce
     * paiement — pour que le reçu serve de vue d'ensemble de sa situation (§retour comptable) : Réf.,
     * Montant, Observation (type de frais + période), et une colonne Payé en gras (OUI/NON) donnant
     * son statut de règlement à cette date.
     */
    private PdfPTable invoicesTable(Long studentId) {
        PdfPTable t = dataTable(new float[]{1.1f, 1.1f, 1.4f, 0.6f});
        headerCell(t, "Réf.");
        headerCell(t, "Montant");
        headerCell(t, "Observation");
        headerCell(t, "Payé");

        List<PayerDirectory.InvoiceRowInfo> invoices = payerDirectory.studentInvoices(studentId);
        for (PayerDirectory.InvoiceRowInfo inv : invoices) {
            cell(t, inv.reference(), Element.ALIGN_LEFT);
            cell(t, money(inv.amount()) + " GNF", Element.ALIGN_RIGHT);
            cell(t, inv.label(), Element.ALIGN_LEFT);
            t.addCell(paidCell(inv.paid()));
        }
        return t;
    }

    /** OUI (vert) / NON (rouge) en gras — jamais dans la police neutre de {@link #cell}, pour que le
     * statut de règlement saute aux yeux sur le reçu papier. */
    private com.lowagie.text.pdf.PdfPCell paidCell(boolean paid) {
        java.awt.Color color = paid ? new java.awt.Color(20, 120, 40) : new java.awt.Color(170, 30, 30);
        com.lowagie.text.pdf.PdfPCell c = new com.lowagie.text.pdf.PdfPCell(
                new com.lowagie.text.Phrase(paid ? "OUI" : "NON", font(FontFactory.HELVETICA_BOLD, 9, color)));
        c.setPadding(6f);
        c.setBorderColor(LINE);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        return c;
    }

    /** Total encore dû par l'élève, toutes factures confondues — mensualités, frais annexes, transport,
     * cantine, etc., toute facture encore générée et non soldée (§retour comptable) — jamais seulement
     * le solde de la facture réglée par ce paiement précis. Calculé côté serveur (jamais une valeur
     * transmise par le client au moment de l'encaissement, potentiellement incomplète sur une
     * fratrie). Rien n'est affiché si l'élève n'a plus aucune facture due. Le libellé "Reste à payer"
     * est souligné plutôt qu'accompagné d'un filet/bordure, pour rester sobre sous le tableau. */
    private void remainingLine(Document doc, Long studentId) {
        BigDecimal total = payerDirectory.unpaidInvoices(studentId).stream()
                .map(PayerDirectory.UnpaidInvoiceInfo::remaining)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.signum() <= 0) return;
        Font underlinedLabel = FontFactory.getFont(FontFactory.HELVETICA, 9.5f, Font.BOLD | Font.UNDERLINE, DARK);
        Paragraph rest = new Paragraph();
        rest.add(new Chunk("Reste à payer", underlinedLabel));
        rest.add(new Chunk(" : " + money(total) + " GNF", font(FontFactory.HELVETICA_BOLD, 9.5f, DARK)));
        rest.setSpacingBefore(6f);
        doc.add(rest);
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

    /** Bloc de signature du comptable (§retour comptable, point 2) : plus de cadre rectangulaire
     * (remplace {@link AdminDocumentLayout#signatureBox}, encore utilisé tel quel par la paie), le
     * libellé porte "Comptable" au lieu de "Caissier", et le nom de l'encaisseur (auparavant affiché
     * dans une ligne "Opérateur" séparée) est imprimé sous ce libellé, avec son téléphone PERSONNEL
     * juste en dessous — distinct du "Tél :" de l'école affiché en en-tête — puis un espace suffisant
     * pour que le comptable puisse y apposer sa signature et son cachet à la main. */
    private void comptableSignatureBlock(Document doc, Receipt receipt, Long collectedByUserId) {
        Paragraph label = new Paragraph("Signature et Cachet du Comptable",
                font(FontFactory.HELVETICA_BOLD, 9.5f, DARK));
        label.setAlignment(Element.ALIGN_RIGHT);
        label.setSpacingBefore(24f);
        doc.add(label);
        String operator = receipt.getCollectedByName();
        if (operator != null && !operator.isBlank()) {
            Paragraph name = new Paragraph(operator, font(FontFactory.HELVETICA, 9.5f, GREY));
            name.setAlignment(Element.ALIGN_RIGHT);
            name.setSpacingBefore(4f);
            doc.add(name);
            String phone = payerDirectory.operatorPhone(collectedByUserId).orElse(null);
            if (phone != null) {
                Paragraph phoneLine = new Paragraph("Tél : " + phone, font(FontFactory.HELVETICA, 9, GREY));
                phoneLine.setAlignment(Element.ALIGN_RIGHT);
                phoneLine.setSpacingBefore(2f);
                doc.add(phoneLine);
            }
            // Espace vide volontairement large (signature + cachet manuscrits), après le nom/téléphone,
            // au lieu des 4pt précédents qui ne laissaient presque aucune place.
            Paragraph spacer = new Paragraph(" ", font(FontFactory.HELVETICA, 9.5f, GREY));
            spacer.setSpacingBefore(36f);
            doc.add(spacer);
        }
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
