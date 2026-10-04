package gn.lacaeducation.audit.application.service;

import gn.lacaeducation.audit.application.event.AccountantAnomalyDetectedEvent;
import gn.lacaeducation.audit.application.port.in.AuditLogUseCase;
import gn.lacaeducation.audit.application.port.out.AccountantAnomalyAlertRepository;
import gn.lacaeducation.audit.application.port.out.AccountantMetricsPort;
import gn.lacaeducation.audit.application.port.out.AccountantMetricsPort.CashDiscrepancyMetric;
import gn.lacaeducation.audit.application.port.out.AccountantMetricsPort.ExemptionVolumeMetric;
import gn.lacaeducation.audit.domain.AccountantAnomalyAlert;
import gn.lacaeducation.audit.domain.AccountantAnomalyStatus;
import gn.lacaeducation.audit.domain.AccountantAnomalyType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Détecteur B3 (§retour fondateurs, contrôle anti-fraude comptable). */
@ExtendWith(MockitoExtension.class)
class AccountantAnomalyAlertServiceTest {

    @Mock AccountantMetricsPort metricsPort;
    @Mock AccountantAnomalyAlertRepository repository;
    @Mock AuditLogUseCase auditLogUseCase;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks AccountantAnomalyAlertService service;

    @Test
    void evaluateForTenant_fewerThanTwoEligiblePeers_noAlert() {
        when(metricsPort.cashDiscrepancyMetrics(eq(10L), any()))
                .thenReturn(List.of(new CashDiscrepancyMetric(1L, new BigDecimal("100000"), 5)));
        when(metricsPort.exemptionVolumeMetrics(eq(10L), any())).thenReturn(List.of());

        service.evaluateForTenant(10L);

        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void evaluateForTenant_oneCashierFarAboveTenantAverage_createsAlertAndNotifies() {
        when(metricsPort.cashDiscrepancyMetrics(eq(10L), any())).thenReturn(List.of(
                new CashDiscrepancyMetric(1L, new BigDecimal("900000"), 5), // bien au-dessus des pairs
                new CashDiscrepancyMetric(2L, new BigDecimal("10000"), 5),
                new CashDiscrepancyMetric(3L, new BigDecimal("5000"), 4)));
        when(metricsPort.exemptionVolumeMetrics(eq(10L), any())).thenReturn(List.of());
        when(repository.findByTenantIdAndCashierIdAndType(eq(10L), any(), eq(AccountantAnomalyType.CASH_DISCREPANCY)))
                .thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(i -> {
            AccountantAnomalyAlert a = i.getArgument(0);
            a.setId(99L);
            return a;
        });

        service.evaluateForTenant(10L);

        ArgumentCaptor<AccountantAnomalyAlert> captor = ArgumentCaptor.forClass(AccountantAnomalyAlert.class);
        verify(repository).save(captor.capture());
        AccountantAnomalyAlert saved = captor.getValue();
        assertThat(saved.getCashierId()).isEqualTo(1L);
        assertThat(saved.getType()).isEqualTo(AccountantAnomalyType.CASH_DISCREPANCY);
        assertThat(saved.getStatus()).isEqualTo(AccountantAnomalyStatus.ACTIVE);

        ArgumentCaptor<AccountantAnomalyDetectedEvent> eventCaptor = ArgumentCaptor.forClass(AccountantAnomalyDetectedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().cashierId()).isEqualTo(1L);
        assertThat(eventCaptor.getValue().type()).isEqualTo("CASH_DISCREPANCY");
    }

    @Test
    void evaluateForTenant_allCashiersSimilar_noAlert() {
        when(metricsPort.cashDiscrepancyMetrics(eq(10L), any())).thenReturn(List.of(
                new CashDiscrepancyMetric(1L, new BigDecimal("10000"), 5),
                new CashDiscrepancyMetric(2L, new BigDecimal("12000"), 5),
                new CashDiscrepancyMetric(3L, new BigDecimal("9000"), 4)));
        when(metricsPort.exemptionVolumeMetrics(eq(10L), any())).thenReturn(List.of());
        when(repository.findByTenantIdAndCashierIdAndType(eq(10L), any(), any())).thenReturn(Optional.empty());

        service.evaluateForTenant(10L);

        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void evaluateForTenant_alreadyActiveAlertStillAnomalous_updatesButNeverRenotifies() {
        when(metricsPort.cashDiscrepancyMetrics(eq(10L), any())).thenReturn(List.of(
                new CashDiscrepancyMetric(1L, new BigDecimal("900000"), 5),
                new CashDiscrepancyMetric(2L, new BigDecimal("10000"), 5),
                new CashDiscrepancyMetric(3L, new BigDecimal("5000"), 4)));
        when(metricsPort.exemptionVolumeMetrics(eq(10L), any())).thenReturn(List.of());
        AccountantAnomalyAlert existing = AccountantAnomalyAlert.builder()
                .id(5L).tenantId(10L).cashierId(1L).type(AccountantAnomalyType.CASH_DISCREPANCY)
                .metricValue(new BigDecimal("800000")).peerAverage(new BigDecimal("7000"))
                .status(AccountantAnomalyStatus.ACTIVE).createdAt(LocalDateTime.now().minusDays(7)).build();
        when(repository.findByTenantIdAndCashierIdAndType(eq(10L), eq(1L), eq(AccountantAnomalyType.CASH_DISCREPANCY)))
                .thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.evaluateForTenant(10L);

        verify(repository).save(existing);
        assertThat(existing.getMetricValue()).isEqualByComparingTo("900000");
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void evaluateForTenant_cashierBackUnderThreshold_resolvesSilently() {
        when(metricsPort.cashDiscrepancyMetrics(eq(10L), any())).thenReturn(List.of(
                new CashDiscrepancyMetric(1L, new BigDecimal("8000"), 5),
                new CashDiscrepancyMetric(2L, new BigDecimal("10000"), 5),
                new CashDiscrepancyMetric(3L, new BigDecimal("9000"), 4)));
        when(metricsPort.exemptionVolumeMetrics(eq(10L), any())).thenReturn(List.of());
        AccountantAnomalyAlert existing = AccountantAnomalyAlert.builder()
                .id(5L).tenantId(10L).cashierId(1L).type(AccountantAnomalyType.CASH_DISCREPANCY)
                .metricValue(new BigDecimal("900000")).peerAverage(new BigDecimal("7000"))
                .status(AccountantAnomalyStatus.ACTIVE).createdAt(LocalDateTime.now().minusDays(7)).build();
        when(repository.findByTenantIdAndCashierIdAndType(eq(10L), eq(1L), eq(AccountantAnomalyType.CASH_DISCREPANCY)))
                .thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.evaluateForTenant(10L);

        assertThat(existing.getStatus()).isEqualTo(AccountantAnomalyStatus.RESOLVED);
        assertThat(existing.getResolvedAt()).isNotNull();
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void evaluateForTenant_exemptionVolumeOutlier_createsAlert() {
        when(metricsPort.cashDiscrepancyMetrics(eq(10L), any())).thenReturn(List.of());
        when(metricsPort.exemptionVolumeMetrics(eq(10L), any())).thenReturn(List.of(
                new ExemptionVolumeMetric(1L, 20),
                new ExemptionVolumeMetric(2L, 2),
                new ExemptionVolumeMetric(3L, 3)));
        when(repository.findByTenantIdAndCashierIdAndType(eq(10L), any(), eq(AccountantAnomalyType.EXEMPTION_VOLUME)))
                .thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.evaluateForTenant(10L);

        ArgumentCaptor<AccountantAnomalyAlert> captor = ArgumentCaptor.forClass(AccountantAnomalyAlert.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCashierId()).isEqualTo(1L);
        assertThat(captor.getValue().getType()).isEqualTo(AccountantAnomalyType.EXEMPTION_VOLUME);
    }
}
