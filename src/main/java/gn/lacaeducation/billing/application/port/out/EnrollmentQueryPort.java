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
}
