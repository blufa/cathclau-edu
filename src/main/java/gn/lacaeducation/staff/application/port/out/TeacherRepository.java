package gn.lacaeducation.staff.application.port.out;

import gn.lacaeducation.staff.domain.Teacher;
import java.util.List;
import java.util.Optional;

public interface TeacherRepository {
    Teacher save(Teacher teacher);
    Optional<Teacher> findById(Long id);
    List<Teacher> findByTenantId(Long tenantId);
    void deleteById(Long id);
    boolean existsById(Long id);
    boolean existsByTenantIdAndStaffNumber(Long tenantId, String staffNumber);
}
