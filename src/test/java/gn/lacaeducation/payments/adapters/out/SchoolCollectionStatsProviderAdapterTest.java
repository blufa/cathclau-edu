package gn.lacaeducation.payments.adapters.out;

import gn.lacaeducation.payments.SchoolCollectionStatsProvider.DailyCollectionLine;
import gn.lacaeducation.payments.application.port.out.PaymentRepository;
import gn.lacaeducation.payments.domain.Payment;
import gn.lacaeducation.payments.domain.PaymentChannel;
import gn.lacaeducation.payments.domain.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * §retour comptable : "voici les factures encaissées, voici l'argent collecté" pour un jour précis —
 * détail ligne par ligne, contrairement à {@code monthlyCollections} qui n'agrège qu'un total par mois.
 */
@ExtendWith(MockitoExtension.class)
class SchoolCollectionStatsProviderAdapterTest {

    @Mock PaymentRepository paymentRepository;
    @InjectMocks SchoolCollectionStatsProviderAdapter adapter;

    private Payment payment(Long id, Long tenantId, Long studentId, String amount, PaymentStatus status) {
        return Payment.builder().id(id).tenantId(tenantId).studentId(studentId)
                .amount(new BigDecimal(amount)).status(status).channel(PaymentChannel.CASH)
                .reference("REF-" + id).paidAt(LocalDateTime.of(2026, 11, 2, 10, 0)).build();
    }

    @Test
    void dailyCollectionLines_onlySucceededAndMatchingTenant() {
        LocalDate day = LocalDate.of(2026, 11, 2);
        when(paymentRepository.findByPaidAtBetween(day.atStartOfDay(), day.plusDays(1).atStartOfDay().minusNanos(1)))
                .thenReturn(List.of(
                        payment(1L, 10L, 53L, "10000", PaymentStatus.SUCCEEDED),
                        payment(2L, 10L, 54L, "20000", PaymentStatus.FAILED),
                        payment(3L, 99L, 55L, "30000", PaymentStatus.SUCCEEDED)));

        List<DailyCollectionLine> result = adapter.dailyCollectionLines(10L, day);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).paymentId()).isEqualTo(1L);
        assertThat(result.get(0).studentId()).isEqualTo(53L);
        assertThat(result.get(0).amount()).isEqualByComparingTo("10000");
        assertThat(result.get(0).channel()).isEqualTo("CASH");
    }

    @Test
    void dailyCollectionLines_noPayments_returnsEmptyList() {
        when(paymentRepository.findByPaidAtBetween(any(), any())).thenReturn(List.of());

        List<DailyCollectionLine> result = adapter.dailyCollectionLines(10L, LocalDate.now());

        assertThat(result).isEmpty();
    }

    @Test
    void collectedTotal_onlySumsPaymentsFromTheRequestedMonth() {
        Payment october = Payment.builder().tenantId(10L).amount(new BigDecimal("500000"))
                .status(PaymentStatus.SUCCEEDED).paidAt(LocalDateTime.of(2026, 10, 15, 9, 0)).build();
        Payment november = Payment.builder().tenantId(10L).amount(new BigDecimal("300000"))
                .status(PaymentStatus.SUCCEEDED).paidAt(LocalDateTime.of(2026, 11, 2, 9, 0)).build();
        when(paymentRepository.findByTenantIdAndStatus(10L, PaymentStatus.SUCCEEDED))
                .thenReturn(List.of(october, november));

        BigDecimal result = adapter.collectedTotal(10L, 2026, 11);

        assertThat(result).isEqualByComparingTo("300000");
    }
}
