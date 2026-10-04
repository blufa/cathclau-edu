package gn.lacaeducation.audit.application.service;

import gn.lacaeducation.audit.application.event.AccountantAnomalyDetectedEvent;
import gn.lacaeducation.audit.application.port.in.AccountantAnomalyAlertUseCase;
import gn.lacaeducation.audit.application.port.in.AuditLogUseCase;
import gn.lacaeducation.audit.application.port.in.AuditLogUseCase.RecordAuditCommand;
import gn.lacaeducation.audit.application.port.out.AccountantAnomalyAlertRepository;
import gn.lacaeducation.audit.application.port.out.AccountantMetricsPort;
import gn.lacaeducation.audit.domain.AccountantAnomalyAlert;
import gn.lacaeducation.audit.domain.AccountantAnomalyStatus;
import gn.lacaeducation.audit.domain.AccountantAnomalyType;
import gn.lacaeducation.common.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Détecteur B3 (§retour fondateurs, contrôle anti-fraude comptable) : compare chaque comptable d'un
 * établissement à la moyenne de ses pairs du MÊME établissement, sur deux signaux déjà attribuables
 * à un comptable précis — écarts de caisse négatifs (manquants) et volume d'exonérations accordées
 * (voir CashReconciliationGuard/ExemptionGrantedEvent pour les détecteurs B1/B2/A1-A2 déjà livrés).
 * Les remboursements ne sont pas inclus dans cette itération : aucun champ ne retient aujourd'hui
 * qui effectue un remboursement.
 *
 * <p>Même pattern idempotent que {@code DpeAlertService#rule} (module dpe) : une ligne par
 * (tenant, comptable, type), créée/réouverte si l'anomalie est détectée, résolue automatiquement et
 * silencieusement sinon. Le SMS (voir {@code AccountantAnomalyDetectedEvent}) ne part qu'à la
 * création/réouverture, jamais à chaque réévaluation hebdomadaire d'une alerte déjà active.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AccountantAnomalyAlertService implements AccountantAnomalyAlertUseCase {

    /** Fenêtre glissante d'observation. */
    private static final int WINDOW_DAYS = 30;
    /** Un comptable avec moins de sessions/exonérations que ce seuil n'est ni comparé, ni retenu
     * comme pair de comparaison pour les autres — trop peu de données pour être significatif. */
    private static final long MIN_SESSIONS_FOR_ELIGIBILITY = 3;
    private static final long MIN_EXEMPTIONS_FOR_ELIGIBILITY = 2;
    /** Il faut au moins deux comptables éligibles pour qu'une comparaison ait un sens. */
    private static final int MIN_PEERS_FOR_COMPARISON = 2;
    /** Multiple de la moyenne des pairs au-delà duquel un comptable est jugé anormal. */
    private static final BigDecimal ANOMALY_MULTIPLIER = BigDecimal.valueOf(3);

    private final AccountantMetricsPort metricsPort;
    private final AccountantAnomalyAlertRepository repository;
    private final AuditLogUseCase auditLogUseCase;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void evaluateForTenant(Long tenantId) {
        LocalDateTime since = LocalDateTime.now().minusDays(WINDOW_DAYS);
        evaluateCashDiscrepancies(tenantId, since);
        evaluateExemptionVolumes(tenantId, since);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountantAnomalyAlert> findForCurrentTenant() {
        return repository.findByTenantId(TenantContext.get());
    }

    private void evaluateCashDiscrepancies(Long tenantId, LocalDateTime since) {
        List<AccountantMetricsPort.CashDiscrepancyMetric> metrics = metricsPort.cashDiscrepancyMetrics(tenantId, since)
                .stream().filter(m -> m.sessionCount() >= MIN_SESSIONS_FOR_ELIGIBILITY).toList();
        if (metrics.size() < MIN_PEERS_FOR_COMPARISON) return;

        for (AccountantMetricsPort.CashDiscrepancyMetric m : metrics) {
            BigDecimal peerAverage = averageExcluding(
                    metrics.stream().map(AccountantMetricsPort.CashDiscrepancyMetric::missingTotal).toList(),
                    m.missingTotal());
            boolean anomalous = m.missingTotal().signum() > 0 && isAnomalous(m.missingTotal(), peerAverage);
            rule(tenantId, m.cashierId(), AccountantAnomalyType.CASH_DISCREPANCY, anomalous, m.missingTotal(), peerAverage);
        }
    }

    private void evaluateExemptionVolumes(Long tenantId, LocalDateTime since) {
        List<AccountantMetricsPort.ExemptionVolumeMetric> metrics = metricsPort.exemptionVolumeMetrics(tenantId, since)
                .stream().filter(m -> m.exemptionCount() >= MIN_EXEMPTIONS_FOR_ELIGIBILITY).toList();
        if (metrics.size() < MIN_PEERS_FOR_COMPARISON) return;

        for (AccountantMetricsPort.ExemptionVolumeMetric m : metrics) {
            BigDecimal value = BigDecimal.valueOf(m.exemptionCount());
            BigDecimal peerAverage = averageExcluding(
                    metrics.stream().map(x -> BigDecimal.valueOf(x.exemptionCount())).toList(), value);
            boolean anomalous = isAnomalous(value, peerAverage);
            rule(tenantId, m.cashierId(), AccountantAnomalyType.EXEMPTION_VOLUME, anomalous, value, peerAverage);
        }
    }

    /** Anormal si la moyenne des pairs est nulle (n'importe quelle valeur positive est alors un
     * multiple infini) ou si la valeur dépasse ANOMALY_MULTIPLIER fois cette moyenne. */
    private boolean isAnomalous(BigDecimal value, BigDecimal peerAverage) {
        if (value.signum() <= 0) return false;
        if (peerAverage.signum() == 0) return true;
        return value.compareTo(peerAverage.multiply(ANOMALY_MULTIPLIER)) > 0;
    }

    private BigDecimal averageExcluding(List<BigDecimal> all, BigDecimal self) {
        BigDecimal sumOthers = BigDecimal.ZERO;
        int countOthers = 0;
        boolean removedSelf = false;
        for (BigDecimal v : all) {
            if (!removedSelf && v.compareTo(self) == 0) { removedSelf = true; continue; }
            sumOthers = sumOthers.add(v);
            countOthers++;
        }
        if (countOthers == 0) return BigDecimal.ZERO;
        return sumOthers.divide(BigDecimal.valueOf(countOthers), 2, RoundingMode.HALF_UP);
    }

    /** Crée/réouvre l'alerte si {@code conditionTrue} (SMS + audit uniquement à ce moment-là), la
     * résout automatiquement et silencieusement sinon — même esprit que {@code DpeAlertService#rule}. */
    private void rule(Long tenantId, Long cashierId, AccountantAnomalyType type, boolean conditionTrue,
                       BigDecimal metricValue, BigDecimal peerAverage) {
        Optional<AccountantAnomalyAlert> existing = repository.findByTenantIdAndCashierIdAndType(tenantId, cashierId, type);
        if (conditionTrue) {
            if (existing.isPresent()) {
                AccountantAnomalyAlert alert = existing.get();
                boolean wasResolved = alert.getStatus() == AccountantAnomalyStatus.RESOLVED;
                alert.setMetricValue(metricValue);
                alert.setPeerAverage(peerAverage);
                alert.setUpdatedAt(LocalDateTime.now());
                if (wasResolved) {
                    alert.setStatus(AccountantAnomalyStatus.ACTIVE);
                    alert.setResolvedAt(null);
                }
                AccountantAnomalyAlert saved = repository.save(alert);
                if (wasResolved) notifyNewAnomaly(saved);
            } else {
                AccountantAnomalyAlert saved = repository.save(AccountantAnomalyAlert.builder()
                        .tenantId(tenantId).cashierId(cashierId).type(type)
                        .metricValue(metricValue).peerAverage(peerAverage)
                        .status(AccountantAnomalyStatus.ACTIVE).createdAt(LocalDateTime.now()).build());
                notifyNewAnomaly(saved);
            }
        } else {
            existing.filter(a -> a.getStatus() != AccountantAnomalyStatus.RESOLVED).ifPresent(alert -> {
                alert.setStatus(AccountantAnomalyStatus.RESOLVED);
                alert.setResolvedAt(LocalDateTime.now());
                alert.setUpdatedAt(LocalDateTime.now());
                repository.save(alert);
                auditLogUseCase.record(new RecordAuditCommand(tenantId, cashierId, "ACCOUNTANT_ANOMALY_RESOLVED",
                        "AccountantAnomalyAlert", alert.getId(), "type=" + type));
            });
        }
    }

    private void notifyNewAnomaly(AccountantAnomalyAlert alert) {
        auditLogUseCase.record(new RecordAuditCommand(alert.getTenantId(), alert.getCashierId(),
                "ACCOUNTANT_ANOMALY_DETECTED", "AccountantAnomalyAlert", alert.getId(),
                "type=" + alert.getType() + ";metricValue=" + alert.getMetricValue()
                        + ";peerAverage=" + alert.getPeerAverage()));
        eventPublisher.publishEvent(new AccountantAnomalyDetectedEvent(alert.getTenantId(), alert.getCashierId(),
                alert.getType().name(), alert.getMetricValue(), alert.getPeerAverage()));
    }
}
