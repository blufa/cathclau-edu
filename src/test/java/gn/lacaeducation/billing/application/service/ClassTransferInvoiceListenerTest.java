package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.port.in.ClassTransferRefundUseCase;
import gn.lacaeducation.billing.application.port.in.InvoiceUseCase;
import gn.lacaeducation.billing.application.port.in.InvoiceUseCase.GenerateMonthlyCommand;
import gn.lacaeducation.billing.application.port.out.SchoolFeeRepository;
import gn.lacaeducation.billing.domain.FeeType;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.InvoiceStatus;
import gn.lacaeducation.billing.domain.SchoolFee;
import gn.lacaeducation.billing.application.event.ClassTransferCreditSurplusEvent;
import gn.lacaeducation.students.application.event.EnrollmentClassChangedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClassTransferInvoiceListenerTest {

    @Mock SchoolFeeRepository schoolFeeRepository;
    @Mock InvoiceUseCase invoiceUseCase;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock ClassTransferRefundUseCase classTransferRefundUseCase;
    @InjectMocks ClassTransferInvoiceListener listener;

    private final EnrollmentClassChangedEvent event = new EnrollmentClassChangedEvent(null, 7L, 1L, 20L, 21L, 99L);

    @Test
    void voidsOldMonthlyInvoicesAndCarriesForwardCreditOntoNewOnes() {
        when(schoolFeeRepository.findByClassIdAndType(20L, FeeType.MONTHLY))
                .thenReturn(List.of(SchoolFee.builder().id(3L).tenantId(10L).classId(20L).type(FeeType.MONTHLY).build()));
        Invoice paidOld = Invoice.builder().id(100L).studentId(7L).feeId(3L).academicYearId(99L)
                .status(InvoiceStatus.PAID).totalAmount(new BigDecimal("50.00")).paidAmount(new BigDecimal("50.00")).build();
        Invoice unpaidOld = Invoice.builder().id(101L).studentId(7L).feeId(3L).academicYearId(99L)
                .status(InvoiceStatus.UNPAID).totalAmount(new BigDecimal("50.00")).paidAmount(BigDecimal.ZERO).build();
        when(invoiceUseCase.findByStudentId(7L)).thenReturn(List.of(paidOld, unpaidOld));

        SchoolFee newFee = SchoolFee.builder().id(4L).tenantId(10L).classId(21L).type(FeeType.MONTHLY).build();
        when(schoolFeeRepository.findByClassIdAndType(21L, FeeType.MONTHLY)).thenReturn(List.of(newFee));

        Invoice newInv1 = Invoice.builder().id(200L).installmentId(1L).totalAmount(new BigDecimal("40.00")).remainingBalance(new BigDecimal("40.00")).build();
        Invoice newInv2 = Invoice.builder().id(201L).installmentId(2L).totalAmount(new BigDecimal("40.00")).remainingBalance(new BigDecimal("40.00")).build();
        when(invoiceUseCase.generateMonthly(any(GenerateMonthlyCommand.class)))
                .thenReturn(new InvoiceUseCase.GenerateMonthlyResult(List.of(newInv1, newInv2), List.of()));

        listener.onEnrollmentClassChanged(event);

        verify(invoiceUseCase).voidInvoice(100L);
        verify(invoiceUseCase).voidInvoice(101L);
        verify(invoiceUseCase).generateMonthly(new GenerateMonthlyCommand(10L, 7L, 4L, 99L));
        // Crédit total = 50.00 (seul le montant déjà payé) réparti sur les nouvelles factures, dans
        // l'ordre chronologique des échéances : la première absorbe 40.00, la seconde le reliquat de 10.00.
        verify(invoiceUseCase).applyCredit(eq(200L), eq(new BigDecimal("40.00")), anyString());
        verify(invoiceUseCase).applyCredit(eq(201L), eq(new BigDecimal("10.00")), anyString());
    }

    @Test
    void noOldMonthlyInvoices_doesNothing() {
        when(schoolFeeRepository.findByClassIdAndType(20L, FeeType.MONTHLY)).thenReturn(List.of());
        when(invoiceUseCase.findByStudentId(7L)).thenReturn(List.of());

        listener.onEnrollmentClassChanged(event);

        verify(invoiceUseCase, never()).voidInvoice(any());
        verify(invoiceUseCase, never()).generateMonthly(any());
    }

    @Test
    void newClassHasNoMonthlyFee_voidsOldButDoesNotGenerate() {
        when(schoolFeeRepository.findByClassIdAndType(20L, FeeType.MONTHLY))
                .thenReturn(List.of(SchoolFee.builder().id(3L).tenantId(10L).classId(20L).type(FeeType.MONTHLY).build()));
        Invoice paidOld = Invoice.builder().id(100L).studentId(7L).feeId(3L).academicYearId(99L)
                .status(InvoiceStatus.PAID).totalAmount(new BigDecimal("50.00")).paidAmount(new BigDecimal("50.00")).build();
        when(invoiceUseCase.findByStudentId(7L)).thenReturn(List.of(paidOld));
        when(schoolFeeRepository.findByClassIdAndType(21L, FeeType.MONTHLY)).thenReturn(List.of());

        listener.onEnrollmentClassChanged(event);

        verify(invoiceUseCase).voidInvoice(100L);
        verify(invoiceUseCase, never()).generateMonthly(any());
        verify(invoiceUseCase, never()).applyCredit(any(), any(), any());
        // Retour comptable : ce crédit entier (plus aucune mensualité où le reporter) est un surplus à
        // rembourser au parent — doit être signalé, pas juste logué.
        verify(eventPublisher).publishEvent(new ClassTransferCreditSurplusEvent(null, 7L, 20L, 21L, new BigDecimal("50.00")));
        verify(classTransferRefundUseCase).record(isNull(), eq(7L), eq(20L), eq(21L), eq(new BigDecimal("50.00")), anyString());
    }

    /**
     * Retour comptable réel : un parent qui avait déjà réglé toute l'année dans l'ancienne classe se
     * retrouve, après transfert vers une classe moins chère, avec un reliquat non affectable une fois
     * toutes les nouvelles mensualités couvertes — ce surplus doit être signalé pour remboursement, pas
     * seulement logué comme avant.
     */
    @Test
    void creditExceedsNewClassInvoices_publishesSurplusForRefund() {
        when(schoolFeeRepository.findByClassIdAndType(20L, FeeType.MONTHLY))
                .thenReturn(List.of(SchoolFee.builder().id(3L).tenantId(10L).classId(20L).type(FeeType.MONTHLY).build()));
        Invoice paidOld = Invoice.builder().id(100L).studentId(7L).feeId(3L).academicYearId(99L)
                .status(InvoiceStatus.PAID).totalAmount(new BigDecimal("100.00")).paidAmount(new BigDecimal("100.00")).build();
        when(invoiceUseCase.findByStudentId(7L)).thenReturn(List.of(paidOld));

        SchoolFee newFee = SchoolFee.builder().id(4L).tenantId(10L).classId(21L).type(FeeType.MONTHLY).build();
        when(schoolFeeRepository.findByClassIdAndType(21L, FeeType.MONTHLY)).thenReturn(List.of(newFee));
        Invoice newInv = Invoice.builder().id(200L).installmentId(1L).totalAmount(new BigDecimal("40.00")).remainingBalance(new BigDecimal("40.00")).build();
        when(invoiceUseCase.generateMonthly(any(GenerateMonthlyCommand.class)))
                .thenReturn(new InvoiceUseCase.GenerateMonthlyResult(List.of(newInv), List.of()));

        listener.onEnrollmentClassChanged(event);

        verify(invoiceUseCase).applyCredit(eq(200L), eq(new BigDecimal("40.00")), anyString());
        // Reliquat = 100.00 - 40.00 = 60.00, jamais affecté à aucune facture.
        verify(eventPublisher).publishEvent(new ClassTransferCreditSurplusEvent(10L, 7L, 20L, 21L, new BigDecimal("60.00")));
        verify(classTransferRefundUseCase).record(eq(10L), eq(7L), eq(20L), eq(21L), eq(new BigDecimal("60.00")), anyString());
    }

    /**
     * Retour comptable réel (cas Abib Kiss BERETE) : un parent qui a réglé toute l'année avec la
     * remise "paiement anticipé" dans l'ancienne classe doit en bénéficier aussi sur la nouvelle,
     * sinon le crédit (déjà net de cette remise) se compare à un total resté au plein tarif — un
     * vrai surplus resterait alors invisible.
     */
    @Test
    void oldClassHadEarlyPaymentDiscount_carriesItOverBeforeComparingCredit() {
        when(schoolFeeRepository.findByClassIdAndType(20L, FeeType.MONTHLY))
                .thenReturn(List.of(SchoolFee.builder().id(3L).tenantId(10L).classId(20L).type(FeeType.MONTHLY).build()));
        // Ancienne classe : 100.00 brut remisé à 5% (payé net = 95.00), remise "paiement anticipé".
        Invoice paidOld = Invoice.builder().id(100L).studentId(7L).feeId(3L).academicYearId(99L)
                .status(InvoiceStatus.PAID).totalAmount(new BigDecimal("100.00")).paidAmount(new BigDecimal("95.00"))
                .discountAmount(new BigDecimal("5.00")).discountReason("Remise paiement anticipé : Scolarite annuelle (5%)")
                .build();
        when(invoiceUseCase.findByStudentId(7L)).thenReturn(List.of(paidOld));

        SchoolFee newFee = SchoolFee.builder().id(4L).tenantId(10L).classId(21L).type(FeeType.MONTHLY).build();
        when(schoolFeeRepository.findByClassIdAndType(21L, FeeType.MONTHLY)).thenReturn(List.of(newFee));
        // Nouvelle classe générée au plein tarif (90.00) — applyEarlyPaymentDiscount doit la ramener à
        // 85.50 net (5% de remise reportée) avant que le crédit ne s'y compare.
        Invoice newInvFullPrice = Invoice.builder().id(200L).installmentId(1L).totalAmount(new BigDecimal("90.00"))
                .remainingBalance(new BigDecimal("90.00")).build();
        when(invoiceUseCase.generateMonthly(any(GenerateMonthlyCommand.class)))
                .thenReturn(new InvoiceUseCase.GenerateMonthlyResult(List.of(newInvFullPrice), List.of()));
        Invoice newInvDiscounted = Invoice.builder().id(200L).installmentId(1L).totalAmount(new BigDecimal("90.00"))
                .discountAmount(new BigDecimal("4.50")).remainingBalance(new BigDecimal("85.50")).build();
        when(invoiceUseCase.applyEarlyPaymentDiscount(7L, 4L)).thenReturn(List.of(newInvDiscounted));

        listener.onEnrollmentClassChanged(event);

        verify(invoiceUseCase).applyEarlyPaymentDiscount(7L, 4L);
        // Crédit = 95.00, plafonné au solde NET remisé (85.50), pas au total brut (90.00).
        verify(invoiceUseCase).applyCredit(eq(200L), eq(new BigDecimal("85.50")), anyString());
        // Surplus = 95.00 - 85.50 = 9.50, à rembourser.
        verify(classTransferRefundUseCase).record(eq(10L), eq(7L), eq(20L), eq(21L), eq(new BigDecimal("9.50")), anyString());
    }

    /**
     * Régression réelle (§retour comptable, transfert testé en conditions réelles) : une version
     * antérieure annulait aussi les factures ANNEXES (hors mensualité/inscription) de l'ancienne
     * classe, mais sans jamais les régénérer pour la nouvelle — une facture annexe encore impayée
     * disparaissait purement et simplement au lieu de rester "à régler". Vérifie que ces factures
     * annexes (ex. "Révision", ajoutée librement par le comptable) ne sont toujours pas touchées —
     * contrairement à l'inscription (voir {@link #voidsOldEnrollmentInvoiceAndCarriesForwardCredit}),
     * qui elle EST désormais reconciliée comme la mensualité.
     */
    @Test
    void doesNotTouchAnnexInvoices_otherThanMonthlyOrEnrollment() {
        when(schoolFeeRepository.findByClassIdAndType(20L, FeeType.MONTHLY)).thenReturn(List.of());
        when(schoolFeeRepository.findByClassIdAndType(20L, FeeType.ENROLLMENT)).thenReturn(List.of());
        when(schoolFeeRepository.findByClassIdAndType(20L, FeeType.RE_ENROLLMENT)).thenReturn(List.of());

        Invoice revisionInvoice = Invoice.builder().id(300L).reference("REF-REV").studentId(7L).feeId(30L).academicYearId(99L)
                .status(InvoiceStatus.UNPAID).totalAmount(new BigDecimal("50000")).paidAmount(BigDecimal.ZERO).build();
        when(invoiceUseCase.findByStudentId(7L)).thenReturn(List.of(revisionInvoice));

        listener.onEnrollmentClassChanged(event);

        verify(invoiceUseCase, never()).voidInvoice(300L);
        verify(schoolFeeRepository, never()).findByClassId(any());
    }

    /**
     * Nouveau comportement (§retour comptable réel : "il inscrit quelqu'un en 9ème, encaisse
     * l'argent, et il se trouve que ça devrait être en 7ème") : le frais d'inscription/réinscription
     * suit désormais la même logique de report de crédit que la mensualité — contrairement aux autres
     * frais annexes (voir {@link #doesNotTouchAnnexInvoices_otherThanMonthlyOrEnrollment}), il est
     * généré automatiquement par l'application (jamais un ajout libre du comptable), donc aucun risque
     * de duplication à le régénérer pour la nouvelle classe.
     */
    @Test
    void voidsOldEnrollmentInvoiceAndCarriesForwardCredit() {
        when(schoolFeeRepository.findByClassIdAndType(20L, FeeType.MONTHLY)).thenReturn(List.of());
        SchoolFee oldEnrollmentFee = SchoolFee.builder().id(31L).tenantId(10L).classId(20L).type(FeeType.ENROLLMENT).build();
        when(schoolFeeRepository.findByClassIdAndType(20L, FeeType.ENROLLMENT)).thenReturn(List.of(oldEnrollmentFee));
        when(schoolFeeRepository.findByClassIdAndType(20L, FeeType.RE_ENROLLMENT)).thenReturn(List.of());

        Invoice enrollmentInvoice = Invoice.builder().id(301L).reference("REF-INS").studentId(7L).feeId(31L).academicYearId(99L)
                .status(InvoiceStatus.PAID).totalAmount(new BigDecimal("250000")).paidAmount(new BigDecimal("250000")).build();
        when(invoiceUseCase.findByStudentId(7L)).thenReturn(List.of(enrollmentInvoice));

        SchoolFee newEnrollmentFee = SchoolFee.builder().id(32L).tenantId(10L).classId(21L).type(FeeType.ENROLLMENT).amount(new BigDecimal("200000")).build();
        when(schoolFeeRepository.findByClassIdAndType(21L, FeeType.RE_ENROLLMENT)).thenReturn(List.of());
        when(schoolFeeRepository.findByClassIdAndType(21L, FeeType.ENROLLMENT)).thenReturn(List.of(newEnrollmentFee));
        when(invoiceUseCase.existsByStudentIdAndFeeId(7L, 32L)).thenReturn(false);

        Invoice newInvoice = Invoice.builder().id(400L).totalAmount(new BigDecimal("200000")).remainingBalance(new BigDecimal("200000")).build();
        when(invoiceUseCase.create(any(InvoiceUseCase.CreateInvoiceCommand.class))).thenReturn(newInvoice);

        listener.onEnrollmentClassChanged(event);

        verify(invoiceUseCase).voidInvoice(301L);
        verify(invoiceUseCase).create(new InvoiceUseCase.CreateInvoiceCommand(10L, 7L, 32L, null, new BigDecimal("200000"), 99L));
        // Crédit = 250 000 (payé en 9ème), nouvelle facture 7ème = 200 000 : plafonné à 200 000, le
        // reliquat de 50 000 part en surplus à rembourser (frais d'inscription moins cher en 7ème).
        verify(invoiceUseCase).applyCredit(eq(400L), eq(new BigDecimal("200000")), anyString());
        verify(classTransferRefundUseCase).record(eq(10L), eq(7L), eq(20L), eq(21L), eq(new BigDecimal("50000")), anyString());
    }

    /** Pas de frais d'inscription configuré du tout pour la nouvelle classe : le crédit entier part en surplus. */
    @Test
    void newClassHasNoEnrollmentFee_voidsOldButDoesNotGenerate() {
        when(schoolFeeRepository.findByClassIdAndType(20L, FeeType.MONTHLY)).thenReturn(List.of());
        SchoolFee oldEnrollmentFee = SchoolFee.builder().id(31L).tenantId(10L).classId(20L).type(FeeType.ENROLLMENT).build();
        when(schoolFeeRepository.findByClassIdAndType(20L, FeeType.ENROLLMENT)).thenReturn(List.of(oldEnrollmentFee));
        when(schoolFeeRepository.findByClassIdAndType(20L, FeeType.RE_ENROLLMENT)).thenReturn(List.of());

        Invoice enrollmentInvoice = Invoice.builder().id(301L).reference("REF-INS").studentId(7L).feeId(31L).academicYearId(99L)
                .tenantId(10L).status(InvoiceStatus.PAID).totalAmount(new BigDecimal("250000")).paidAmount(new BigDecimal("250000")).build();
        when(invoiceUseCase.findByStudentId(7L)).thenReturn(List.of(enrollmentInvoice));

        when(schoolFeeRepository.findByClassIdAndType(21L, FeeType.RE_ENROLLMENT)).thenReturn(List.of());
        when(schoolFeeRepository.findByClassIdAndType(21L, FeeType.ENROLLMENT)).thenReturn(List.of());

        listener.onEnrollmentClassChanged(event);

        verify(invoiceUseCase).voidInvoice(301L);
        verify(invoiceUseCase, never()).create(any());
        verify(invoiceUseCase, never()).applyCredit(any(), any(), any());
        verify(classTransferRefundUseCase).record(eq(10L), eq(7L), eq(20L), eq(21L), eq(new BigDecimal("250000")), anyString());
    }

    @Test
    void alreadyCancelledOldInvoices_areIgnored() {
        when(schoolFeeRepository.findByClassIdAndType(20L, FeeType.MONTHLY))
                .thenReturn(List.of(SchoolFee.builder().id(3L).tenantId(10L).classId(20L).type(FeeType.MONTHLY).build()));
        Invoice alreadyCancelled = Invoice.builder().id(100L).studentId(7L).feeId(3L).academicYearId(99L)
                .status(InvoiceStatus.CANCELLED).totalAmount(new BigDecimal("50.00")).paidAmount(BigDecimal.ZERO).build();
        when(invoiceUseCase.findByStudentId(7L)).thenReturn(List.of(alreadyCancelled));

        listener.onEnrollmentClassChanged(event);

        verify(invoiceUseCase, never()).voidInvoice(any());
        verify(invoiceUseCase, never()).generateMonthly(any());
    }
}
