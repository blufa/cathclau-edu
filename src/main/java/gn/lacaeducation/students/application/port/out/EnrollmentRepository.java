package gn.lacaeducation.students.application.port.out;

import gn.lacaeducation.students.domain.Enrollment;
import gn.lacaeducation.students.domain.EnrollmentStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository {
    Enrollment save(Enrollment enrollment);
    Optional<Enrollment> findById(Long id);
    List<Enrollment> findByStudentId(Long studentId);
    List<Enrollment> findByClassId(Long classId);
    List<Enrollment> findByAcademicYearId(Long academicYearId);
    /** Inscriptions dont la date (saisie) tombe ce jour-là, tenant courant (§retour comptable : "ce
     * jour tu inscris tant d'élèves") — {@code enrollments} n'a pas de tenant_id propre, filtré via
     * l'élève. */
    List<Enrollment> findByTenantIdAndEnrolledAt(Long tenantId, LocalDate date);
    void deleteById(Long id);
    boolean existsById(Long id);
    long countByClassIdAndStatus(Long classId, EnrollmentStatus status);
    boolean existsValidatedForStudentYear(Long studentId, Long academicYearId);
    /** True when the student already has a non-cancelled (PENDING or VALIDATED) enrollment for that year. */
    boolean existsActiveForStudentYear(Long studentId, Long academicYearId);
    /** The single PENDING enrollment for a student in a class, if any (used when a payment validates it). */
    Optional<Enrollment> findPendingByStudentAndClass(Long studentId, Long classId);
    List<Enrollment> search(Long studentId, Long classId, Long academicYearId, EnrollmentStatus status);
}
