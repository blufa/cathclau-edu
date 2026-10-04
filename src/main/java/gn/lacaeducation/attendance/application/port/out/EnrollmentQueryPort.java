package gn.lacaeducation.attendance.application.port.out;

/**
 * Port pour interroger les inscriptions depuis le module students, sans dépendre de ses beans
 * (adaptateur lisant directement la table partagée {@code enrollments} — même principe que
 * {@code billing.application.port.out.EnrollmentQueryPort}).
 */
public interface EnrollmentQueryPort {

    /** L'élève a-t-il au moins une inscription non annulée (PENDING ou VALIDATED) ? */
    boolean hasActiveEnrollment(Long studentId);
}
