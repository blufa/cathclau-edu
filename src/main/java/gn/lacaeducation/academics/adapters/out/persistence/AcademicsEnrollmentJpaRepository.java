package gn.lacaeducation.academics.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface AcademicsEnrollmentJpaRepository extends JpaRepository<AcademicsEnrollmentJpaEntity, Long> {

    @Query("SELECT e.studentId FROM AcademicsEnrollmentJpaEntity e WHERE e.classId = :classId AND e.status = 'VALIDATED'")
    List<Long> findValidatedStudentIdsByClassId(@Param("classId") Long classId);

    @Query("SELECT COUNT(e) FROM AcademicsEnrollmentJpaEntity e WHERE e.classId = :classId AND e.status = 'VALIDATED'")
    long countValidatedByClassId(@Param("classId") Long classId);
}
