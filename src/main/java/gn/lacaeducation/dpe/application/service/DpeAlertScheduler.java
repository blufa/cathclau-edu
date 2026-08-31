package gn.lacaeducation.dpe.application.service;

import gn.lacaeducation.common.DpeScopeProvider;
import gn.lacaeducation.dpe.application.port.in.DpeAlertUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Réévaluation quotidienne des alertes DPE/DCE pour toutes les préfectures/communes actives —
 * mêmes conventions que {@code DeadlineCheckScheduler}/{@code AbsenceAutoMarkScheduler}. Un échec
 * sur une préfecture ne doit jamais bloquer l'évaluation des autres.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DpeAlertScheduler {

    private final DpeScopeProvider dpeScopeProvider;
    private final DpeAlertUseCase dpeAlertUseCase;

    @Scheduled(cron = "0 0 5 * * *") // daily at 5am, before the school day
    @Transactional
    public void evaluateAllPrefectures() {
        var prefectureIds = dpeScopeProvider.activePrefectureIds();
        log.info("Évaluation quotidienne des alertes DPE : {} préfecture(s)/commune(s) active(s)", prefectureIds.size());
        for (Long prefectureId : prefectureIds) {
            try {
                var tenantIds = dpeScopeProvider.tenantIdsForPrefecture(prefectureId);
                dpeAlertUseCase.evaluateForPrefecture(prefectureId, tenantIds);
            } catch (RuntimeException e) {
                log.error("Échec de l'évaluation des alertes pour la préfecture id={} : {}", prefectureId, e.getMessage());
            }
        }
    }
}
