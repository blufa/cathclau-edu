package gn.lacaeducation.billing.adapters.out.persistence;

import gn.lacaeducation.billing.application.port.in.InvoiceUseCase;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.InvoiceStatus;
import gn.lacaeducation.students.ArrearsPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArrearsPortAdapterTest {

    @Mock InvoiceUseCase invoiceUseCase;
    @InjectMocks ArrearsPortAdapter adapter;

    @Test
    void arrears_sumsUnpaidAndPartialFromOtherYears() {
        Invoice pastUnpaid = Invoice.builder().id(1L).studentId(7L).academicYearId(19L)
                .status(InvoiceStatus.UNPAID).remainingBalance(new BigDecimal("100000")).build();
        Invoice pastPartial = Invoice.builder().id(2L).studentId(7L).academicYearId(19L)
                .status(InvoiceStatus.PARTIAL).remainingBalance(new BigDecimal("30000")).build();
        Invoice pastPaid = Invoice.builder().id(3L).studentId(7L).academicYearId(19L)
                .status(InvoiceStatus.PAID).remainingBalance(BigDecimal.ZERO).build();
        when(invoiceUseCase.findByStudentId(7L)).thenReturn(List.of(pastUnpaid, pastPartial, pastPaid));

        ArrearsPort.ArrearsInfo result = adapter.arrears(7L, 20L);

        assertThat(result.totalDue()).isEqualByComparingTo("130000");
        assertThat(result.invoiceCount()).isEqualTo(2);
    }

    @Test
    void arrears_excludesTheTargetAcademicYear() {
        Invoice targetYearUnpaid = Invoice.builder().id(1L).studentId(7L).academicYearId(20L)
                .status(InvoiceStatus.UNPAID).remainingBalance(new BigDecimal("50000")).build();
        when(invoiceUseCase.findByStudentId(7L)).thenReturn(List.of(targetYearUnpaid));

        ArrearsPort.ArrearsInfo result = adapter.arrears(7L, 20L);

        assertThat(result.totalDue()).isEqualByComparingTo("0");
        assertThat(result.invoiceCount()).isZero();
    }

    @Test
    void arrears_excludesCancelled() {
        Invoice cancelled = Invoice.builder().id(1L).studentId(7L).academicYearId(19L)
                .status(InvoiceStatus.CANCELLED).remainingBalance(BigDecimal.ZERO).build();
        when(invoiceUseCase.findByStudentId(7L)).thenReturn(List.of(cancelled));

        ArrearsPort.ArrearsInfo result = adapter.arrears(7L, 20L);

        assertThat(result.totalDue()).isEqualByComparingTo("0");
    }

    @Test
    void arrears_noInvoices_returnsZero() {
        when(invoiceUseCase.findByStudentId(7L)).thenReturn(List.of());

        ArrearsPort.ArrearsInfo result = adapter.arrears(7L, 20L);

        assertThat(result.totalDue()).isEqualByComparingTo("0");
        assertThat(result.invoiceCount()).isZero();
    }
}
