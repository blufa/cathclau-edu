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

    @Query("SELECT e.academicYearId FROM BillingEnrollmentJpaEntity e WHERE e.studentId = :studentId "
            + "AND e.status <> 'CANCELLED' ORDER BY e.id DESC")
    List<Long> findActiveAcademicYearIdsMostRecentFirst(@Param("studentId") Long studentId);
}
