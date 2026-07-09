package gn.lacaeducation.grading;

import java.math.BigDecimal;
import java.util.List;

/**
 * API publique du module grading pour le reporting académique (§10.1) : agrégats des bulletins d'une
 * période (taux de réussite, moyennes par classe, classement global). Renvoie des identifiants bruts ;
 * l'enrichissement des libellés (élèves, classes) est fait par le module appelant.
 */
public interface SchoolAcademicStatsProvider {

    /** Agrégats d'une classe pour la période. */
    record ClassStat(Long classId, int studentCount, BigDecimal classAverage,
                     BigDecimal highestAverage, BigDecimal lowestAverage, BigDecimal passRate) {}

    /** Un élève du classement global (moyenne générale décroissante). */
    record RankedStudent(Long studentId, Long classId, BigDecimal overallAverage, Integer rank) {}

    /**
     * Statistiques académiques d'une période.
     *
     * @param periodId       période concernée
     * @param studentCount   nombre d'élèves ayant un bulletin
     * @param overallAverage moyenne générale de la période (moyenne des moyennes générales)
     * @param passRate       taux de réussite global (part de décisions « ADMIS », en %)
     * @param classStats     agrégats par classe
     * @param topStudents    meilleurs élèves (classement global, borné)
     */
    record AcademicStats(Long periodId, int studentCount, BigDecimal overallAverage, BigDecimal passRate,
                         List<ClassStat> classStats, List<RankedStudent> topStudents) {}

    AcademicStats academicStats(Long periodId, int topLimit);
}
