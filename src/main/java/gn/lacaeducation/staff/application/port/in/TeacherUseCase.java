package gn.lacaeducation.staff.application.port.in;

import gn.lacaeducation.academics.ClassPublicInfo;
import gn.lacaeducation.academics.ClassSubjectPublicInfo;
import gn.lacaeducation.academics.ScheduleSlotPublicInfo;
import gn.lacaeducation.staff.domain.Teacher;
import gn.lacaeducation.staff.domain.TeacherWorkload;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TeacherUseCase {
    record CreateTeacherCommand(Long tenantId, Long userId, String staffNumber,
                                String firstName, String lastName, String phone,
                                String email, String specialization, LocalDate hireDate,
                                String employmentStatus, String diploma, String remunerationType,
                                BigDecimal remunerationRate, String teachingAuthorizationNumber, String cnssNumber) {}
    record UpdateTeacherCommand(String staffNumber, String firstName, String lastName,
                                String phone, String email, String specialization, LocalDate hireDate,
                                String employmentStatus, String diploma, String remunerationType,
                                BigDecimal remunerationRate, String teachingAuthorizationNumber, String cnssNumber) {}

    Teacher create(CreateTeacherCommand command);
    Teacher update(Long id, UpdateTeacherCommand command);
    Teacher setActive(Long id, boolean active);
    Optional<Teacher> findById(Long id);
    Optional<Teacher> findByUserId(Long userId);
    List<Teacher> findByTenantId(Long tenantId);
    void delete(Long id);
    boolean existsById(Long id);

    // Consolidated views (read-through to academics)
    List<ClassPublicInfo> findClasses(Long teacherId);
    List<ClassSubjectPublicInfo> findSubjects(Long teacherId);
    List<ScheduleSlotPublicInfo> findSchedule(Long teacherId);

    /** Charge horaire hebdomadaire consolidée (somme des créneaux d'emploi du temps). */
    TeacherWorkload computeWorkload(Long teacherId);
}
