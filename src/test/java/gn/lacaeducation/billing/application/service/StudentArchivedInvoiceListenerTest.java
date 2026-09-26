package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.port.in.InvoiceUseCase;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.InvoiceStatus;
import gn.lacaeducation.students.application.event.StudentArchivedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentArchivedInvoiceListenerTest {

    @Mock InvoiceUseCase invoiceUseCase;
    @InjectMocks StudentArchivedInvoiceListener listener;

    private final StudentArchivedEvent event = new StudentArchivedEvent(1L, 7L);

    @Test
    void voidsUnpaidAndPartialInvoices_acrossAllYears_leavesPaidUntouched() {
        Invoice unpaidThisYear = Invoice.builder().id(100L).studentId(7L).academicYearId(99L)
                .status(InvoiceStatus.UNPAID).totalAmount(new BigDecimal("50.00")).paidAmount(BigDecimal.ZERO).build();
        Invoice partialLastYear = Invoice.builder().id(101L).studentId(7L).academicYearId(88L)
                .status(InvoiceStatus.PARTIAL).totalAmount(new BigDecimal("50.00")).paidAmount(new BigDecimal("20.00")).build();
        Invoice paid = Invoice.builder().id(102L).studentId(7L).academicYearId(99L)
                .status(InvoiceStatus.PAID).totalAmount(new BigDecimal("50.00")).paidAmount(new BigDecimal("50.00")).build();
        when(invoiceUseCase.findByStudentId(7L)).thenReturn(List.of(unpaidThisYear, partialLastYear, paid));

        listener.onStudentArchived(event);

        verify(invoiceUseCase).voidInvoice(100L);
        verify(invoiceUseCase).voidInvoice(101L);
        verify(invoiceUseCase, never()).voidInvoice(102L);
    }

    @Test
    void alreadyCancelledInvoices_areIgnored() {
        Invoice alreadyCancelled = Invoice.builder().id(100L).studentId(7L).academicYearId(99L)
                .status(InvoiceStatus.CANCELLED).totalAmount(new BigDecimal("50.00")).paidAmount(BigDecimal.ZERO).build();
        when(invoiceUseCase.findByStudentId(7L)).thenReturn(List.of(alreadyCancelled));

        listener.onStudentArchived(event);

        verify(invoiceUseCase, never()).voidInvoice(any());
    }

    @Test
    void noDueInvoices_doesNothing() {
        when(invoiceUseCase.findByStudentId(7L)).thenReturn(List.of());

        listener.onStudentArchived(event);

        verify(invoiceUseCase, never()).voidInvoice(any());
    }
}
