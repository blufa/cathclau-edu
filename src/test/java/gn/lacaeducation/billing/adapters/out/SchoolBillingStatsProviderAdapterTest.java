package gn.lacaeducation.billing.adapters.out;

import gn.lacaeducation.billing.SchoolBillingStatsProvider.BillingStats;
import gn.lacaeducation.billing.application.port.out.InvoiceRepository;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.InvoiceStatus;
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
class SchoolBillingStatsProviderAdapterTest {

    @Mock InvoiceRepository invoiceRepository;
    @InjectMocks SchoolBillingStatsProviderAdapter adapter;

    private Invoice inv(String total, String paid, String remaining, InvoiceStatus status) {
        return Invoice.builder().totalAmount(new BigDecimal(total)).paidAmount(new BigDecimal(paid))
                .remainingBalance(new BigDecimal(remaining)).status(status).build();
    }

    @Test
    void billingStats_sumsAndComputesRecoveryRate() {
        when(invoiceRepository.findByTenantId(9L)).thenReturn(List.of(
                inv("100000", "100000", "0", InvoiceStatus.PAID),
                inv("100000", "40000", "60000", InvoiceStatus.PARTIAL),
                inv("50000", "0", "50000", InvoiceStatus.UNPAID)));

        BillingStats s = adapter.billingStats(9L);

        assertThat(s.invoiced()).isEqualByComparingTo("250000");
        assertThat(s.collected()).isEqualByComparingTo("140000");
        assertThat(s.outstanding()).isEqualByComparingTo("110000");
        // 140000 / 250000 = 56.00 %
        assertThat(s.recoveryRate()).isEqualByComparingTo("56.00");
        assertThat(s.unpaidCount()).isEqualTo(2); // PARTIAL + UNPAID
    }

    @Test
    void billingStats_noInvoices_isZero() {
        when(invoiceRepository.findByTenantId(9L)).thenReturn(List.of());
        BillingStats s = adapter.billingStats(9L);
        assertThat(s.invoiced()).isEqualByComparingTo("0");
        assertThat(s.recoveryRate()).isEqualByComparingTo("0");
        assertThat(s.unpaidCount()).isZero();
    }
}
