package gn.lacaeducation.billing.application.port.out;

import java.util.List;

/**
 * Port for querying enrollment data from the students module.
 * Implemented by an adapter that reads directly from the enrollments table,
 * avoiding direct coupling to the students module beans.
 */
public interface EnrollmentQueryPort {
    /**
     * Returns the IDs of all students with a VALIDATED enrollment in the given class.
     */
    List<Long> findValidatedStudentIdsByClassId(Long classId);

    /** Whether the student has at least one non-cancelled enrollment (PENDING or VALIDATED). */
    boolean hasActiveEnrollment(Long studentId);

    /** Le parent a-t-il accepté l'accès à la plateforme pour l'inscription de cet élève sur cette
     * année scolaire ? Conditionne l'ajout du frais d'usage plateforme aux mensualités (§billing). */
    boolean hasPlatformAccessOptIn(Long studentId, Long academicYearId);
}
