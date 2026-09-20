package gn.lacaeducation.billing.adapters.out.persistence;

import gn.lacaeducation.billing.application.port.out.EnrollmentQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adapter that reads directly from the enrollments table to find validated students.
 * This avoids importing beans from the students module (hexagonal boundary).
 */
@Component("billingEnrollmentQueryAdapter")
@RequiredArgsConstructor
class EnrollmentQueryAdapter implements EnrollmentQueryPort {

    private final BillingEnrollmentJpaRepository enrollmentJpaRepository;

    @Override
    public List<Long> findValidatedStudentIdsByClassId(Long classId) {
        return enrollmentJpaRepository.findValidatedStudentIdsByClassId(classId);
    }

    @Override
    public boolean hasActiveEnrollment(Long studentId) {
        return enrollmentJpaRepository.existsActiveByStudentId(studentId);
    }

    @Override
    public boolean hasPlatformAccessOptIn(Long studentId, Long academicYearId) {
        return enrollmentJpaRepository.existsPlatformAccessOptInByStudentIdAndAcademicYearId(studentId, academicYearId);
    }

    @Override
    public boolean hasMonthlyPaymentOptIn(Long studentId, Long academicYearId) {
        return enrollmentJpaRepository.existsMonthlyPaymentOptInByStudentIdAndAcademicYearId(studentId, academicYearId);
    }

    @Override
    public Long findActiveAcademicYearId(Long studentId) {
        List<Long> ids = enrollmentJpaRepository.findActiveAcademicYearIdsMostRecentFirst(studentId);
        return ids.isEmpty() ? null : ids.get(0);
    }

    @Override
    public ActiveLevel findActiveLevel(Long studentId) {
        BillingEnrollmentJpaRepository.ActiveLevelProjection p = enrollmentJpaRepository.findActiveLevel(studentId);
        return p == null ? null : new ActiveLevel(p.getCycle(), p.getLabel(), p.getSortOrder());
    }
}
