package gn.lacaeducation.attendance.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Dépôt JPA minimal scopé à l'unique requête nécessaire au module attendance sur les inscriptions.
 */
interface AttendanceEnrollmentJpaRepository extends JpaRepository<AttendanceEnrollmentJpaEntity, Long> {

    @Query("SELECT COUNT(e) > 0 FROM AttendanceEnrollmentJpaEntity e WHERE e.studentId = :studentId AND e.status <> 'CANCELLED'")
    boolean existsActiveByStudentId(@Param("studentId") Long studentId);
}
