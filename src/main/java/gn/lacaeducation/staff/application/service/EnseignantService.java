package gn.lacaeducation.staff.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.academics.ClassPublicInfo;
import gn.lacaeducation.academics.ClassSubjectPublicInfo;
import gn.lacaeducation.academics.ScheduleSlotPublicInfo;
import gn.lacaeducation.common.ConflictException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.common.SchoolCode;
import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.grading.GradingTeacherProvider;
import gn.lacaeducation.staff.application.event.StaffCreatedEvent;
import gn.lacaeducation.staff.application.port.in.TeacherUseCase;
import gn.lacaeducation.staff.application.port.out.TeacherRepository;
import gn.lacaeducation.staff.domain.Teacher;
import gn.lacaeducation.staff.domain.TeacherWorkload;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;
import java.util.Optional;

@Service @Transactional @RequiredArgsConstructor
public class EnseignantService implements TeacherUseCase {

    private final TeacherRepository teacherRepository;
    private final GradingTeacherProvider gradingTeacherProvider;
    private final AcademicsDataProvider academicsDataProvider;
    private final ApplicationEventPublisher eventPublisher;
    private final TenantInfoProvider tenantInfoProvider;

    @Override
    public Teacher create(CreateTeacherCommand cmd) {
        String matricule = resolveStaffNumber(cmd.tenantId(), cmd.staffNumber());
        Teacher saved = teacherRepository.save(Teacher.builder()
                .tenantId(cmd.tenantId()).userId(cmd.userId()).staffNumber(matricule)
                .firstName(cmd.firstName()).lastName(cmd.lastName()).phone(cmd.phone())
                .email(cmd.email()).specialization(cmd.specialization()).hireDate(cmd.hireDate())
                .employmentStatus(cmd.employmentStatus()).diploma(cmd.diploma())
                .remunerationType(cmd.remunerationType()).remunerationRate(cmd.remunerationRate())
                .teachingAuthorizationNumber(cmd.teachingAuthorizationNumber()).cnssNumber(cmd.cnssNumber())
                .active(true).build());
        eventPublisher.publishEvent(new StaffCreatedEvent(saved.getTenantId(), saved.getId(), saved.getStaffNumber()));
        return saved;
    }

    /** Returns a tenant-unique staff number: validates a provided one, or auto-generates {@code EMP-YEAR-SEQ}. */
    private String resolveStaffNumber(Long tenantId, String provided) {
        if (provided != null && !provided.isBlank()) {
            if (teacherRepository.existsByTenantIdAndStaffNumber(tenantId, provided)) {
                throw new ConflictException("Staff number already exists for this tenant: " + provided);
            }
            return provided;
        }
        String prefix = SchoolCode.prefix(tenantInfoProvider, tenantId);
        String year = String.valueOf(Year.now().getValue());
        long seq = teacherRepository.findByTenantId(tenantId).size() + 1L;
        String candidate;
        do {
            candidate = String.format("%s-ENS-%s-%04d", prefix, year, seq++);
        } while (teacherRepository.existsByTenantIdAndStaffNumber(tenantId, candidate));
        return candidate;
    }

    @Override
    public Teacher update(Long id, UpdateTeacherCommand cmd) {
        Teacher t = load(id);
        t.setStaffNumber(cmd.staffNumber()); t.setFirstName(cmd.firstName()); t.setLastName(cmd.lastName());
        t.setPhone(cmd.phone()); t.setEmail(cmd.email()); t.setSpecialization(cmd.specialization()); t.setHireDate(cmd.hireDate());
        t.setEmploymentStatus(cmd.employmentStatus()); t.setDiploma(cmd.diploma());
        t.setRemunerationType(cmd.remunerationType()); t.setRemunerationRate(cmd.remunerationRate());
        t.setTeachingAuthorizationNumber(cmd.teachingAuthorizationNumber()); t.setCnssNumber(cmd.cnssNumber());
        return teacherRepository.save(t);
    }

    @Override
    public Teacher setActive(Long id, boolean active) {
        Teacher t = load(id);
        t.setActive(active);
        return teacherRepository.save(t);
    }

    @Override @Transactional(readOnly = true)
    public Optional<Teacher> findById(Long id) { return teacherRepository.findById(id); }

    @Override @Transactional(readOnly = true)
    public Optional<Teacher> findByUserId(Long userId) { return userId == null ? Optional.empty() : teacherRepository.findByUserId(userId); }

    @Override @Transactional(readOnly = true)
    public List<Teacher> findByTenantId(Long tenantId) { return teacherRepository.findByTenantId(tenantId); }

    @Override
    public void delete(Long id) {
        Teacher t = load(id);
        // Deletion is forbidden when the teacher owns evaluations/grades: deactivate instead.
        if (gradingTeacherProvider.teacherHasEvaluations(id)) {
            throw new ConflictException(
                    "Teacher " + id + " is linked to evaluations and cannot be deleted; deactivate instead.");
        }
        teacherRepository.deleteById(t.getId());
    }

    @Override @Transactional(readOnly = true)
    public boolean existsById(Long id) { return teacherRepository.existsById(id); }

    @Override @Transactional(readOnly = true)
    public List<ClassPublicInfo> findClasses(Long teacherId) {
        return academicsDataProvider.findClassesByHeadTeacher(teacherId);
    }

    @Override @Transactional(readOnly = true)
    public List<ClassSubjectPublicInfo> findSubjects(Long teacherId) {
        return academicsDataProvider.findClassSubjectsByTeacher(teacherId);
    }

    @Override @Transactional(readOnly = true)
    public List<ScheduleSlotPublicInfo> findSchedule(Long teacherId) {
        return academicsDataProvider.findScheduleByTeacher(teacherId);
    }

    @Override @Transactional(readOnly = true)
    public TeacherWorkload computeWorkload(Long teacherId) {
        List<TeacherWorkload.Slot> slots = academicsDataProvider.findScheduleByTeacher(teacherId).stream()
                .map(s -> new TeacherWorkload.Slot(s.day(), parseTime(s.startTime()), parseTime(s.endTime())))
                .toList();
        return TeacherWorkload.from(teacherId, slots);
    }

    /** Parse une heure ISO ("HH:mm" / "HH:mm:ss"), ou {@code null} si absente/illisible. */
    private static java.time.LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return java.time.LocalTime.parse(value);
        } catch (java.time.format.DateTimeParseException e) {
            return null;
        }
    }

    private Teacher load(Long id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", id));
    }
}
