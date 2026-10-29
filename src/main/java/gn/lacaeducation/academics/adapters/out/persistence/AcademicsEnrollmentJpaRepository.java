package gn.lacaeducation.academics.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface AcademicsEnrollmentJpaRepository extends JpaRepository<AcademicsEnrollmentJpaEntity, Long> {

    // Retour utilisateur : "sur son dashboard l'enseignant ne voit pas la liste des élèves de la
    // classe" — un élève en inscription PENDING (pas encore validée par le secrétariat) est un vrai
    // élève présent en cours, pas une fiche fantôme ; ne pas le faire disparaître du roster. Exclut
    // seulement CANCELLED/ABANDONED, même convention que StudentDataProvider#findStudentsByClass
    // (module grading/students) — countValidatedByClassId ci-dessous reste strict, lui, pour le
    // décompte officiel de capacité.
    @Query("SELECT e.studentId FROM AcademicsEnrollmentJpaEntity e WHERE e.classId = :classId "
            + "AND e.status <> 'CANCELLED' AND e.status <> 'ABANDONED'")
    List<Long> findActiveStudentIdsByClassId(@Param("classId") Long classId);

    @Query("SELECT COUNT(e) FROM AcademicsEnrollmentJpaEntity e WHERE e.classId = :classId AND e.status = 'VALIDATED'")
    long countValidatedByClassId(@Param("classId") Long classId);

    @Query("SELECT COUNT(e) FROM AcademicsEnrollmentJpaEntity e WHERE e.classId = :classId "
            + "AND e.status <> 'CANCELLED' AND e.status <> 'ABANDONED'")
    long countActiveByClassId(@Param("classId") Long classId);
}
