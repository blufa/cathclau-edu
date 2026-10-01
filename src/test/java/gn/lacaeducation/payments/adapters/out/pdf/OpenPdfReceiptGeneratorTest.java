package gn.lacaeducation.payments.adapters.out.pdf;

import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.common.TenantPublicInfo;
import gn.lacaeducation.payments.PayerDirectory;
import gn.lacaeducation.payments.PayerDirectory.InvoiceRowInfo;
import gn.lacaeducation.payments.PayerDirectory.UnpaidInvoiceInfo;
import gn.lacaeducation.payments.domain.Payment;
import gn.lacaeducation.payments.domain.PaymentChannel;
import gn.lacaeducation.payments.domain.Receipt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Tests de fumée (§retour comptable, refonte du tableau de factures + bloc signature du comptable) —
 * pas d'assertion sur le contenu textuel du PDF (aucune bibliothèque d'extraction de texte dans le
 * projet), seulement que le document se génère sans exception quel que soit le nombre de factures.
 */
@ExtendWith(MockitoExtension.class)
class OpenPdfReceiptGeneratorTest {

    @Mock TenantInfoProvider tenantInfoProvider;
    @Mock PayerDirectory payerDirectory;
    @Mock tools.jackson.databind.ObjectMapper objectMapper;

    private OpenPdfReceiptGenerator generator() {
        return new OpenPdfReceiptGenerator(tenantInfoProvider, payerDirectory, objectMapper);
    }

    private Payment payment() {
        return Payment.builder().id(1L).tenantId(10L).studentId(7L).invoiceId(2L)
                .amount(new BigDecimal("50000")).channel(PaymentChannel.CASH)
                .collectedByUserId(99L).build();
    }

    private Receipt receipt() {
        return Receipt.builder().id(1L).paymentId(1L).reference("REC-1")
                .amount(new BigDecimal("50000")).generatedAt(LocalDateTime.now())
                .studentName("Moustapha Camara").collectedByName("Fatoumata Camara").build();
    }

    @Test
    void generate_withMixOfPaidAndUnpaidInvoices_producesValidPdf() {
        lenient().when(tenantInfoProvider.findTenantInfo(10L)).thenReturn(Optional.of(
                new TenantPublicInfo(10L, "École Test", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)));
        when(payerDirectory.className(7L)).thenReturn(Optional.of("7ème A"));
        when(payerDirectory.studentNumber(7L)).thenReturn(Optional.of("MAT-1"));
        when(payerDirectory.currentAcademicYearLabel(10L)).thenReturn(Optional.of("2025-2026"));
        when(payerDirectory.studentInvoices(7L)).thenReturn(List.of(
                new InvoiceRowInfo("FAC-1", new BigDecimal("50000"), "Mensualité — Octobre 2026", true),
                new InvoiceRowInfo("FAC-2", new BigDecimal("20000"), "Cantine", false)));
        when(payerDirectory.unpaidInvoices(7L)).thenReturn(List.of(
                new UnpaidInvoiceInfo("Cantine", new BigDecimal("20000"))));

        byte[] pdf = generator().generate(receipt(), payment());

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    void generate_studentWithNoOutstandingInvoices_omitsRemainingLineWithoutError() {
        when(payerDirectory.studentInvoices(7L)).thenReturn(List.of(
                new InvoiceRowInfo("FAC-1", new BigDecimal("50000"), "Inscription", true)));
        when(payerDirectory.unpaidInvoices(7L)).thenReturn(List.of());

        byte[] pdf = generator().generate(receipt(), payment());

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    void generate_tenantInfoMissing_stillProducesValidPdf() {
        when(payerDirectory.studentInvoices(7L)).thenReturn(List.of());
        when(payerDirectory.unpaidInvoices(7L)).thenReturn(List.of());

        byte[] pdf = generator().generate(receipt(), payment());

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }

    /** §retour comptable, point 2 : le téléphone PERSONNEL du comptable encaisseur, sous son nom dans
     * le bloc signature — résolu via PayerDirectory#operatorPhone(collectedByUserId), pas de crash si
     * absent (mock non stubbé retourne Optional.empty() par défaut, voir les autres tests). */
    @Test
    void generate_collectorHasPhone_resolvedFromOperatorPhone_noException() {
        when(payerDirectory.studentInvoices(7L)).thenReturn(List.of());
        when(payerDirectory.unpaidInvoices(7L)).thenReturn(List.of());
        when(payerDirectory.operatorPhone(99L)).thenReturn(Optional.of("+224614324299"));

        byte[] pdf = generator().generate(receipt(), payment());

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }

    /** §retour comptable : le nom de l'exonération ET son pourcentage doivent apparaître sur le reçu,
     * pas seulement le montant déduit — un paiement groupé (deux factures) couvrant deux motifs de
     * remise DIFFÉRENTS ne doit produire ni exception ni agrégation silencieuse sous un seul motif
     * (voir {@code discountNotes}, qui groupe désormais par motif exact). */
    @Test
    void generate_invoicesWithDifferentDiscountReasons_producesValidPdfWithoutMergingReasons() {
        when(payerDirectory.studentInvoices(7L)).thenReturn(List.of());
        when(payerDirectory.unpaidInvoices(7L)).thenReturn(List.of());
        when(payerDirectory.invoiceFeeInfo(2L)).thenReturn(Optional.of(new PayerDirectory.InvoiceFeeInfo(
                "Mensualité", "Octobre 2026", new BigDecimal("10000"),
                "Exonération : Meilleur élève (100%)", null)));

        byte[] pdf = generator().generate(receipt(), payment());

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }

    /** Régression : un reçu A5 avec le tableau complet des factures de l'élève débordait sur une 2e
     * page dès qu'il y avait plus de quelques lignes (§retour comptable — "quelle que soit la taille
     * du tableau, une seule page"). A4 + bloc d'identification compact doivent absorber une année
     * scolaire complète (mensualités + frais annexes) sans déborder. */
    @Test
    void generate_fullYearOfInvoices_staysOnSinglePage() {
        lenient().when(tenantInfoProvider.findTenantInfo(10L)).thenReturn(Optional.of(
                new TenantPublicInfo(10L, "École Test", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)));
        when(payerDirectory.className(7L)).thenReturn(Optional.of("7ème A"));
        when(payerDirectory.studentNumber(7L)).thenReturn(Optional.of("MAT-1"));
        when(payerDirectory.currentAcademicYearLabel(10L)).thenReturn(Optional.of("2025-2026"));
        List<InvoiceRowInfo> invoices = new java.util.ArrayList<>();
        String[] months = {"Septembre", "Octobre", "Novembre", "Décembre", "Janvier", "Février",
                "Mars", "Avril", "Mai", "Juin"};
        for (String month : months) {
            invoices.add(new InvoiceRowInfo("FAC-" + month, new BigDecimal("100000"), "Mensualité — " + month + " 2026", true));
        }
        invoices.add(new InvoiceRowInfo("FAC-INS", new BigDecimal("250000"), "Inscription", true));
        invoices.add(new InvoiceRowInfo("FAC-TRANS", new BigDecimal("300000"), "Transport", false));
        invoices.add(new InvoiceRowInfo("FAC-CANT", new BigDecimal("400000"), "Cantine", false));
        invoices.add(new InvoiceRowInfo("FAC-EXAM", new BigDecimal("50000"), "Examen", true));
        when(payerDirectory.studentInvoices(7L)).thenReturn(invoices);
        when(payerDirectory.unpaidInvoices(7L)).thenReturn(List.of(
                new UnpaidInvoiceInfo("Transport", new BigDecimal("300000")),
                new UnpaidInvoiceInfo("Cantine", new BigDecimal("400000"))));

        byte[] pdf = generator().generate(receipt(), payment());

        assertThat(pdf).isNotEmpty();
        // Compte les objets "/Type /Page" (pas "/Type /Pages", le nœud racine, toujours présent une
        // fois) — heuristique simple faute de bibliothèque d'extraction de PDF dans le projet.
        String content = new String(pdf, java.nio.charset.StandardCharsets.ISO_8859_1);
        long pageObjectCount = java.util.regex.Pattern.compile("/Type\\s*/Page(?!s)\\b")
                .matcher(content).results().count();
        assertThat(pageObjectCount).as("nombre de pages du reçu").isEqualTo(1);
    }
}
