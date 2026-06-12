package gn.lacaeducation.staff.application.port.in;

import gn.lacaeducation.academics.ClassPublicInfo;
import gn.lacaeducation.academics.ClassSubjectPublicInfo;
import gn.lacaeducation.academics.ScheduleSlotPublicInfo;
import gn.lacaeducation.staff.domain.Teacher;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TeacherUseCase {
    record CreateTeacherCommand(Long tenantId, Long userId, String staffNumber,
                                String firstName, String lastName, String phone,
                                String email, String specialization, LocalDate hireDate) {}
    record UpdateTeacherCommand(String staffNumber, String firstName, String lastName,
                                String phone, String email, String specialization, LocalDate hireDate) {}

    Teacher create(CreateTeacherCommand command);
    Teacher update(Long id, UpdateTeacherCommand command);
    Teacher setActive(Long id, boolean active);
    Optional<Teacher> findById(Long id);
    List<Teacher> findByTenantId(Long tenantId);
    void delete(Long id);
    boolean existsById(Long id);

    // Consolidated views (read-through to academics)
    List<ClassPublicInfo> findClasses(Long teacherId);
    List<ClassSubjectPublicInfo> findSubjects(Long teacherId);
    List<ScheduleSlotPublicInfo> findSchedule(Long teacherId);
}
