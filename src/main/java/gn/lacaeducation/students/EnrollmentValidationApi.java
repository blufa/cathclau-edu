package gn.lacaeducation.students;

/**
 * Public inbound API of the students module letting other modules validate an enrollment as a
 * side effect of their own workflow. Used by billing: once the ENROLLMENT invoice is fully paid,
 * the matching PENDING enrollment is validated (capacity and one-per-year rules still apply).
 */
public interface EnrollmentValidationApi {
    /** Validate the PENDING enrollment of {@code studentId} in {@code classId}; no-op if none/already validated. */
    void validatePaidEnrollment(Long studentId, Long classId);
}
