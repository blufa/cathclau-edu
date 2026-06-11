package gn.lacaeducation.academics.application.port.out;

import java.util.List;

/**
 * Port for querying enrollment data needed by the academics module.
 * Implemented by an adapter that reads directly from the enrollments table,
 * maintaining the hexagonal boundary between modules.
 */
public interface EnrollmentQueryPort {
    /**
     * Returns a list of student IDs with a VALIDATED enrollment in the given class.
     */
    List<Long> findValidatedStudentIdsByClassId(Long classId);

    /**
     * Returns the count of VALIDATED enrollments for the given class.
     */
    long countValidatedByClassId(Long classId);
}
