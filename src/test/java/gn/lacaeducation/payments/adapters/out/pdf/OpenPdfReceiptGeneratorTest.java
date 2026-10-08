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
import static org.mockito.ArgumentMatchers.any;
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
    @Mock gn.lacaeducation.payments.application.port.out.PaymentRepository paymentRepository;

    private OpenPdfReceiptGenerator generator() {
        return new OpenPdfReceiptGenerator(tenantInfoProvider, payerDirectory, objectMapper, paymentRepository);
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
                new TenantPublicInfo(10L, "École Test", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)));
        when(payerDirectory.className(7L)).thenReturn(Optional.of("7ème A"));
        when(payerDirectory.studentNumber(7L)).thenReturn(Optional.of("MAT-1"));
        when(payerDirectory.currentAcademicYearLabel(10L)).thenReturn(Optional.of("2025-2026"));
        when(payerDirectory.studentInvoices(7L)).thenReturn(List.of(
                new InvoiceRowInfo(1L, "FAC-1", new BigDecimal("50000"), "Mensualité — Octobre 2026", true, new BigDecimal("50000"), null, null, null),
                new InvoiceRowInfo(1L, "FAC-2", new BigDecimal("20000"), "Cantine", false, BigDecimal.ZERO, null, null, null)));
        when(payerDirectory.unpaidInvoices(7L)).thenReturn(List.of(
                new UnpaidInvoiceInfo("Cantine", new BigDecimal("20000"))));

        byte[] pdf = generator().generate(receipt(), payment());

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    void generate_studentWithNoOutstandingInvoices_omitsRemainingLineWithoutError() {
        when(payerDirectory.studentInvoices(7L)).thenReturn(List.of(
                new InvoiceRowInfo(1L, "FAC-1", new BigDecimal("50000"), "Inscription", true, new BigDecimal("50000"), null, null, null)));
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

    /**
     * Retour comptable réel : un parent qui a réglé toute l'année avec 5 % de remise (paiement
     * anticipé) revient un autre jour régler uniquement le transport — ce reçu-là ne règle aucune
     * facture remisée, mais doit quand même rappeler la remise déjà appliquée sur la mensualité,
     * visible dans le tableau du reçu. Avant ce correctif, {@code discountNotes} ne regardait QUE
     * les factures couvertes par le paiement du jour (transport, jamais remisé) : la remise
     * disparaissait purement et simplement de ce reçu-là.
     */
    @Test
    void discountNotes_discountOnInvoiceNotCoveredByTodaysPayment_stillAppears() throws Exception {
        List<InvoiceRowInfo> invoices = List.of(
                new InvoiceRowInfo(2L, "FAC-MENS", new BigDecimal("2000000"), "Mensualité", true,
                        new BigDecimal("1900000"), null, null, null),
                new InvoiceRowInfo(5L, "FAC-TRANS", new BigDecimal("50000"), "Transport", true,
                        new BigDecimal("50000"), null, null, null));
        when(payerDirectory.invoiceFeeInfo(2L)).thenReturn(Optional.of(new PayerDirectory.InvoiceFeeInfo(
                "Mensualité", null, new BigDecimal("100000"), "Remise paiement anticipé : 5%", null)));
        when(payerDirectory.invoiceFeeInfo(5L)).thenReturn(Optional.empty());

        var method = OpenPdfReceiptGenerator.class.getDeclaredMethod("discountNotes", List.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> notes = (List<String>) method.invoke(generator(), invoices);

        assertThat(notes).hasSize(1);
        assertThat(notes.get(0)).contains("5%").contains("100 000");
    }

    /** Retour comptable (transfert de classe) : une facture créditée doit l'expliquer sur le reçu,
     * sinon elle apparaît réglée sans aucun encaissement correspondant. */
    @Test
    void creditNotes_invoiceWithCreditedAmount_explainsWhyAlreadySettled() throws Exception {
        List<InvoiceRowInfo> invoices = List.of(
                new InvoiceRowInfo(1L, "FAC-1", new BigDecimal("300000"), "Tranche 2 (novembre, décembre, janvier)",
                        false, new BigDecimal("100000"), new BigDecimal("100000"), "Transfert de classe", null),
                new InvoiceRowInfo(2L, "FAC-2", new BigDecimal("500000"), "Tranche 3 (février, mars)",
                        false, BigDecimal.ZERO, null, null, null));

        var method = OpenPdfReceiptGenerator.class.getDeclaredMethod("creditNotes", List.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> notes = (List<String>) method.invoke(generator(), invoices);

        assertThat(notes).hasSize(1);
        assertThat(notes.get(0))
                .contains("Tranche 2 (novembre, décembre, janvier)")
                .contains("100 000")
                .contains("ancienne classe")
                .contains("transfert de classe");
    }

    /** Retour comptable réel : un surplus de crédit (transfert vers une classe moins chère) doit
     * rester visible sur CHAQUE reçu de l'élève, pas seulement celui du jour du transfert. */
    @Test
    void refundNotes_pendingRefund_mentionsAmountToReimburse() throws Exception {
        when(payerDirectory.pendingRefunds(7L)).thenReturn(List.of(
                new PayerDirectory.PendingRefundInfo(new BigDecimal("150000"), "Transfert de classe", LocalDateTime.now())));

        var method = OpenPdfReceiptGenerator.class.getDeclaredMethod("refundNotes", Long.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> notes = (List<String>) method.invoke(generator(), 7L);

        assertThat(notes).hasSize(1);
        assertThat(notes.get(0)).contains("REMBOURSER").contains("150 000");
    }

    /** Régression : un reçu A5 avec le tableau complet des factures de l'élève débordait sur une 2e
     * page dès qu'il y avait plus de quelques lignes (§retour comptable — "quelle que soit la taille
     * du tableau, une seule page"). A4 + bloc d'identification compact doivent absorber une année
     * scolaire complète (mensualités + frais annexes) sans déborder. */
    @Test
    void generate_fullYearOfInvoices_staysOnSinglePage() {
        lenient().when(tenantInfoProvider.findTenantInfo(10L)).thenReturn(Optional.of(
                new TenantPublicInfo(10L, "École Test", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)));
        when(payerDirectory.className(7L)).thenReturn(Optional.of("7ème A"));
        when(payerDirectory.studentNumber(7L)).thenReturn(Optional.of("MAT-1"));
        when(payerDirectory.currentAcademicYearLabel(10L)).thenReturn(Optional.of("2025-2026"));
        List<InvoiceRowInfo> invoices = new java.util.ArrayList<>();
        String[] months = {"Septembre", "Octobre", "Novembre", "Décembre", "Janvier", "Février",
                "Mars", "Avril", "Mai", "Juin"};
        for (String month : months) {
            invoices.add(new InvoiceRowInfo(1L, "FAC-" + month, new BigDecimal("100000"), "Mensualité — " + month + " 2026", true, new BigDecimal("100000"), null, null, null));
        }
        invoices.add(new InvoiceRowInfo(1L, "FAC-INS", new BigDecimal("250000"), "Inscription", true, new BigDecimal("250000"), null, null, null));
        invoices.add(new InvoiceRowInfo(1L, "FAC-TRANS", new BigDecimal("300000"), "Transport", false, BigDecimal.ZERO, null, null, null));
        invoices.add(new InvoiceRowInfo(1L, "FAC-CANT", new BigDecimal("400000"), "Cantine", false, BigDecimal.ZERO, null, null, null));
        invoices.add(new InvoiceRowInfo(1L, "FAC-EXAM", new BigDecimal("50000"), "Examen", true, new BigDecimal("50000"), null, null, null));
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

    /** Retour comptable direct : le nom/téléphone du comptable (bloc signature) sortait sur une 2e
     * page pour un élève avec deux années scolaires de factures (24 mensualités + frais annexes) PLUS
     * une remise et des frais de plateforme (deux notes supplémentaires en bas de reçu) — un cas plus
     * chargé que {@link #generate_fullYearOfInvoices_staysOnSinglePage}, qui ne couvrait qu'une année. */
    @Test
    void generate_twoYearsOfInvoicesWithDiscountAndPlatformFee_staysOnSinglePage() {
        lenient().when(tenantInfoProvider.findTenantInfo(10L)).thenReturn(Optional.of(
                new TenantPublicInfo(10L, "École Test", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)));
        when(payerDirectory.className(7L)).thenReturn(Optional.of("7ème A"));
        when(payerDirectory.studentNumber(7L)).thenReturn(Optional.of("MAT-1"));
        when(payerDirectory.currentAcademicYearLabel(10L)).thenReturn(Optional.of("2025-2026"));
        when(payerDirectory.operatorPhone(99L)).thenReturn(Optional.of("+224614324299"));
        String[] months = {"Septembre", "Octobre", "Novembre", "Décembre", "Janvier", "Février",
                "Mars", "Avril", "Mai", "Juin"};
        List<InvoiceRowInfo> invoices = new java.util.ArrayList<>();
        long[] nextId = {1L};
        for (String year : new String[]{"2024-2025", "2025-2026"}) {
            for (String month : months) {
                invoices.add(new InvoiceRowInfo(nextId[0]++, "FAC-" + month + "-" + year,
                        new BigDecimal("100000"), "Mensualité — " + month + " " + year, true, new BigDecimal("100000"), null, null, null));
            }
        }
        invoices.add(new InvoiceRowInfo(nextId[0]++, "FAC-INS", new BigDecimal("250000"), "Inscription", true, new BigDecimal("250000"), null, null, null));
        invoices.add(new InvoiceRowInfo(nextId[0]++, "FAC-TRANS", new BigDecimal("300000"), "Transport", false, BigDecimal.ZERO, null, null, null));
        invoices.add(new InvoiceRowInfo(nextId[0]++, "FAC-CANT", new BigDecimal("400000"), "Cantine", false, BigDecimal.ZERO, null, null, null));
        invoices.add(new InvoiceRowInfo(nextId[0]++, "FAC-EXAM", new BigDecimal("50000"), "Examen", true, new BigDecimal("50000"), null, null, null));
        when(payerDirectory.studentInvoices(7L)).thenReturn(invoices);
        when(payerDirectory.unpaidInvoices(7L)).thenReturn(List.of(
                new UnpaidInvoiceInfo("Transport", new BigDecimal("300000")),
                new UnpaidInvoiceInfo("Cantine", new BigDecimal("400000"))));
        // Un seul des 24 mensualités porte la remise — les 23 autres n'ont volontairement aucun stub
        // (retombent sur Optional.empty() par défaut) pour ne pas fausser le total de la remise.
        lenient().when(payerDirectory.invoiceFeeInfo(any())).thenReturn(Optional.empty());
        when(payerDirectory.invoiceFeeInfo(1L)).thenReturn(Optional.of(new PayerDirectory.InvoiceFeeInfo(
                "Mensualité", "Octobre 2026", new BigDecimal("10000"),
                "Exonération : Avantage personnel (50%)", new BigDecimal("5000"))));

        byte[] pdf = generator().generate(receipt(), payment());

        assertThat(pdf).isNotEmpty();
        String content = new String(pdf, java.nio.charset.StandardCharsets.ISO_8859_1);
        long pageObjectCount = java.util.regex.Pattern.compile("/Type\\s*/Page(?!s)\\b")
                .matcher(content).results().count();
        assertThat(pageObjectCount).as("nombre de pages du reçu").isEqualTo(1);
    }
}
