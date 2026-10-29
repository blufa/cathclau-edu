package gn.lacaeducation.academics.adapters.out.persistence;

import gn.lacaeducation.academics.application.port.out.EnrollmentQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("academicsEnrollmentQueryAdapter")
@RequiredArgsConstructor
    class EnrollmentQueryAdapter implements EnrollmentQueryPort {

    private final AcademicsEnrollmentJpaRepository enrollmentRepo;

    @Override
    public long countValidatedByClassId(Long classId) {
        return enrollmentRepo.countValidatedByClassId(classId);
    }

    @Override
    public long countActiveByClassId(Long classId) {
        return enrollmentRepo.countActiveByClassId(classId);
    }
}
