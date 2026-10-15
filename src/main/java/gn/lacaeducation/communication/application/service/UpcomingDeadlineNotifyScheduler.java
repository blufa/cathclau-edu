package gn.lacaeducation.communication.application.service;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.platform.TenantListPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job quotidien : relance INTERNE au personnel (pas le SMS parent déjà existant via
 * {@code DeadlineCheckScheduler}/{@code EcheanceDepasseeEvent}, qui ne couvre que les échéances DÉJÀ
 * en retard) — le comptable n'avait aucun rappel poussé pour une échéance qui APPROCHE (élèves en
 * mensualité/tranches), seulement la possibilité d'aller consulter "Suivi des paiements" de sa propre
 * initiative (§retour comptable). Une seule notification groupée par jour par utilisateur concerné
 * (pas une par élève, confirmé par l'utilisateur) — voir {@link UpcomingDeadlineNotifyProcessor} pour
 * le calcul et la publication (dans sa propre transaction, condition pour que
 * {@code @TransactionalEventListener} se déclenche).
 *
 * <p>Même motif que {@code DeadlineCheckScheduler} (hors contexte de requête, boucle explicitement par
 * tenant en restaurant {@link TenantContext}) — exécuté après lui (7h, contre 6h pour les retards) pour
 * que les deux jobs ne se chevauchent jamais sur le même tenant.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UpcomingDeadlineNotifyScheduler {

    private final TenantListPort tenantListPort;
    private final UpcomingDeadlineNotifyProcessor processor;

    @Scheduled(cron = "0 0 7 * * *") // quotidien à 7h, après le job des retards (6h)
    public void notifyUpcomingDeadlines() {
        for (TenantListPort.TenantRef tenant : tenantListPort.listActiveTenantIds()) {
            TenantContext.set(tenant.id());
            try {
                processor.notifyForTenant(tenant.id());
            } catch (Exception e) {
                log.error("Upcoming deadline digest failed for tenant {}: {}", tenant.id(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }

    /** Rejoue le traitement pour UN SEUL tenant (voir contrôleur, endpoint de test
     * "notify-upcoming-now") — délègue au même processeur transactionnel que la boucle planifiée,
     * jamais une logique dupliquée ici. */
    public void notifyForTenant(Long tenantId) {
        processor.notifyForTenant(tenantId);
    }
}
