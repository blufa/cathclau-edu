package gn.lacaeducation.academics.application.port.out;

import java.util.List;

/**
 * Port for querying enrollment data needed by the academics module.
 * Implemented by an adapter that reads directly from the enrollments table,
 * maintaining the hexagonal boundary between modules.
 */
public interface EnrollmentQueryPort {
    /**
     * Returns the count of VALIDATED enrollments for the given class.
     */
    long countValidatedByClassId(Long classId);

    /**
     * Returns the count of ACTIVE enrollments (any status except CANCELLED/ABANDONED) for the given
     * class — used to block deleting a class that still has real students in it, PENDING enrollments
     * included (a class isn't "empty" just because the secretariat hasn't validated every enrollment
     * yet).
     */
    long countActiveByClassId(Long classId);
}
