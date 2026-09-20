package gn.lacaeducation.communication.application.service;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.communication.application.port.in.ScheduledBroadcastUseCase;
import gn.lacaeducation.platform.TenantListPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Déclenche les diffusions de masse programmées ({@code ScheduledBroadcast}) dont l'heure d'envoi est
 * passée. S'exécute hors contexte de requête : boucle explicitement par tenant en restaurant
 * {@link TenantContext}, comme {@code AbsenceAutoMarkScheduler}/{@code DeadlineCheckScheduler}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledBroadcastDispatchScheduler {

    private final TenantListPort tenantListPort;
    private final ScheduledBroadcastUseCase scheduledBroadcastUseCase;

    @Scheduled(cron = "0 * * * * *") // chaque minute : la précision attendue par l'utilisateur qui programme "à cette heure"
    public void dispatchDueBroadcasts() {
        LocalDateTime now = LocalDateTime.now();
        for (TenantListPort.TenantRef tenant : tenantListPort.listActiveTenantIds()) {
            TenantContext.set(tenant.id());
            try {
                scheduledBroadcastUseCase.processDueForTenant(tenant.id(), now);
            } catch (Exception e) {
                log.error("Dispatch des diffusions programmées en échec pour tenant {} : {}", tenant.id(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }
}
