package gn.lacaeducation.payroll.application.port.out;

import gn.lacaeducation.payroll.domain.TeacherCompensation;

import java.util.List;
import java.util.Optional;

public interface TeacherCompensationRepository {
    TeacherCompensation save(TeacherCompensation c);
    Optional<TeacherCompensation> findByTeacherId(Long teacherId);
    List<TeacherCompensation> findByTenantId(Long tenantId);
}
