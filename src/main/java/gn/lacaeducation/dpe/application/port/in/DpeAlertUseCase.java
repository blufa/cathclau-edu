package gn.lacaeducation.dpe.application.port.in;

import gn.lacaeducation.dpe.domain.DpeAlert;
import gn.lacaeducation.dpe.domain.DpeAlertStatus;

import java.util.List;
import java.util.Set;

/**
 * Alertes DPE/DCE (features/dpe.md §13) — évaluées automatiquement (job planifié ou déclenchement
 * manuel via {@link #evaluateForPrefecture}) à partir des mêmes données que le tableau de bord
 * (§DpeDashboardUseCase). Jamais générées à la demande d'une simple lecture : {@code
 * findForPrefecture} ne fait que lire l'état déjà persisté.
 */
public interface DpeAlertUseCase {

    /** @param statutFilter null = toutes les alertes, quel que soit leur statut */
    List<DpeAlert> findForPrefecture(Long prefectureId, DpeAlertStatus statutFilter);

    /** 404 si l'alerte n'existe pas, 403 si elle n'appartient pas à {@code prefectureId}. */
    DpeAlert updateStatus(Long prefectureId, Long alertId, DpeAlertStatus newStatus, Long userId);

    /** Réévalue toutes les règles pour les écoles de {@code tenantIds} : crée/réouvre les alertes dont
     * la condition est vraie, résout automatiquement celles dont la condition n'est plus vraie. */
    void evaluateForPrefecture(Long prefectureId, Set<Long> tenantIds);
}
