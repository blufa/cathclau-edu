package gn.lacaeducation.attendance.adapters.out.persistence;

import gn.lacaeducation.attendance.application.port.out.EnrollmentQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Adaptateur qui lit directement la table enrollments pour éviter une dépendance aux beans du
 * module students (frontière hexagonale).
 */
@Component("attendanceEnrollmentQueryAdapter")
@RequiredArgsConstructor
class EnrollmentQueryAdapter implements EnrollmentQueryPort {

    private final AttendanceEnrollmentJpaRepository enrollmentJpaRepository;

    @Override
    public boolean hasActiveEnrollment(Long studentId) {
        return enrollmentJpaRepository.existsActiveByStudentId(studentId);
    }
}
