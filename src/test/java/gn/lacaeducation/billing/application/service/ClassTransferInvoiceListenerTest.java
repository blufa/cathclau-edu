package gn.lacaeducation.billing.application.service;

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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClassTransferInvoiceListenerTest {

    @Mock SchoolFeeRepository schoolFeeRepository;
    @Mock InvoiceUseCase invoiceUseCase;
    @Mock ApplicationEventPublisher eventPublisher;
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

        Invoice newInv1 = Invoice.builder().id(200L).installmentId(1L).totalAmount(new BigDecimal("40.00")).build();
        Invoice newInv2 = Invoice.builder().id(201L).installmentId(2L).totalAmount(new BigDecimal("40.00")).build();
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
        Invoice newInv = Invoice.builder().id(200L).installmentId(1L).totalAmount(new BigDecimal("40.00")).build();
        when(invoiceUseCase.generateMonthly(any(GenerateMonthlyCommand.class)))
                .thenReturn(new InvoiceUseCase.GenerateMonthlyResult(List.of(newInv), List.of()));

        listener.onEnrollmentClassChanged(event);

        verify(invoiceUseCase).applyCredit(eq(200L), eq(new BigDecimal("40.00")), anyString());
        // Reliquat = 100.00 - 40.00 = 60.00, jamais affecté à aucune facture.
        verify(eventPublisher).publishEvent(new ClassTransferCreditSurplusEvent(10L, 7L, 20L, 21L, new BigDecimal("60.00")));
    }

    /**
     * Régression réelle (§retour comptable, transfert testé en conditions réelles) : une version
     * antérieure annulait aussi les factures ANNEXES (hors mensualité) de l'ancienne classe, mais
     * sans jamais les régénérer pour la nouvelle — une facture annexe encore impayée disparaissait
     * purement et simplement au lieu de rester "à régler". Vérifie que ces factures annexes ne sont
     * plus touchées du tout par un transfert de classe.
     */
    @Test
    void doesNotTouchAnnexInvoices_otherThanMonthly() {
        when(schoolFeeRepository.findByClassIdAndType(20L, FeeType.MONTHLY)).thenReturn(List.of());

        Invoice revisionInvoice = Invoice.builder().id(300L).reference("REF-REV").studentId(7L).feeId(30L).academicYearId(99L)
                .status(InvoiceStatus.UNPAID).totalAmount(new BigDecimal("50000")).paidAmount(BigDecimal.ZERO).build();
        Invoice enrollmentInvoice = Invoice.builder().id(301L).reference("REF-INS").studentId(7L).feeId(31L).academicYearId(99L)
                .status(InvoiceStatus.PAID).totalAmount(new BigDecimal("250000")).paidAmount(new BigDecimal("250000")).build();
        when(invoiceUseCase.findByStudentId(7L)).thenReturn(List.of(revisionInvoice, enrollmentInvoice));

        listener.onEnrollmentClassChanged(event);

        verify(invoiceUseCase, never()).voidInvoice(300L);
        verify(invoiceUseCase, never()).voidInvoice(301L);
        verify(schoolFeeRepository, never()).findByClassId(any());
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
