package gn.lacaeducation.billing.adapters.out.persistence;

import gn.lacaeducation.billing.application.port.out.EnrollmentQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adapter that reads directly from the enrollments table to find validated students.
 * This avoids importing beans from the students module (hexagonal boundary).
 */
@Component
@RequiredArgsConstructor
class EnrollmentQueryAdapter implements EnrollmentQueryPort {

    private final BillingEnrollmentJpaRepository enrollmentJpaRepository;

    @Override
    public List<Long> findValidatedStudentIdsByClassId(Long classId) {
        return enrollmentJpaRepository.findValidatedStudentIdsByClassId(classId);
    }
}
