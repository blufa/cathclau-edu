package gn.lacaeducation.billing.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Minimal JPA repository scoped to billing's read-only access of enrollment data.
 * Uses the same enrollments table as the students module, but with a distinct entity
 * to avoid coupling. Only queries needed for billing are defined here.
 */
interface BillingEnrollmentJpaRepository extends JpaRepository<BillingEnrollmentJpaEntity, Long> {

    @Query("SELECT e.studentId FROM BillingEnrollmentJpaEntity e WHERE e.classId = :classId AND e.status = 'VALIDATED'")
    List<Long> findValidatedStudentIdsByClassId(@Param("classId") Long classId);

    @Query("SELECT COUNT(e) FROM BillingEnrollmentJpaEntity e WHERE e.classId = :classId AND e.status = 'VALIDATED'")
    long countValidatedByClassId(@Param("classId") Long classId);

    @Query("SELECT COUNT(e) > 0 FROM BillingEnrollmentJpaEntity e WHERE e.studentId = :studentId AND e.status <> 'CANCELLED'")
    boolean existsActiveByStudentId(@Param("studentId") Long studentId);

    // VALIDATED uniquement (pas juste "non annulée") : une inscription PENDING avec l'opt-in coché ne
    // doit pas encore déclencher le frais plateforme — aligné sur StudentDataProviderAdapter, qui exige
    // déjà VALIDATED pour ce même opt-in côté notifications. Avant ce correctif, les deux pouvaient
    // diverger (frais facturé sans que la notification correspondante ne parte, ou l'inverse).
    @Query("SELECT COUNT(e) > 0 FROM BillingEnrollmentJpaEntity e WHERE e.studentId = :studentId "
            + "AND e.academicYearId = :academicYearId AND e.status = 'VALIDATED' AND e.platformAccessOptIn = true")
    boolean existsPlatformAccessOptInByStudentIdAndAcademicYearId(
            @Param("studentId") Long studentId, @Param("academicYearId") Long academicYearId);

    // VALIDATED en premier (peu importe l'id), PENDING/ABANDONED ensuite par id décroissant : sans ce
    // tri, un élève avec une inscription PENDING plus récente (ex. réinscription pour l'an prochain pas
    // encore validée) pouvait faire résoudre l'année scolaire vers CETTE inscription non validée —
    // alors que existsPlatformAccessOptInByStudentIdAndAcademicYearId/existsMonthlyPaymentOptIn...
    // exigent tous les deux VALIDATED sur l'année exacte. Résultat : la facture générée était rattachée
    // à une année où l'opt-in plateforme n'a jamais été coché (il l'est sur l'inscription VALIDÉE de
    // l'année en cours), donc aucun frais plateforme n'était jamais ajouté malgré un opt-in bien réel.
    @Query("SELECT e.academicYearId FROM BillingEnrollmentJpaEntity e WHERE e.studentId = :studentId "
            + "AND e.status <> 'CANCELLED' ORDER BY CASE WHEN e.status = 'VALIDATED' THEN 0 ELSE 1 END, e.id DESC")
    List<Long> findActiveAcademicYearIdsMostRecentFirst(@Param("studentId") Long studentId);

    // À la différence de l'opt-in plateforme ci-dessus, VALIDATED ou PENDING sont acceptés ici : ce
    // choix n'a aucun impact sur les notifications (contrairement au frais plateforme), et
    // generateMonthly() est explicitement conçu pour tourner dès l'inscription PENDING ("on peut payer
    // l'inscription + les premiers mois en même temps"). Exiger VALIDATED faisait silencieusement
    // ignorer l'opt-in « paiement par mois » choisi à l'inscription tant que celle-ci n'était pas
    // encore validée administrativement, générant des factures en tranche au lieu de mensuelles.
    @Query("SELECT COUNT(e) > 0 FROM BillingEnrollmentJpaEntity e WHERE e.studentId = :studentId "
            + "AND e.academicYearId = :academicYearId AND e.status IN ('VALIDATED', 'PENDING') "
            + "AND e.monthlyPaymentOptIn = true")
    boolean existsMonthlyPaymentOptInByStudentIdAndAcademicYearId(
            @Param("studentId") Long studentId, @Param("academicYearId") Long academicYearId);

    // Requête native (jointure directe sur les tables, pas d'entité JPA dédiée pour school_classes/
    // levels) : même ordre VALIDATED-d'abord que findActiveAcademicYearIdsMostRecentFirst, pour rester
    // cohérent avec la même notion d'« inscription active la plus récente » utilisée partout ailleurs
    // dans ce module. Renvoie le grade précis de la classe (school_classes.grade — CP1, SEPTIEME,
    // TERMINALE...) en priorité, plus le cycle/sort_order du Niveau (réduit à 3 valeurs depuis la
    // réforme du catalogue de référentiels) en repli grossier quand la classe n'a pas de grade
    // renseigné.
    @Query(value = "SELECT c.grade AS grade, l.cycle AS niveauCycle, l.sort_order AS niveauSortOrder FROM enrollments e "
            + "JOIN school_classes c ON c.id = e.class_id "
            + "JOIN levels l ON l.id = c.level_id "
            + "WHERE e.student_id = :studentId AND e.status <> 'CANCELLED' "
            + "ORDER BY CASE WHEN e.status = 'VALIDATED' THEN 0 ELSE 1 END, e.id DESC LIMIT 1",
            nativeQuery = true)
    ActiveGradeProjection findActiveLevel(@Param("studentId") Long studentId);

    interface ActiveGradeProjection {
        String getGrade();
        String getNiveauCycle();
        Integer getNiveauSortOrder();
    }
}
