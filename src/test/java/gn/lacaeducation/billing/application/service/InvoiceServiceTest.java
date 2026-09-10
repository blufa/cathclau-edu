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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import javax.print.attribute.standard.Media;
import java.math.BigDecimal;
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
    @Mock gn.lacaeducation.billing.application.port.in.FamilyDiscountUseCase familyDiscountUseCase;
    @Mock gn.lacaeducation.billing.application.port.in.EarlyPaymentDiscountUseCase earlyPaymentDiscountUseCase;
    @Mock gn.lacaeducation.platform.PlatformFeeQuoteApi platformFeeQuoteApi;
    @Mock gn.lacaeducation.platform.PlatformFeeLedgerApi platformFeeLedgerApi;

    InvoiceService service() {
        return new InvoiceService(repo, schoolFeeRepository, installmentRepository, enrollmentQueryPort, eventPublisher, invoicePdfGenerator, mediaUploadService, tenantInfoProvider, familyDiscountUseCase, earlyPaymentDiscountUseCase, platformFeeQuoteApi, platformFeeLedgerApi);

    }

    @Test
    void generateMonthly_appliesFamilyDiscountOnEachInstallment() {
        when(enrollmentQueryPort.hasActiveEnrollment(7L)).thenReturn(true);
        when(installmentRepository.findByFeeId(3L)).thenReturn(java.util.List.of(
                gn.lacaeducation.billing.domain.Installment.builder()
                        .id(1L).feeId(3L).label("Septembre").amount(new BigDecimal("100")).build()));
        when(repo.existsByStudentIdAndInstallmentId(7L, 1L)).thenReturn(false);
        when(familyDiscountUseCase.computeApplicable(1L, 7L, gn.lacaeducation.billing.domain.FeeType.MONTHLY))
                .thenReturn(new gn.lacaeducation.billing.application.port.in.FamilyDiscountUseCase.ApplicableDiscount(
                        new BigDecimal("5"), "Règle fratrie"));
        when(repo.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        var invoices = service().generateMonthly(
                new gn.lacaeducation.billing.application.port.in.InvoiceUseCase.GenerateMonthlyCommand(1L, 7L, 3L, 99L));

        assertThat(invoices).hasSize(1);
        Invoice inv = invoices.get(0);
        assertThat(inv.getDiscountAmount()).isEqualByComparingTo("5.00");
        assertThat(inv.getRemainingBalance()).isEqualByComparingTo("95.00");
        assertThat(inv.getDiscountReason()).contains("Règle fratrie");
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
        when(familyDiscountUseCase.computeApplicable(1L, 7L, gn.lacaeducation.billing.domain.FeeType.MONTHLY))
                .thenReturn(new gn.lacaeducation.billing.application.port.in.FamilyDiscountUseCase.ApplicableDiscount(
                        BigDecimal.ZERO, null));
        when(repo.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        var invoices = service().generateMonthly(
                new gn.lacaeducation.billing.application.port.in.InvoiceUseCase.GenerateMonthlyCommand(1L, 7L, 3L, null));

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
    void generateMonthly_addsPlatformFee_whenFeeTypeIsMonthly() {
        when(enrollmentQueryPort.hasActiveEnrollment(7L)).thenReturn(true);
        when(enrollmentQueryPort.hasPlatformAccessOptIn(7L, 99L)).thenReturn(true);
        when(installmentRepository.findByFeeId(3L)).thenReturn(java.util.List.of(
                gn.lacaeducation.billing.domain.Installment.builder()
                        .id(1L).feeId(3L).label("Septembre").amount(new BigDecimal("100")).build()));
        when(repo.existsByStudentIdAndInstallmentId(7L, 1L)).thenReturn(false);
        when(schoolFeeRepository.findById(3L)).thenReturn(Optional.of(
                gn.lacaeducation.billing.domain.SchoolFee.builder().id(3L)
                        .type(gn.lacaeducation.billing.domain.FeeType.MONTHLY).build()));
        when(familyDiscountUseCase.computeApplicable(1L, 7L, gn.lacaeducation.billing.domain.FeeType.MONTHLY))
                .thenReturn(new gn.lacaeducation.billing.application.port.in.FamilyDiscountUseCase.ApplicableDiscount(
                        null, null));
        when(platformFeeQuoteApi.currentAmount()).thenReturn(new BigDecimal("500"));
        when(repo.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        var invoices = service().generateMonthly(
                new gn.lacaeducation.billing.application.port.in.InvoiceUseCase.GenerateMonthlyCommand(1L, 7L, 3L, 99L));

        Invoice inv = invoices.get(0);
        assertThat(inv.getTotalAmount()).isEqualByComparingTo("600");
        assertThat(inv.getPlatformFeeAmount()).isEqualByComparingTo("500");
        assertThat(inv.getRemainingBalance()).isEqualByComparingTo("600");
    }

    @Test
    void generateMonthly_neverAddsPlatformFee_whenStudentNotOptedIn() {
        // Frais annexe optionnel (§billing) : sans opt-in du parent à l'inscription, le frais
        // plateforme n'est jamais ajouté, même sur une vraie mensualité (MONTHLY).
        when(enrollmentQueryPort.hasActiveEnrollment(7L)).thenReturn(true);
        when(enrollmentQueryPort.hasPlatformAccessOptIn(7L, 99L)).thenReturn(false);
        when(installmentRepository.findByFeeId(3L)).thenReturn(java.util.List.of(
                gn.lacaeducation.billing.domain.Installment.builder()
                        .id(1L).feeId(3L).label("Septembre").amount(new BigDecimal("100")).build()));
        when(repo.existsByStudentIdAndInstallmentId(7L, 1L)).thenReturn(false);
        when(schoolFeeRepository.findById(3L)).thenReturn(Optional.of(
                gn.lacaeducation.billing.domain.SchoolFee.builder().id(3L)
                        .type(gn.lacaeducation.billing.domain.FeeType.MONTHLY).build()));
        when(familyDiscountUseCase.computeApplicable(1L, 7L, gn.lacaeducation.billing.domain.FeeType.MONTHLY))
                .thenReturn(new gn.lacaeducation.billing.application.port.in.FamilyDiscountUseCase.ApplicableDiscount(
                        null, null));
        when(repo.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        var invoices = service().generateMonthly(
                new gn.lacaeducation.billing.application.port.in.InvoiceUseCase.GenerateMonthlyCommand(1L, 7L, 3L, 99L));

        Invoice inv = invoices.get(0);
        assertThat(inv.getTotalAmount()).isEqualByComparingTo("100");
        assertThat(inv.getPlatformFeeAmount()).isNull();
    }

    @Test
    void generateMonthly_neverAddsPlatformFee_whenFeeTypeIsNotMonthly() {
        // Transport (et cantine, etc.) réutilisent generateMonthly() : le frais plateforme ne doit
        // jamais s'y ajouter — schoolFeeRepository répond TRANSPORT, platformFeeQuoteApi n'est même
        // pas interrogé (court-circuit), volontairement non stubbé pour le prouver.
        when(enrollmentQueryPort.hasActiveEnrollment(7L)).thenReturn(true);
        when(installmentRepository.findByFeeId(3L)).thenReturn(java.util.List.of(
                gn.lacaeducation.billing.domain.Installment.builder()
                        .id(1L).feeId(3L).label("Septembre").amount(new BigDecimal("100")).build()));
        when(repo.existsByStudentIdAndInstallmentId(7L, 1L)).thenReturn(false);
        when(schoolFeeRepository.findById(3L)).thenReturn(Optional.of(
                gn.lacaeducation.billing.domain.SchoolFee.builder().id(3L)
                        .type(gn.lacaeducation.billing.domain.FeeType.TRANSPORT).build()));
        when(familyDiscountUseCase.computeApplicable(1L, 7L, gn.lacaeducation.billing.domain.FeeType.MONTHLY))
                .thenReturn(new gn.lacaeducation.billing.application.port.in.FamilyDiscountUseCase.ApplicableDiscount(
                        null, null));
        when(repo.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        var invoices = service().generateMonthly(
                new gn.lacaeducation.billing.application.port.in.InvoiceUseCase.GenerateMonthlyCommand(1L, 7L, 3L, 99L));

        Invoice inv = invoices.get(0);
        assertThat(inv.getTotalAmount()).isEqualByComparingTo("100");
        assertThat(inv.getPlatformFeeAmount()).isNull();
    }

    @Test
    void applyPayment_recordsPlatformFeeLedger_proportionalToAmountPaid() {
        Invoice existing = Invoice.builder().id(1L).tenantId(10L).studentId(7L)
                .totalAmount(new BigDecimal("600.00")).paidAmount(BigDecimal.ZERO)
                .remainingBalance(new BigDecimal("600.00")).status(InvoiceStatus.UNPAID)
                .platformFeeAmount(new BigDecimal("500.00")).build();
        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        service().applyPayment(1L, new BigDecimal("60.00"));

        verify(platformFeeLedgerApi).record(10L, 1L, new BigDecimal("50.00"));
    }

    @Test
    void applyPayment_doesNotRecordPlatformFeeLedger_whenInvoiceHasNoPlatformFee() {
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
}
