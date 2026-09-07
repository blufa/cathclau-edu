package gn.lacaeducation.lessons;

import java.time.LocalDate;
import java.util.Map;

/**
 * API publique du cahier de textes pour la paie : heures de cours RÉALISÉES par un enseignant
 * (séances dont le statut est passé à « réalisée »).
 */
public interface TeacherTeachingProvider {
    /** Total des minutes des séances réalisées (DONE) de l'enseignant sur la période [from, to]. */
    int realizedMinutes(Long teacherId, LocalDate from, LocalDate to);

    /** Comme {@link #realizedMinutes}, mais regroupé par classe (clé = classId) — permet à la paie
     * d'appliquer le tarif horaire correspondant à la typologie de chaque classe (normal/examen). */
    Map<Long, Integer> realizedMinutesByClass(Long teacherId, LocalDate from, LocalDate to);
}
