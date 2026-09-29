package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.event.InvoiceGeneratedEvent;
import gn.lacaeducation.billing.application.event.InvoiceIssuedEvent;
import gn.lacaeducation.billing.application.event.InvoicePaidEvent;
import gn.lacaeducation.billing.application.port.in.InvoiceUseCase.CreateInvoiceCommand;
import gn.lacaeducation.billing.application.port.out.*;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.InvoiceStatus;
import gn.lacaeducation.common.MediaUploadService;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.billing.application.port.in.ExemptionUseCase;
import gn.lacaeducation.billing.application.port.in.ExemptionUseCase.ApplicableExemption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock InvoiceRepository repo;
    @Mock SchoolFeeRepository schoolFeeRepository;
    @Mock EnrollmentQueryPort enrollmentQueryPort;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock InstallmentRepository installmentRepository;
    @Mock InvoicePdfGenerator invoicePdfGenerator;
    @Mock MediaUploadService mediaUploadService;
    @Mock TenantInfoProvider tenantInfoProvider;
    @Mock ExemptionUseCase exemptionUseCase;
    @Mock gn.lacaeducation.billing.application.port.in.EarlyPaymentDiscountUseCase earlyPaymentDiscountUseCase;
    @Mock gn.lacaeducation.billing.application.port.in.StaffChildBenefitUseCase staffChildBenefitUseCase;
    @Mock gn.lacaeducation.platform.PlatformFeeLedgerApi platformFeeLedgerApi;

    @BeforeEach
    void defaultNoExemption() {
        // La plupart des tests ne s'intéressent pas à l'exonération : réponse neutre par défaut
        // (lenient — certains tests, ex. applyPayment/*, n'atteignent jamais ce calcul).
        lenient().when(exemptionUseCase.computeApplicable(any(), any()))
                .thenReturn(new ApplicableExemption(BigDecimal.ZERO, null, false, BigDecimal.ZERO, null));
        lenient().when(staffChildBenefitUseCase.isStaffChild(any())).thenReturn(false);
    }

    InvoiceService service() {
        return new InvoiceService(repo, schoolFeeRepository, installmentRepository, enrollmentQueryPort, eventPublisher, invoicePdfGenerator, mediaUploadService, tenantInfoProvider, exemptionUseCase, earlyPaymentDiscountUseCase, staffChildBenefitUseCase, platformFeeLedgerApi);

    }

    @Test
    void generateMonthly_appliesExemptionOnEachInstallment() {
        when(enrollmentQueryPort.hasActiveEnrollment(7L)).thenReturn(true);
        when(installmentRepository.findByFeeId(3L)).thenReturn(java.util.List.of(
                gn.lacaeducation.billing.domain.Installment.builder()
                        .id(1L).feeId(3L).label("Septembre").amount(new BigDecimal("100")).build()));
        when(repo.existsByStudentIdAndInstallmentId(7L, 1L)).thenReturn(false);
        when(exemptionUseCase.computeApplicable(7L, gn.lacaeducation.billing.domain.FeeType.MONTHLY))
                .thenReturn(new ApplicableExemption(new BigDecimal("5"), "Cas social", false, BigDecimal.ZERO, null));
        when(repo.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        var invoices = service().generateMonthly(
                new gn.lacaeducation.billing.application.port.in.InvoiceUseCase.GenerateMonthlyCommand(1L, 7L, 3L, 99L)).created();

        assertThat(invoices).hasSize(1);
        Invoice inv = invoices.get(0);
        assertThat(inv.getDiscountAmount()).isEqualByComparingTo("5.00");
        assertThat(inv.getRemainingBalance()).isEqualByComparingTo("95.00");
        assertThat(inv.getDiscountReason()).contains("Cas social");
    }

    @Test
    void generateMonthly_transportFee_usesTransportFeeTypeNotHardcodedMonthlyForDiscount() {
        // Régression : generateMonthly() est aussi appelée pour les frais TRANSPORT (voir
        // TransportInvoiceListener) — le calcul de remise doit utiliser le VRAI type du frais, pas
        // FeeType.MONTHLY codé en dur, sinon une exonération spécifique au transport ne s'applique
        // jamais (et une exonération spécifique à la mensualité s'appliquerait à tort au transport).
        when(enrollmentQueryPort.hasActiveEnrollment(7L)).thenReturn(true);
        when(schoolFeeRepository.findById(3L)).thenReturn(Optional.of(
                gn.lacaeducation.billing.domain.SchoolFee.builder()
                        .id(3L).type(gn.lacaeducation.billing.domain.FeeType.TRANSPORT).build()));
        when(installmentRepository.findByFeeId(3L)).thenReturn(java.util.List.of(
                gn.lacaeducation.billing.domain.Installment.builder()
                        .id(1L).feeId(3L).label("Septembre").amount(new BigDecimal("50")).build()));
        when(repo.existsByStudentIdAndInstallmentId(7L, 1L)).thenReturn(false);
        when(exemptionUseCase.computeApplicable(7L, gn.lacaeducation.billing.domain.FeeType.TRANSPORT))
                .thenReturn(new ApplicableExemption(new BigDecimal("10"), "Exonération transport", false, BigDecimal.ZERO, null));
        when(repo.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        var invoices = service().generateMonthly(
                new gn.lacaeducation.billing.application.port.in.InvoiceUseCase.GenerateMonthlyCommand(1L, 7L, 3L, 99L)).created();

        assertThat(invoices).hasSize(1);
        assertThat(invoices.get(0).getDiscountReason()).contains("Exonération transport");
        verify(exemptionUseCase, never()).computeApplicable(7L, gn.lacaeducation.billing.domain.FeeType.MONTHLY);
    }

    @Test
    void generateMonthly_withoutAcademicYearId_resolvesFromActiveEnrollment() {
        // Régression : un appelant qui omet l'année scolaire (ex. facturation transport
        // auto-générée) ne doit plus produire une facture avec `academic_year_id = NULL` — invisible
        // des tableaux de bord qui filtrent strictement par année (Vision Fondateur).
        when(enrollmentQueryPort.hasActiveEnrollment(7L)).thenReturn(true);
        when(enrollmentQueryPort.findActiveAcademicYearId(7L)).thenReturn(42L);
        when(installmentRepository.findByFeeId(3L)).thenReturn(java.util.List.of(
                gn.lacaeducation.billing.domain.Installment.builder()
                        .id(1L).feeId(3L).label("Septembre").amount(new BigDecimal("100")).build()));
        when(repo.existsByStudentIdAndInstallmentId(7L, 1L)).thenReturn(false);
        when(repo.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        var invoices = service().generateMonthly(
                new gn.lacaeducation.billing.application.port.in.InvoiceUseCase.GenerateMonthlyCommand(1L, 7L, 3L, null)).created();

        assertThat(invoices).hasSize(1);
        assertThat(invoices.get(0).getAcademicYearId()).isEqualTo(42L);
    }

    @Test
    void create_initialisesUnpaidWithFullRemainingBalanceAndPublishesEvents() {
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            i.setId(1L);
            return i;
        });

        Invoice inv = service().create(new CreateInvoiceCommand(10L, 7L, 3L, "FAC-1", new BigDecimal("100.00"), 99L));

        assertThat(inv.getStatus()).isEqualTo(InvoiceStatus.UNPAID);
        assertThat(inv.getPaidAmount()).isEqualByComparingTo("0");
        assertThat(inv.getRemainingBalance()).isEqualByComparingTo("100.00");
        verify(eventPublisher).publishEvent(any(InvoiceGeneratedEvent.class));
        verify(eventPublisher).publishEvent(any(InvoiceIssuedEvent.class));
    }

    @Test
    void create_autoGeneratesTenantScopedReferenceWhenBlank() {
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice inv = service().create(new CreateInvoiceCommand(42L, 7L, 3L, null, new BigDecimal("50.00"), 99L));

        // Référence au format <codeÉcole>-FAC-<année>-<suffixe>, ex. EC-FAC-2026-4EAF7F.
        assertThat(inv.getReference()).matches("[A-Z]{2}-FAC-\\d{4}-.*");
    }

    @Test
    void applyPayment_partial_setsPartialStatusAndRecomputesBalance() {
        Invoice existing = Invoice.builder().id(1L).tenantId(10L).studentId(7L)
                .totalAmount(new BigDecimal("100.00")).paidAmount(BigDecimal.ZERO)
                .remainingBalance(new BigDecimal("100.00")).status(InvoiceStatus.UNPAID).build();
        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice inv = service().applyPayment(1L, new BigDecimal("30.00"));

        assertThat(inv.getStatus()).isEqualTo(InvoiceStatus.PARTIAL);
        assertThat(inv.getRemainingBalance()).isEqualByComparingTo("70.00");
        verify(eventPublisher, never()).publishEvent(any(InvoicePaidEvent.class));
    }

    @Test
    void applyPayment_full_setsPaidAndPublishesInvoicePaidEvent() {
        Invoice existing = Invoice.builder().id(1L).tenantId(10L).studentId(7L).reference("FAC-1")
                .totalAmount(new BigDecimal("100.00")).paidAmount(new BigDecimal("40.00"))
                .remainingBalance(new BigDecimal("60.00")).status(InvoiceStatus.PARTIAL).build();
        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice inv = service().applyPayment(1L, new BigDecimal("60.00"));

        assertThat(inv.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(inv.getRemainingBalance()).isEqualByComparingTo("0");
        ArgumentCaptor<InvoicePaidEvent> captor = ArgumentCaptor.forClass(InvoicePaidEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().invoiceId()).isEqualTo(1L);
        assertThat(captor.getValue().reference()).isEqualTo("FAC-1");
    }

    @Test
    void reversePayment_reducesPaidAmountAndRevertsStatusToPartial() {
        // Régression : le remboursement doit se répercuter sur la facture (avant ce correctif, seul
        // le statut du Payment changeait, la facture restait affichée soldée).
        Invoice existing = Invoice.builder().id(1L).tenantId(10L).studentId(7L)
                .totalAmount(new BigDecimal("100.00")).paidAmount(new BigDecimal("100.00"))
                .remainingBalance(BigDecimal.ZERO).status(InvoiceStatus.PAID).build();
        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice inv = service().reversePayment(1L, new BigDecimal("40.00"));

        assertThat(inv.getPaidAmount()).isEqualByComparingTo("60.00");
        assertThat(inv.getRemainingBalance()).isEqualByComparingTo("40.00");
        assertThat(inv.getStatus()).isEqualTo(InvoiceStatus.PARTIAL);
        // Ne doit publier aucun événement de notification (ne doit pas envoyer un SMS « paiement reçu »
        // au parent alors qu'il s'agit d'un remboursement).
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void reversePayment_neverGoesBelowZero() {
        Invoice existing = Invoice.builder().id(1L).tenantId(10L).studentId(7L)
                .totalAmount(new BigDecimal("100.00")).paidAmount(new BigDecimal("30.00"))
                .remainingBalance(new BigDecimal("70.00")).status(InvoiceStatus.PARTIAL).build();
        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice inv = service().reversePayment(1L, new BigDecimal("999.00"));

        assertThat(inv.getPaidAmount()).isEqualByComparingTo("0");
        assertThat(inv.getStatus()).isEqualTo(InvoiceStatus.UNPAID);
    }

    @Test
    void voidInvoice_setsCancelledStatusAndZeroesBalanceButKeepsHistory() {
        Invoice existing = Invoice.builder().id(1L).tenantId(10L).studentId(7L)
                .totalAmount(new BigDecimal("100.00")).paidAmount(new BigDecimal("40.00"))
                .remainingBalance(new BigDecimal("60.00")).status(InvoiceStatus.PARTIAL).build();
        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice inv = service().voidInvoice(1L);

        assertThat(inv.getStatus()).isEqualTo(InvoiceStatus.CANCELLED);
        assertThat(inv.getRemainingBalance()).isEqualByComparingTo("0");
        // L'historique (montant facturé/payé) reste inchangé pour l'audit.
        assertThat(inv.getPaidAmount()).isEqualByComparingTo("40.00");
        assertThat(inv.getTotalAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void applyCredit_increasesPaidAmountAndRecomputesBalance() {
        Invoice existing = Invoice.builder().id(1L).tenantId(10L).studentId(7L)
                .totalAmount(new BigDecimal("100.00")).paidAmount(BigDecimal.ZERO)
                .remainingBalance(new BigDecimal("100.00")).status(InvoiceStatus.UNPAID).build();
        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice inv = service().applyCredit(1L, new BigDecimal("60.00"));

        assertThat(inv.getPaidAmount()).isEqualByComparingTo("60.00");
        assertThat(inv.getRemainingBalance()).isEqualByComparingTo("40.00");
        assertThat(inv.getStatus()).isEqualTo(InvoiceStatus.PARTIAL);
        // Report de crédit, pas un nouveau paiement : aucune notification au parent.
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void applyPayment_withPaidMonths_mergesIntoInvoicePaidMonths() {
        // Facture multi-mois (tranche) réglée en une fois : les mois cochés au guichet doivent se
        // retrouver sur la facture, pas seulement le montant global.
        Invoice existing = Invoice.builder().id(1L).tenantId(10L).studentId(7L)
                .totalAmount(new BigDecimal("90.00")).paidAmount(BigDecimal.ZERO)
                .remainingBalance(new BigDecimal("90.00")).status(InvoiceStatus.UNPAID).build();
        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice inv = service().applyPayment(1L, new BigDecimal("60.00"), false, java.util.List.of("2026-03", "2026-04"));

        assertThat(inv.getPaidMonths()).containsExactly("2026-03", "2026-04");
        assertThat(inv.getStatus()).isEqualTo(InvoiceStatus.PARTIAL);
    }

    @Test
    void applyPayment_withPaidMonths_accumulatesAcrossSuccessivePayments() {
        // Un deuxième paiement partiel sur la même tranche (ex. le mois restant réglé plus tard) doit
        // AJOUTER au mois déjà payé, jamais l'écraser.
        Invoice existing = Invoice.builder().id(1L).tenantId(10L).studentId(7L)
                .totalAmount(new BigDecimal("90.00")).paidAmount(new BigDecimal("60.00"))
                .remainingBalance(new BigDecimal("30.00")).status(InvoiceStatus.PARTIAL)
                .paidMonths(new java.util.ArrayList<>(java.util.List.of("2026-03", "2026-04")))
                .build();
        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice inv = service().applyPayment(1L, new BigDecimal("30.00"), false, java.util.List.of("2026-05"));

        assertThat(inv.getPaidMonths()).containsExactly("2026-03", "2026-04", "2026-05");
        assertThat(inv.getStatus()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    void applyPayment_withoutPaidMonths_leavesPaidMonthsUnchanged() {
        // Facture d'un seul mois (pas de tranche) : paidMonths reste null, comme avant ce correctif.
        Invoice existing = Invoice.builder().id(1L).tenantId(10L).studentId(7L)
                .totalAmount(new BigDecimal("30.00")).paidAmount(BigDecimal.ZERO)
                .remainingBalance(new BigDecimal("30.00")).status(InvoiceStatus.UNPAID).build();
        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice inv = service().applyPayment(1L, new BigDecimal("30.00"));

        assertThat(inv.getPaidMonths()).isNull();
    }

    @Test
    void generateMonthly_monthlyPaymentOptIn_splitsMultiMonthInstallmentIntoOneInvoicePerMonth() {
        // Ce parent règle mois par mois (§billing) : une tranche de 3 mois (300) doit produire 3
        // factures de 100, une par mois, au lieu d'une seule facture groupée de 300.
        when(enrollmentQueryPort.hasActiveEnrollment(7L)).thenReturn(true);
        when(enrollmentQueryPort.hasMonthlyPaymentOptIn(7L, 99L)).thenReturn(true);
        when(installmentRepository.findByFeeId(3L)).thenReturn(java.util.List.of(
                gn.lacaeducation.billing.domain.Installment.builder()
                        .id(1L).feeId(3L).label("Trimestre 1").amount(new BigDecimal("300"))
                        .months(java.util.List.of("2026-09", "2026-10", "2026-11")).build()));
        when(repo.existsByStudentIdAndInstallmentIdAndPeriodLabel(eq(7L), eq(1L), any())).thenReturn(false);
        when(repo.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        var invoices = service().generateMonthly(
                new gn.lacaeducation.billing.application.port.in.InvoiceUseCase.GenerateMonthlyCommand(1L, 7L, 3L, 99L)).created();

        assertThat(invoices).hasSize(3);
        assertThat(invoices).extracting(Invoice::getTotalAmount)
                .allSatisfy(a -> assertThat(a).isEqualByComparingTo("100.00"));
        assertThat(invoices).extracting(Invoice::getPeriodLabel)
                .containsExactly("Septembre 2026", "Octobre 2026", "Novembre 2026");
        assertThat(invoices).allMatch(inv -> inv.getInstallmentId().equals(1L));
    }

    @Test
    void generateMonthly_withoutMonthlyPaymentOptIn_keepsOneInvoicePerInstallment() {
        // Comportement inchangé pour les autres familles de la classe (pas d'opt-in).
        when(enrollmentQueryPort.hasActiveEnrollment(7L)).thenReturn(true);
        when(enrollmentQueryPort.hasMonthlyPaymentOptIn(7L, 99L)).thenReturn(false);
        when(installmentRepository.findByFeeId(3L)).thenReturn(java.util.List.of(
                gn.lacaeducation.billing.domain.Installment.builder()
                        .id(1L).feeId(3L).label("Trimestre 1").amount(new BigDecimal("300"))
                        .months(java.util.List.of("2026-09", "2026-10", "2026-11")).build()));
        when(repo.existsByStudentIdAndInstallmentId(7L, 1L)).thenReturn(false);
        when(repo.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        var invoices = service().generateMonthly(
                new gn.lacaeducation.billing.application.port.in.InvoiceUseCase.GenerateMonthlyCommand(1L, 7L, 3L, 99L)).created();

        assertThat(invoices).hasSize(1);
        assertThat(invoices.get(0).getTotalAmount()).isEqualByComparingTo("300");
        assertThat(invoices.get(0).getPeriodLabel()).isEqualTo("Trimestre 1");
    }

    // ── generateNextInstallment (§billing, retour comptable) : transport facturé au mois, à la
    // demande — jamais toute l'année d'un coup comme generateMonthly ────────────────────────────────

    @Test
    void generateNextInstallment_generatesOnlyTheEarliestUnbilledOne() {
        when(enrollmentQueryPort.hasActiveEnrollment(7L)).thenReturn(true);
        when(installmentRepository.findByFeeId(9L)).thenReturn(java.util.List.of(
                gn.lacaeducation.billing.domain.Installment.builder()
                        .id(2L).feeId(9L).label("Octobre").amount(new BigDecimal("30000"))
                        .dueDate(java.time.LocalDate.of(2026, 10, 5)).build(),
                gn.lacaeducation.billing.domain.Installment.builder()
                        .id(1L).feeId(9L).label("Septembre").amount(new BigDecimal("30000"))
                        .dueDate(java.time.LocalDate.of(2026, 9, 5)).build()));
        // Septembre déjà facturé (mois précédent), Octobre pas encore : c'est Octobre qui doit sortir.
        when(repo.existsByStudentIdAndInstallmentId(7L, 1L)).thenReturn(true);
        when(repo.existsByStudentIdAndInstallmentId(7L, 2L)).thenReturn(false);
        when(repo.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        var result = service().generateNextInstallment(
                new gn.lacaeducation.billing.application.port.in.InvoiceUseCase.GenerateMonthlyCommand(1L, 7L, 9L, 99L));

        assertThat(result).isPresent();
        assertThat(result.get().getPeriodLabel()).isEqualTo("Octobre");
        assertThat(result.get().getTotalAmount()).isEqualByComparingTo("30000");
    }

    @Test
    void generateNextInstallment_everythingAlreadyBilled_returnsEmpty() {
        when(enrollmentQueryPort.hasActiveEnrollment(7L)).thenReturn(true);
        when(installmentRepository.findByFeeId(9L)).thenReturn(java.util.List.of(
                gn.lacaeducation.billing.domain.Installment.builder()
                        .id(1L).feeId(9L).label("Septembre").amount(new BigDecimal("30000"))
                        .dueDate(java.time.LocalDate.of(2026, 9, 5)).build()));
        when(repo.existsByStudentIdAndInstallmentId(7L, 1L)).thenReturn(true);

        var result = service().generateNextInstallment(
                new gn.lacaeducation.billing.application.port.in.InvoiceUseCase.GenerateMonthlyCommand(1L, 7L, 9L, 99L));

        assertThat(result).isEmpty();
        verify(repo, never()).save(any());
    }

    @Test
    void generateNextInstallment_noInstallmentsConfigured_throwsBusinessRule() {
        when(enrollmentQueryPort.hasActiveEnrollment(7L)).thenReturn(true);
        when(installmentRepository.findByFeeId(9L)).thenReturn(java.util.List.of());

        assertThatThrownBy(() -> service().generateNextInstallment(
                new gn.lacaeducation.billing.application.port.in.InvoiceUseCase.GenerateMonthlyCommand(1L, 7L, 9L, 99L)))
                .isInstanceOf(gn.lacaeducation.common.BusinessRuleException.class);
    }

    @Test
    void applyPayment_recordsPlatformFeeLedger_whenFeeTypeIsPlatform() {
        // Frais plateforme (§billing) : désormais un frais annexe comme un autre, configuré librement
        // par l'école (FeeType.PLATFORM) — tout encaissement dessus alimente le solde dû à l'éditeur.
        Invoice existing = Invoice.builder().id(1L).tenantId(10L).studentId(7L).feeId(3L)
                .totalAmount(new BigDecimal("500.00")).paidAmount(BigDecimal.ZERO)
                .remainingBalance(new BigDecimal("500.00")).status(InvoiceStatus.UNPAID).build();
        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(schoolFeeRepository.findById(3L)).thenReturn(Optional.of(
                gn.lacaeducation.billing.domain.SchoolFee.builder().id(3L)
                        .type(gn.lacaeducation.billing.domain.FeeType.PLATFORM).build()));
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        service().applyPayment(1L, new BigDecimal("60.00"));

        verify(platformFeeLedgerApi).record(10L, 1L, new BigDecimal("60.00"));
    }

    @Test
    void applyPayment_doesNotRecordPlatformFeeLedger_whenFeeTypeIsNotPlatform() {
        Invoice existing = Invoice.builder().id(1L).tenantId(10L).studentId(7L).feeId(3L)
                .totalAmount(new BigDecimal("100.00")).paidAmount(BigDecimal.ZERO)
                .remainingBalance(new BigDecimal("100.00")).status(InvoiceStatus.UNPAID).build();
        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(schoolFeeRepository.findById(3L)).thenReturn(Optional.of(
                gn.lacaeducation.billing.domain.SchoolFee.builder().id(3L)
                        .type(gn.lacaeducation.billing.domain.FeeType.MONTHLY).build()));
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        service().applyPayment(1L, new BigDecimal("30.00"));

        verify(platformFeeLedgerApi, never()).record(any(), any(), any());
    }

    @Test
    void applyPayment_doesNotRecordPlatformFeeLedger_whenInvoiceHasNoFeeId() {
        Invoice existing = Invoice.builder().id(1L).tenantId(10L).studentId(7L)
                .totalAmount(new BigDecimal("100.00")).paidAmount(BigDecimal.ZERO)
                .remainingBalance(new BigDecimal("100.00")).status(InvoiceStatus.UNPAID).build();
        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        service().applyPayment(1L, new BigDecimal("30.00"));

        verify(platformFeeLedgerApi, never()).record(any(), any(), any());
    }

    @Test
    void applyPayment_unknownInvoice_throwsNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().applyPayment(99L, BigDecimal.TEN))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void applyEarlyPaymentDiscount_appliesOnFullAnnualTotal_whenNothingPaidYet() {
        // Premier paiement de l'année : 3 tranches de 300 (annuel = 900), aucune encore payée. Le parent
        // choisit de tout régler d'un coup dès ce premier paiement → remise de 10% sur les 900 (=90),
        // répartie sur les 3 tranches (30 chacune).
        gn.lacaeducation.billing.domain.SchoolFee fee = gn.lacaeducation.billing.domain.SchoolFee.builder()
                .id(3L).tenantId(1L).type(gn.lacaeducation.billing.domain.FeeType.MONTHLY).build();
        when(schoolFeeRepository.findById(3L)).thenReturn(Optional.of(fee));

        Invoice unpaid1 = Invoice.builder().id(1L).feeId(3L).totalAmount(new BigDecimal("300.00")).paidAmount(BigDecimal.ZERO).status(InvoiceStatus.UNPAID).build();
        Invoice unpaid2 = Invoice.builder().id(2L).feeId(3L).totalAmount(new BigDecimal("300.00")).paidAmount(BigDecimal.ZERO).status(InvoiceStatus.UNPAID).build();
        Invoice unpaid3 = Invoice.builder().id(3L).feeId(3L).totalAmount(new BigDecimal("300.00")).paidAmount(BigDecimal.ZERO).status(InvoiceStatus.UNPAID).build();
        when(repo.findByStudentId(7L)).thenReturn(java.util.List.of(unpaid1, unpaid2, unpaid3));
        when(repo.findById(1L)).thenReturn(Optional.of(unpaid1));
        when(repo.findById(2L)).thenReturn(Optional.of(unpaid2));
        when(repo.findById(3L)).thenReturn(Optional.of(unpaid3));
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        when(earlyPaymentDiscountUseCase.computeApplicable(1L, gn.lacaeducation.billing.domain.FeeType.MONTHLY))
                .thenReturn(new gn.lacaeducation.billing.application.port.in.EarlyPaymentDiscountUseCase.ApplicableDiscount(
                        new BigDecimal("10"), "Paiement annuel"));

        var updated = service().applyEarlyPaymentDiscount(7L, 3L);

        assertThat(updated).hasSize(3);
        BigDecimal totalDiscount = updated.stream().map(Invoice::getDiscountAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalDiscount).isEqualByComparingTo("90.00");
    }

    @Test
    void applyEarlyPaymentDiscount_noDiscountIfAnyTrancheAlreadyPaid() {
        // Le parent a déjà réglé une tranche (même partiellement) sans remise, puis décide de payer le
        // reste de l'année d'un coup : trop tard pour la remise « paiement annuel » — elle ne récompense
        // que le choix fait dès le tout premier paiement de scolarité.
        gn.lacaeducation.billing.domain.SchoolFee fee = gn.lacaeducation.billing.domain.SchoolFee.builder()
                .id(3L).tenantId(1L).type(gn.lacaeducation.billing.domain.FeeType.MONTHLY).build();
        when(schoolFeeRepository.findById(3L)).thenReturn(Optional.of(fee));

        Invoice paid = Invoice.builder().id(1L).feeId(3L).totalAmount(new BigDecimal("300.00"))
                .paidAmount(new BigDecimal("300.00")).status(InvoiceStatus.PAID).build();
        Invoice unpaid1 = Invoice.builder().id(2L).feeId(3L).totalAmount(new BigDecimal("300.00")).paidAmount(BigDecimal.ZERO).status(InvoiceStatus.UNPAID).build();
        Invoice unpaid2 = Invoice.builder().id(3L).feeId(3L).totalAmount(new BigDecimal("300.00")).paidAmount(BigDecimal.ZERO).status(InvoiceStatus.UNPAID).build();
        when(repo.findByStudentId(7L)).thenReturn(java.util.List.of(paid, unpaid1, unpaid2));

        var updated = service().applyEarlyPaymentDiscount(7L, 3L);

        assertThat(updated).extracting(Invoice::getDiscountAmount).allMatch(d -> d == null);
        verify(earlyPaymentDiscountUseCase, never()).computeApplicable(any(), any());
        verify(repo, never()).save(any());
    }

    @Test
    void removeEarlyPaymentDiscount_revertsDiscountedInvoicesToOriginalAmount() {
        // Régression : un parent coche "toute la scolarité" (remise appliquée), puis décoche un mois —
        // la remise ne doit plus rester acquise, les factures doivent revenir à leur montant initial.
        Invoice discounted1 = Invoice.builder().id(1L).feeId(3L).totalAmount(new BigDecimal("300.00"))
                .paidAmount(BigDecimal.ZERO).discountAmount(new BigDecimal("30.00")).discountReason("Paiement annuel (10%)")
                .status(InvoiceStatus.UNPAID).build();
        Invoice discounted2 = Invoice.builder().id(2L).feeId(3L).totalAmount(new BigDecimal("300.00"))
                .paidAmount(BigDecimal.ZERO).discountAmount(new BigDecimal("30.00")).discountReason("Paiement annuel (10%)")
                .status(InvoiceStatus.UNPAID).build();
        when(repo.findByStudentId(7L)).thenReturn(java.util.List.of(discounted1, discounted2));
        when(repo.findById(1L)).thenReturn(Optional.of(discounted1));
        when(repo.findById(2L)).thenReturn(Optional.of(discounted2));
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        var reverted = service().removeEarlyPaymentDiscount(7L, 3L);

        assertThat(reverted).hasSize(2);
        assertThat(reverted).extracting(Invoice::getDiscountAmount).allMatch(d -> d.signum() == 0);
        assertThat(reverted).extracting(Invoice::getRemainingBalance)
                .allMatch(b -> b.compareTo(new BigDecimal("300.00")) == 0);
    }

    @Test
    void removeEarlyPaymentDiscount_doesNothing_whenNoDiscountWasApplied() {
        Invoice plain = Invoice.builder().id(1L).feeId(3L).totalAmount(new BigDecimal("300.00"))
                .paidAmount(BigDecimal.ZERO).status(InvoiceStatus.UNPAID).build();
        when(repo.findByStudentId(7L)).thenReturn(java.util.List.of(plain));

        var result = service().removeEarlyPaymentDiscount(7L, 3L);

        assertThat(result).containsExactly(plain);
        verify(repo, never()).save(any());
    }

    @Test
    void applyEarlyPaymentDiscount_neverTouchesExemptedTranches() {
        // Régression : un élève avec une exonération 100% ("prise en charge totale") sur TOUTES ses
        // tranches de scolarité ne doit jamais voir cette exonération remplacée par la remise paiement
        // anticipé — sinon il passerait de 0 à devoir 95% du tarif plein dès que le comptable coche
        // toute la scolarité pour encaisser (bug vécu en vrai, voir retour comptable).
        gn.lacaeducation.billing.domain.SchoolFee fee = gn.lacaeducation.billing.domain.SchoolFee.builder()
                .id(3L).tenantId(1L).type(gn.lacaeducation.billing.domain.FeeType.MONTHLY).build();
        when(schoolFeeRepository.findById(3L)).thenReturn(Optional.of(fee));

        Invoice exempted1 = Invoice.builder().id(1L).feeId(3L).totalAmount(new BigDecimal("300.00"))
                .paidAmount(BigDecimal.ZERO).discountAmount(new BigDecimal("300.00"))
                .discountReason("Exonération : prise en charge totale")
                .discountSource(gn.lacaeducation.billing.domain.DiscountSource.EXEMPTION)
                .remainingBalance(BigDecimal.ZERO).status(InvoiceStatus.UNPAID).build();
        Invoice exempted2 = Invoice.builder().id(2L).feeId(3L).totalAmount(new BigDecimal("300.00"))
                .paidAmount(BigDecimal.ZERO).discountAmount(new BigDecimal("300.00"))
                .discountReason("Exonération : prise en charge totale")
                .discountSource(gn.lacaeducation.billing.domain.DiscountSource.EXEMPTION)
                .remainingBalance(BigDecimal.ZERO).status(InvoiceStatus.UNPAID).build();
        when(repo.findByStudentId(7L)).thenReturn(java.util.List.of(exempted1, exempted2));

        var updated = service().applyEarlyPaymentDiscount(7L, 3L);

        assertThat(updated).hasSize(2);
        assertThat(updated).extracting(Invoice::getDiscountAmount)
                .allMatch(d -> d.compareTo(new BigDecimal("300.00")) == 0);
        assertThat(updated).extracting(Invoice::getDiscountSource)
                .allMatch(s -> s == gn.lacaeducation.billing.domain.DiscountSource.EXEMPTION);
        assertThat(updated).extracting(Invoice::getRemainingBalance).allMatch(b -> b.signum() == 0);
        verify(earlyPaymentDiscountUseCase, never()).computeApplicable(any(), any());
        verify(repo, never()).save(any());
    }

    @Test
    void applyEarlyPaymentDiscount_appliesOnlyToNonExemptedTranches_whenMixed() {
        // Même famille de bug que ci-dessus, mais avec seulement UNE tranche exonérée sur trois : la
        // remise paiement anticipé doit porter uniquement sur les deux tranches encore au tarif plein,
        // jamais sur la tranche exonérée.
        gn.lacaeducation.billing.domain.SchoolFee fee = gn.lacaeducation.billing.domain.SchoolFee.builder()
                .id(3L).tenantId(1L).type(gn.lacaeducation.billing.domain.FeeType.MONTHLY).build();
        when(schoolFeeRepository.findById(3L)).thenReturn(Optional.of(fee));

        Invoice exempted = Invoice.builder().id(1L).feeId(3L).totalAmount(new BigDecimal("300.00"))
                .paidAmount(BigDecimal.ZERO).discountAmount(new BigDecimal("300.00"))
                .discountReason("Exonération : prise en charge totale")
                .discountSource(gn.lacaeducation.billing.domain.DiscountSource.EXEMPTION)
                .remainingBalance(BigDecimal.ZERO).status(InvoiceStatus.UNPAID).build();
        Invoice plain1 = Invoice.builder().id(2L).feeId(3L).totalAmount(new BigDecimal("300.00")).paidAmount(BigDecimal.ZERO).status(InvoiceStatus.UNPAID).build();
        Invoice plain2 = Invoice.builder().id(3L).feeId(3L).totalAmount(new BigDecimal("300.00")).paidAmount(BigDecimal.ZERO).status(InvoiceStatus.UNPAID).build();
        when(repo.findByStudentId(7L)).thenReturn(java.util.List.of(exempted, plain1, plain2));
        when(repo.findById(2L)).thenReturn(Optional.of(plain1));
        when(repo.findById(3L)).thenReturn(Optional.of(plain2));
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        when(earlyPaymentDiscountUseCase.computeApplicable(1L, gn.lacaeducation.billing.domain.FeeType.MONTHLY))
                .thenReturn(new gn.lacaeducation.billing.application.port.in.EarlyPaymentDiscountUseCase.ApplicableDiscount(
                        new BigDecimal("10"), "Paiement annuel"));

        var updated = service().applyEarlyPaymentDiscount(7L, 3L);

        assertThat(updated).hasSize(3);
        Invoice updatedExempted = updated.stream().filter(i -> i.getId().equals(1L)).findFirst().orElseThrow();
        assertThat(updatedExempted.getDiscountAmount()).isEqualByComparingTo("300.00");
        assertThat(updatedExempted.getDiscountSource()).isEqualTo(gn.lacaeducation.billing.domain.DiscountSource.EXEMPTION);
        // Base annuelle = 600 (les deux tranches au tarif plein uniquement) → remise 10% = 60.
        BigDecimal discountOnPlain = updated.stream().filter(i -> i.getId() != 1L)
                .map(Invoice::getDiscountAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(discountOnPlain).isEqualByComparingTo("60.00");
    }

    @Test
    void removeEarlyPaymentDiscount_neverTouchesExemptedTranches() {
        Invoice exempted = Invoice.builder().id(1L).feeId(3L).totalAmount(new BigDecimal("300.00"))
                .paidAmount(BigDecimal.ZERO).discountAmount(new BigDecimal("300.00"))
                .discountReason("Exonération : prise en charge totale")
                .discountSource(gn.lacaeducation.billing.domain.DiscountSource.EXEMPTION)
                .remainingBalance(BigDecimal.ZERO).status(InvoiceStatus.UNPAID).build();
        when(repo.findByStudentId(7L)).thenReturn(java.util.List.of(exempted));

        var result = service().removeEarlyPaymentDiscount(7L, 3L);

        assertThat(result).containsExactly(exempted);
        assertThat(result.get(0).getDiscountAmount()).isEqualByComparingTo("300.00");
        verify(repo, never()).save(any());
    }

    // ── Réduction familiale / avantage personnel sur create() (§billing — pas seulement les
    // mensualités via generateMonthly, désormais tous les types de frais : inscription, transport,
    // cantine, examen — voir InvoiceService#computeApplicableDiscount) ──────────────────────────────

    @Test
    void create_unknownFeeId_appliesNoDiscount() {
        // Aucun frais résolu (feeId inconnu du référentiel) : impossible de savoir quel feeType
        // évaluer, donc aucune remise — comportement inchangé par rapport à avant ce câblage.
        when(schoolFeeRepository.findById(3L)).thenReturn(Optional.empty());
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice inv = service().create(new CreateInvoiceCommand(10L, 7L, 3L, "FAC-1", new BigDecimal("100.00"), 99L));

        assertThat(inv.getDiscountAmount()).isEqualByComparingTo("0");
        assertThat(inv.getRemainingBalance()).isEqualByComparingTo("100.00");
        verify(exemptionUseCase, never()).computeApplicable(any(), any());
    }

    @Test
    void create_appliesExemption_onNonMonthlyFee() {
        // Régression du bug corrigé : avant, seule generateMonthly() (mensualités) appliquait une
        // remise — une facture d'inscription/transport/cantine/examen restait toujours plein tarif
        // même pour un élève exonéré. Ici : frais d'inscription (ENROLLMENT).
        when(schoolFeeRepository.findById(3L)).thenReturn(Optional.of(
                gn.lacaeducation.billing.domain.SchoolFee.builder().id(3L)
                        .type(gn.lacaeducation.billing.domain.FeeType.ENROLLMENT).build()));
        when(exemptionUseCase.computeApplicable(7L, gn.lacaeducation.billing.domain.FeeType.ENROLLMENT))
                .thenReturn(new ApplicableExemption(new BigDecimal("20"), "3 enfants inscrits", false, BigDecimal.ZERO, null));
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice inv = service().create(new CreateInvoiceCommand(10L, 7L, 3L, "FAC-1", new BigDecimal("100.00"), 99L));

        assertThat(inv.getDiscountAmount()).isEqualByComparingTo("20.00");
        assertThat(inv.getRemainingBalance()).isEqualByComparingTo("80.00");
        assertThat(inv.getDiscountReason()).contains("3 enfants inscrits");
    }

    @Test
    void create_appliesStaffBenefitExemption_onNonMonthlyFee() {
        // Exonération marquée "avantage personnel" (§retour comptable — la détection automatique
        // enfant de personnel est retirée, le comptable choisit manuellement ce type d'exonération).
        when(schoolFeeRepository.findById(3L)).thenReturn(Optional.of(
                gn.lacaeducation.billing.domain.SchoolFee.builder().id(3L)
                        .type(gn.lacaeducation.billing.domain.FeeType.TRANSPORT).build()));
        when(exemptionUseCase.computeApplicable(7L, gn.lacaeducation.billing.domain.FeeType.TRANSPORT))
                .thenReturn(new ApplicableExemption(new BigDecimal("50"), "Enseignants — 50%", true, BigDecimal.ZERO, null));
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice inv = service().create(new CreateInvoiceCommand(10L, 7L, 3L, "FAC-1", new BigDecimal("100.00"), 99L));

        assertThat(inv.getDiscountAmount()).isEqualByComparingTo("50.00");
        assertThat(inv.getRemainingBalance()).isEqualByComparingTo("50.00");
        assertThat(inv.getDiscountReason()).contains("Enseignants — 50%");
        assertThat(inv.getDiscountSource()).isEqualTo(gn.lacaeducation.billing.domain.DiscountSource.STAFF_BENEFIT);
    }

    @Test
    void generateMonthly_staffBenefitExemption100Percent_skipsInvoiceCreation_reportsAsExemptedPeriod() {
        // Une échéance entièrement couverte par un avantage personnel/une exonération ne doit JAMAIS
        // produire une facture à 0 GNF (§billing, retour comptable) — sans quoi elle risquait de se
        // retrouver cochée par erreur dans un encaissement groupé et de déclencher la remise "paiement
        // anticipé", qui REMPLACE (au lieu de s'y ajouter) la remise existante et effacerait donc
        // l'exonération. Elle doit apparaître uniquement dans `exemptedPeriods`.
        when(enrollmentQueryPort.hasActiveEnrollment(7L)).thenReturn(true);
        when(installmentRepository.findByFeeId(3L)).thenReturn(java.util.List.of(
                gn.lacaeducation.billing.domain.Installment.builder()
                        .id(1L).feeId(3L).label("Septembre").amount(new BigDecimal("100")).build()));
        when(repo.existsByStudentIdAndInstallmentId(7L, 1L)).thenReturn(false);
        when(exemptionUseCase.computeApplicable(7L, gn.lacaeducation.billing.domain.FeeType.MONTHLY))
                .thenReturn(new ApplicableExemption(new BigDecimal("100"), "Direction — 100%", true, BigDecimal.ZERO, null));

        var result = service().generateMonthly(
                new gn.lacaeducation.billing.application.port.in.InvoiceUseCase.GenerateMonthlyCommand(1L, 7L, 3L, 99L));

        assertThat(result.created()).isEmpty();
        assertThat(result.exemptedPeriods()).hasSize(1);
        var exempted = result.exemptedPeriods().get(0);
        assertThat(exempted.installmentId()).isEqualTo(1L);
        assertThat(exempted.periodLabel()).isEqualTo("Septembre");
        assertThat(exempted.discountReason()).contains("Direction — 100%");
        verify(repo, never()).save(any());
    }

    @Test
    void generateMonthly_partialExemption_stillCreatesInvoiceWithRemainingBalance() {
        // Contre-exemple : une exonération PARTIELLE (donc un reste à payer non nul) doit continuer à
        // produire une vraie facture, comme avant — seule une couverture à 100% est concernée par le
        // saut de génération ci-dessus.
        when(enrollmentQueryPort.hasActiveEnrollment(7L)).thenReturn(true);
        when(installmentRepository.findByFeeId(3L)).thenReturn(java.util.List.of(
                gn.lacaeducation.billing.domain.Installment.builder()
                        .id(1L).feeId(3L).label("Septembre").amount(new BigDecimal("100")).build()));
        when(repo.existsByStudentIdAndInstallmentId(7L, 1L)).thenReturn(false);
        when(exemptionUseCase.computeApplicable(7L, gn.lacaeducation.billing.domain.FeeType.MONTHLY))
                .thenReturn(new ApplicableExemption(new BigDecimal("30"), "Prise en charge partielle", false, BigDecimal.ZERO, null));
        when(repo.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        var result = service().generateMonthly(
                new gn.lacaeducation.billing.application.port.in.InvoiceUseCase.GenerateMonthlyCommand(1L, 7L, 3L, 99L));

        assertThat(result.exemptedPeriods()).isEmpty();
        assertThat(result.created()).hasSize(1);
        assertThat(result.created().get(0).getRemainingBalance()).isEqualByComparingTo("70.00");
    }

    @Test
    void generateMonthly_staffChildWithoutAnyExemption_taggedStaffBenefitAtZeroPercent_fullInvoiceCreated() {
        // Retour comptable : un enfant du personnel qui n'a AUCUNE remise (ex. le cadet, quand seul
        // l'aîné est couvert par la politique de prise en charge) doit quand même voir sa scolarité
        // taguée "avantage personnel" — à 0%, donc une vraie facture à plein tarif est bien créée — pour
        // que le prélèvement salarial automatique (SalaryFeeDeductionService) la prenne en charge sans
        // action manuelle du comptable, "quelle qu'en soit le nombre" [d'enfants dans ce cas].
        when(enrollmentQueryPort.hasActiveEnrollment(7L)).thenReturn(true);
        when(installmentRepository.findByFeeId(3L)).thenReturn(java.util.List.of(
                gn.lacaeducation.billing.domain.Installment.builder()
                        .id(1L).feeId(3L).label("Septembre").amount(new BigDecimal("100")).build()));
        when(repo.existsByStudentIdAndInstallmentId(7L, 1L)).thenReturn(false);
        when(staffChildBenefitUseCase.isStaffChild(7L)).thenReturn(true);
        when(repo.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        var result = service().generateMonthly(
                new gn.lacaeducation.billing.application.port.in.InvoiceUseCase.GenerateMonthlyCommand(1L, 7L, 3L, 99L));

        assertThat(result.exemptedPeriods()).isEmpty();
        assertThat(result.created()).hasSize(1);
        Invoice inv = result.created().get(0);
        assertThat(inv.getRemainingBalance()).isEqualByComparingTo("100.00");
        assertThat(inv.getDiscountAmount()).isEqualByComparingTo("0");
        assertThat(inv.getDiscountSource()).isEqualTo(gn.lacaeducation.billing.domain.DiscountSource.STAFF_BENEFIT);
        assertThat(inv.getDiscountReason()).contains("enfant du personnel");
    }

    @Test
    void generateMonthly_staffChildWithExistingExemption_notOverriddenByZeroPercentTag() {
        // Un enfant du personnel qui a DÉJÀ une exonération (même non liée au personnel, ex. cas social)
        // garde cette exonération — le tag "avantage personnel à 0%" ne s'applique qu'en l'absence de
        // toute autre remise.
        when(enrollmentQueryPort.hasActiveEnrollment(7L)).thenReturn(true);
        when(installmentRepository.findByFeeId(3L)).thenReturn(java.util.List.of(
                gn.lacaeducation.billing.domain.Installment.builder()
                        .id(1L).feeId(3L).label("Septembre").amount(new BigDecimal("100")).build()));
        when(repo.existsByStudentIdAndInstallmentId(7L, 1L)).thenReturn(false);
        when(exemptionUseCase.computeApplicable(7L, gn.lacaeducation.billing.domain.FeeType.MONTHLY))
                .thenReturn(new ApplicableExemption(new BigDecimal("20"), "Cas social", false, BigDecimal.ZERO, null));
        when(repo.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        var result = service().generateMonthly(
                new gn.lacaeducation.billing.application.port.in.InvoiceUseCase.GenerateMonthlyCommand(1L, 7L, 3L, 99L));

        Invoice inv = result.created().get(0);
        assertThat(inv.getDiscountAmount()).isEqualByComparingTo("20.00");
        assertThat(inv.getDiscountSource()).isEqualTo(gn.lacaeducation.billing.domain.DiscountSource.EXEMPTION);
        verify(staffChildBenefitUseCase, never()).isStaffChild(any());
    }

    @Test
    void generateMonthly_staffChild_onlyTaggedForMonthlyFee_notOtherFeeTypes() {
        // Le tag "avantage personnel à 0%" ne concerne que la SCOLARITÉ (MONTHLY) — pas les autres
        // types de frais (transport, cantine, inscription...), où le comptable garde la main.
        when(enrollmentQueryPort.hasActiveEnrollment(7L)).thenReturn(true);
        when(schoolFeeRepository.findById(3L)).thenReturn(Optional.of(
                gn.lacaeducation.billing.domain.SchoolFee.builder()
                        .id(3L).type(gn.lacaeducation.billing.domain.FeeType.TRANSPORT).build()));
        when(installmentRepository.findByFeeId(3L)).thenReturn(java.util.List.of(
                gn.lacaeducation.billing.domain.Installment.builder()
                        .id(1L).feeId(3L).label("Septembre").amount(new BigDecimal("100")).build()));
        when(repo.existsByStudentIdAndInstallmentId(7L, 1L)).thenReturn(false);
        when(repo.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        var result = service().generateMonthly(
                new gn.lacaeducation.billing.application.port.in.InvoiceUseCase.GenerateMonthlyCommand(1L, 7L, 3L, 99L));

        assertThat(result.created().get(0).getDiscountSource()).isNull();
        verify(staffChildBenefitUseCase, never()).isStaffChild(any());
    }

    // ── Exonération (§8) : TOTAL/PARTIAL déterminent le pourcentage, EXCEPTIONAL (montant fixe)
    // s'ajoute en plus du pourcentage ──────────────────────────────────────────────────────────────

    @Test
    void create_exemptionApplies_setsDiscountAndReason() {
        when(schoolFeeRepository.findById(3L)).thenReturn(Optional.of(
                gn.lacaeducation.billing.domain.SchoolFee.builder().id(3L)
                        .type(gn.lacaeducation.billing.domain.FeeType.EXAM).build()));
        when(exemptionUseCase.computeApplicable(7L, gn.lacaeducation.billing.domain.FeeType.EXAM))
                .thenReturn(new ApplicableExemption(new BigDecimal("70"), "Situation familiale difficile",
                        false, BigDecimal.ZERO, null));
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice inv = service().create(new CreateInvoiceCommand(10L, 7L, 3L, "FAC-1", new BigDecimal("100.00"), 99L));

        assertThat(inv.getDiscountAmount()).isEqualByComparingTo("70.00");
        assertThat(inv.getDiscountReason()).contains("Situation familiale difficile");
        assertThat(inv.getDiscountSource()).isEqualTo(gn.lacaeducation.billing.domain.DiscountSource.EXEMPTION);
    }

    @Test
    void create_exceptionalExemption_stacksOnTopOfWinningPercentage() {
        // 50% (avantage personnel) + 15 GNF fixes (remise exceptionnelle) : les deux s'additionnent.
        when(schoolFeeRepository.findById(3L)).thenReturn(Optional.of(
                gn.lacaeducation.billing.domain.SchoolFee.builder().id(3L)
                        .type(gn.lacaeducation.billing.domain.FeeType.EXAM).build()));
        when(exemptionUseCase.computeApplicable(7L, gn.lacaeducation.billing.domain.FeeType.EXAM))
                .thenReturn(new ApplicableExemption(new BigDecimal("50"), "Enseignants — 50%", true,
                        new BigDecimal("15"), "Geste directeur"));
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice inv = service().create(new CreateInvoiceCommand(10L, 7L, 3L, "FAC-1", new BigDecimal("100.00"), 99L));

        // 50% de 100 = 50, + 15 fixes = 65.
        assertThat(inv.getDiscountAmount()).isEqualByComparingTo("65.00");
        assertThat(inv.getRemainingBalance()).isEqualByComparingTo("35.00");
        assertThat(inv.getDiscountReason()).contains("Enseignants — 50%");
        assertThat(inv.getDiscountReason()).contains("Geste directeur");
    }

    @Test
    void create_discountNeverExceedsGrossAmount() {
        // Cumul défensif : même si le pourcentage gagnant + la remise exceptionnelle dépassent le
        // montant brut, la remise totale est plafonnée au montant brut (jamais un net négatif).
        when(schoolFeeRepository.findById(3L)).thenReturn(Optional.of(
                gn.lacaeducation.billing.domain.SchoolFee.builder().id(3L)
                        .type(gn.lacaeducation.billing.domain.FeeType.EXAM).build()));
        when(exemptionUseCase.computeApplicable(7L, gn.lacaeducation.billing.domain.FeeType.EXAM))
                .thenReturn(new ApplicableExemption(new BigDecimal("100"), "Exonération totale", false,
                        new BigDecimal("50"), "Geste directeur"));
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice inv = service().create(new CreateInvoiceCommand(10L, 7L, 3L, "FAC-1", new BigDecimal("100.00"), 99L));

        assertThat(inv.getDiscountAmount()).isEqualByComparingTo("100.00");
        assertThat(inv.getRemainingBalance()).isEqualByComparingTo("0.00");
    }

    // ── discountSource (§billing ↔ payroll, avantage personnel → reste retenu sur salaire) ────────────

    @Test
    void create_staffBenefitExemption_setsDiscountSourceStaffBenefit() {
        when(schoolFeeRepository.findById(3L)).thenReturn(Optional.of(
                gn.lacaeducation.billing.domain.SchoolFee.builder().id(3L)
                        .type(gn.lacaeducation.billing.domain.FeeType.MONTHLY).build()));
        when(exemptionUseCase.computeApplicable(7L, gn.lacaeducation.billing.domain.FeeType.MONTHLY))
                .thenReturn(new ApplicableExemption(new BigDecimal("50"), "Enseignants — 50%", true, BigDecimal.ZERO, null));
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice inv = service().create(new CreateInvoiceCommand(10L, 7L, 3L, "FAC-1", new BigDecimal("100.00"), 99L));

        assertThat(inv.getDiscountSource()).isEqualTo(gn.lacaeducation.billing.domain.DiscountSource.STAFF_BENEFIT);
    }

    @Test
    void create_regularExemptionWins_setsDiscountSourceExemption() {
        when(schoolFeeRepository.findById(3L)).thenReturn(Optional.of(
                gn.lacaeducation.billing.domain.SchoolFee.builder().id(3L)
                        .type(gn.lacaeducation.billing.domain.FeeType.MONTHLY).build()));
        when(exemptionUseCase.computeApplicable(7L, gn.lacaeducation.billing.domain.FeeType.MONTHLY))
                .thenReturn(new ApplicableExemption(new BigDecimal("10"), "Cas social", false, BigDecimal.ZERO, null));
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice inv = service().create(new CreateInvoiceCommand(10L, 7L, 3L, "FAC-1", new BigDecimal("100.00"), 99L));

        assertThat(inv.getDiscountSource()).isEqualTo(gn.lacaeducation.billing.domain.DiscountSource.EXEMPTION);
    }

    @Test
    void create_noPercentageDiscount_discountSourceIsNull() {
        when(schoolFeeRepository.findById(3L)).thenReturn(Optional.of(
                gn.lacaeducation.billing.domain.SchoolFee.builder().id(3L)
                        .type(gn.lacaeducation.billing.domain.FeeType.MONTHLY).build()));
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice inv = service().create(new CreateInvoiceCommand(10L, 7L, 3L, "FAC-1", new BigDecimal("100.00"), 99L));

        assertThat(inv.getDiscountSource()).isNull();
    }

    // ── outstandingStaffBenefitRemainder (§billing ↔ payroll) ───────────────────────────────────────

    @Test
    void outstandingStaffBenefitRemainder_sumsOnlyStaffBenefitSourcedOutstandingInvoices() {
        Invoice staffBenefit1 = Invoice.builder().id(1L).studentId(7L)
                .discountSource(gn.lacaeducation.billing.domain.DiscountSource.STAFF_BENEFIT)
                .status(InvoiceStatus.UNPAID).remainingBalance(new BigDecimal("15000")).build();
        Invoice staffBenefit2 = Invoice.builder().id(2L).studentId(7L)
                .discountSource(gn.lacaeducation.billing.domain.DiscountSource.STAFF_BENEFIT)
                .status(InvoiceStatus.PARTIAL).remainingBalance(new BigDecimal("5000")).build();
        Invoice familyDiscounted = Invoice.builder().id(3L).studentId(7L)
                .discountSource(gn.lacaeducation.billing.domain.DiscountSource.FAMILY)
                .status(InvoiceStatus.UNPAID).remainingBalance(new BigDecimal("9999")).build();
        Invoice staffBenefitPaid = Invoice.builder().id(4L).studentId(7L)
                .discountSource(gn.lacaeducation.billing.domain.DiscountSource.STAFF_BENEFIT)
                .status(InvoiceStatus.PAID).remainingBalance(BigDecimal.ZERO).build();
        when(repo.findByStudentId(7L)).thenReturn(List.of(staffBenefit1, staffBenefit2, familyDiscounted, staffBenefitPaid));

        BigDecimal remainder = service().outstandingStaffBenefitRemainder(7L, null, null);

        assertThat(remainder).isEqualByComparingTo("20000");
    }

    @Test
    void outstandingStaffBenefitRemainder_noStaffBenefitInvoices_returnsZero() {
        when(repo.findByStudentId(7L)).thenReturn(List.of());

        assertThat(service().outstandingStaffBenefitRemainder(7L, null, null)).isEqualByComparingTo("0");
    }

    // ── outstandingStaffBenefitRemainder scoping to one payroll period (§retour comptable, bug corrigé :
    // une mensualité éclatée en 9 factures dès l'inscription ne doit faire retenir qu'UN mois) ──────────

    @Test
    void outstandingStaffBenefitRemainder_onlyCountsInvoiceMatchingThePayrollPeriod() {
        Invoice october = Invoice.builder().id(1L).studentId(7L)
                .discountSource(gn.lacaeducation.billing.domain.DiscountSource.STAFF_BENEFIT)
                .status(InvoiceStatus.UNPAID).remainingBalance(new BigDecimal("67500"))
                .periodLabel("Octobre 2026").build();
        Invoice november = Invoice.builder().id(2L).studentId(7L)
                .discountSource(gn.lacaeducation.billing.domain.DiscountSource.STAFF_BENEFIT)
                .status(InvoiceStatus.UNPAID).remainingBalance(new BigDecimal("67500"))
                .periodLabel("Novembre 2026").build();
        when(repo.findByStudentId(7L)).thenReturn(List.of(october, november));

        // La fiche de paie d'octobre ne doit récupérer QUE la facture d'octobre — jamais celle de
        // novembre, déjà générée d'avance mais pas encore due.
        BigDecimal remainder = service().outstandingStaffBenefitRemainder(7L, java.time.LocalDate.of(2026, 10, 1), "Octobre 2026");

        assertThat(remainder).isEqualByComparingTo("67500");
    }

    @Test
    void outstandingStaffBenefitRemainder_nonMonthlyLabelledInvoice_alwaysCountedRegardlessOfPeriod() {
        // Tranche non éclatée (ex. "Trimestre 1") ou frais non-mensuel (transport, cantine...) : pas
        // rattachée à un seul mois, donc comptée quel que soit le mois de paie demandé.
        Invoice tranche = Invoice.builder().id(1L).studentId(7L)
                .discountSource(gn.lacaeducation.billing.domain.DiscountSource.STAFF_BENEFIT)
                .status(InvoiceStatus.UNPAID).remainingBalance(new BigDecimal("15000"))
                .periodLabel("Trimestre 1").build();
        when(repo.findByStudentId(7L)).thenReturn(List.of(tranche));

        BigDecimal remainder = service().outstandingStaffBenefitRemainder(7L, java.time.LocalDate.of(2026, 10, 1), "Octobre 2026");

        assertThat(remainder).isEqualByComparingTo("15000");
    }

    @Test
    void outstandingStaffBenefitRemainder_periodStartTakesPrecedenceOverPeriodLabel() {
        Invoice october = Invoice.builder().id(1L).studentId(7L)
                .discountSource(gn.lacaeducation.billing.domain.DiscountSource.STAFF_BENEFIT)
                .status(InvoiceStatus.UNPAID).remainingBalance(new BigDecimal("67500"))
                .periodLabel("Octobre 2026").build();
        when(repo.findByStudentId(7L)).thenReturn(List.of(october));

        // periodStart (2026-10) l'emporte sur un periodLabel de paie qui ne correspond à rien
        // ("Paie #3") — dérive quand même "Octobre 2026" et trouve la bonne facture.
        BigDecimal remainder = service().outstandingStaffBenefitRemainder(7L, java.time.LocalDate.of(2026, 10, 15), "Paie #3");

        assertThat(remainder).isEqualByComparingTo("67500");
    }

    // ── applyStaffBenefitRemainderPayment (§retour comptable) ───────────────────────────────────────

    @Test
    void applyStaffBenefitRemainderPayment_paysOnlyTheMatchingMonthsInvoice() {
        Invoice october = Invoice.builder().id(1L).studentId(7L).tenantId(1L)
                .discountSource(gn.lacaeducation.billing.domain.DiscountSource.STAFF_BENEFIT)
                .status(InvoiceStatus.UNPAID).totalAmount(new BigDecimal("67500")).paidAmount(BigDecimal.ZERO)
                .remainingBalance(new BigDecimal("67500")).periodLabel("Octobre 2026").build();
        Invoice november = Invoice.builder().id(2L).studentId(7L).tenantId(1L)
                .discountSource(gn.lacaeducation.billing.domain.DiscountSource.STAFF_BENEFIT)
                .status(InvoiceStatus.UNPAID).totalAmount(new BigDecimal("67500")).paidAmount(BigDecimal.ZERO)
                .remainingBalance(new BigDecimal("67500")).periodLabel("Novembre 2026").build();
        when(repo.findByStudentId(7L)).thenReturn(List.of(october, november));
        when(repo.findById(1L)).thenReturn(Optional.of(october));
        when(repo.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        service().applyStaffBenefitRemainderPayment(7L, java.time.LocalDate.of(2026, 10, 1), "Octobre 2026", new BigDecimal("67500"));

        assertThat(october.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(november.getStatus()).isEqualTo(InvoiceStatus.UNPAID);
        verify(repo, never()).findById(2L);
    }
}
