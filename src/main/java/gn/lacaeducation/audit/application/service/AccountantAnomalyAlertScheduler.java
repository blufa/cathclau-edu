package gn.lacaeducation.audit.application.service;

import gn.lacaeducation.audit.application.port.in.AccountantAnomalyAlertUseCase;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.platform.TenantListPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Réévaluation hebdomadaire du détecteur B3 (§retour fondateurs) pour tous les tenants actifs —
 * mêmes conventions que {@code AbsenceAutoMarkScheduler}/{@code UndeclaredEnrollmentDetectionScheduler} :
 * boucle par tenant, restaure {@link TenantContext}, un échec sur un tenant n'empêche jamais
 * l'évaluation des autres. Hebdomadaire (pas quotidien) : les deux signaux ont besoin de volume
 * (fenêtre de 30 jours) pour être statistiquement significatifs — une réévaluation quotidienne
 * n'apporterait aucune donnée nouvelle la plupart des jours.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountantAnomalyAlertScheduler {

    private final TenantListPort tenantListPort;
    private final AccountantAnomalyAlertUseCase accountantAnomalyAlertUseCase;

    @Scheduled(cron = "0 0 6 ? * MON") // lundi 6h, avant le début de la semaine scolaire
    @Transactional
    public void evaluateAllTenants() {
        for (TenantListPort.TenantRef tenant : tenantListPort.listActiveTenantIds()) {
            TenantContext.set(tenant.id());
            try {
                accountantAnomalyAlertUseCase.evaluateForTenant(tenant.id());
            } catch (Exception e) {
                log.error("Détection B3 (écarts comptables) en échec pour tenant {} : {}", tenant.id(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }
}
