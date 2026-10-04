package gn.lacaeducation.audit.adapters.out.persistence;

import gn.lacaeducation.audit.application.port.out.AccountantMetricsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
class AccountantMetricsAdapter implements AccountantMetricsPort {

    private final AuditCashierSessionJpaRepository cashierSessionJpaRepository;
    private final AuditExemptionJpaRepository exemptionJpaRepository;

    @Override
    public List<CashDiscrepancyMetric> cashDiscrepancyMetrics(Long tenantId, LocalDateTime since) {
        return cashierSessionJpaRepository.aggregateDiscrepancies(tenantId, since).stream()
                .map(p -> new CashDiscrepancyMetric(p.getCashierId(), p.getMissingTotal(), p.getSessionCount()))
                .toList();
    }

    @Override
    public List<ExemptionVolumeMetric> exemptionVolumeMetrics(Long tenantId, LocalDateTime since) {
        return exemptionJpaRepository.aggregateVolumes(tenantId, since).stream()
                .map(p -> new ExemptionVolumeMetric(p.getCashierId(), p.getExemptionCount()))
                .toList();
    }
}
