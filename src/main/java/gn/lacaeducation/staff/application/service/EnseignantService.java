package gn.lacaeducation.staff.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.academics.ClassPublicInfo;
import gn.lacaeducation.academics.ClassSubjectPublicInfo;
import gn.lacaeducation.academics.ScheduleSlotPublicInfo;
import gn.lacaeducation.common.ConflictException;
import gn.lacaeducation.common.PersonName;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.grading.GradingTeacherProvider;
import gn.lacaeducation.staff.application.event.StaffCreatedEvent;
import gn.lacaeducation.staff.application.port.in.TeacherUseCase;
import gn.lacaeducation.staff.application.port.out.TeacherRepository;
import gn.lacaeducation.staff.domain.Teacher;
import gn.lacaeducation.staff.domain.TeacherMonthlyWorkload;
import gn.lacaeducation.staff.domain.TeacherWorkload;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        // Rejoué après une coupure réseau (mode hors-ligne) avec la même clé : renvoie l'enseignant déjà
        // créé au lieu d'en créer un second — même pattern que l'idempotence des élèves/paiements.
        if (cmd.idempotencyKey() != null && !cmd.idempotencyKey().isBlank()) {
            Optional<Teacher> existing = teacherRepository.findByIdempotencyKey(cmd.idempotencyKey());
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        String matricule = resolveStaffNumber(cmd.tenantId(), cmd.staffNumber());
        Teacher saved = teacherRepository.save(Teacher.builder()
                .tenantId(cmd.tenantId()).userId(cmd.userId()).staffNumber(matricule)
                .firstName(PersonName.capitalizeFirstName(cmd.firstName()))
                .lastName(PersonName.upperCaseLastName(cmd.lastName())).phone(cmd.phone())
                .email(cmd.email()).specialization(cmd.specialization()).hireDate(cmd.hireDate())
                .employmentStatus(cmd.employmentStatus()).diploma(cmd.diploma())
                .remunerationType(cmd.remunerationType()).remunerationRate(cmd.remunerationRate())
                .teachingAuthorizationNumber(cmd.teachingAuthorizationNumber()).cnssNumber(cmd.cnssNumber())
                .bankAccount(cmd.bankAccount())
                .idempotencyKey(cmd.idempotencyKey())
                .active(true).build());
        eventPublisher.publishEvent(new StaffCreatedEvent(saved.getTenantId(), saved.getId(), saved.getStaffNumber()));
        return saved;
    }

    /**
     * Returns a tenant-unique staff number: validates a provided one, or auto-generates one using the
     * school's own format (settings, defaults to {@code SIGLESEQ}, e.g. {@code AT00001}, same as the
     * student matricule) — see {@link gn.lacaeducation.common.MatriculeFormatter}.
     */
    private String resolveStaffNumber(Long tenantId, String provided) {
        if (provided != null && !provided.isBlank()) {
            if (teacherRepository.existsByTenantIdAndStaffNumber(tenantId, provided)) {
                throw new ConflictException("Staff number already exists for this tenant: " + provided);
            }
            return provided;
        }
        long seq = teacherRepository.findByTenantId(tenantId).size() + 1L;
        return gn.lacaeducation.common.MatriculeAllocator.resolveUnique(tenantInfoProvider, tenantId, seq,
                c -> teacherRepository.existsByTenantIdAndStaffNumber(tenantId, c));
    }

    @Override
    public Teacher update(Long id, UpdateTeacherCommand cmd) {
        Teacher t = load(id);
        t.setStaffNumber(cmd.staffNumber());
        t.setFirstName(PersonName.capitalizeFirstName(cmd.firstName()));
        t.setLastName(PersonName.upperCaseLastName(cmd.lastName()));
        t.setPhone(cmd.phone()); t.setEmail(cmd.email()); t.setSpecialization(cmd.specialization()); t.setHireDate(cmd.hireDate());
        t.setEmploymentStatus(cmd.employmentStatus()); t.setDiploma(cmd.diploma());
        t.setRemunerationType(cmd.remunerationType()); t.setRemunerationRate(cmd.remunerationRate());
        t.setTeachingAuthorizationNumber(cmd.teachingAuthorizationNumber()); t.setCnssNumber(cmd.cnssNumber());
        t.setBankAccount(cmd.bankAccount());
        // Ne lie un compte que s'il est fourni — ne jamais délier un compte déjà existant via une
        // simple mise à jour de profil (voir javadoc de UpdateTeacherCommand#userId).
        if (cmd.userId() != null) t.setUserId(cmd.userId());
        return teacherRepository.save(t);
    }

    @Override
    public Teacher setActive(Long id, boolean active) {
        Teacher t = load(id);
        t.setActive(active);
        return teacherRepository.save(t);
    }

    @Override
    public Teacher updateOwnProfile(Long userId, UpdateOwnProfileCommand cmd) {
        Teacher t = requireByUserId(userId);
        // Mise à jour partielle : seuls les champs fournis sont modifiés (rien d'administratif).
        if (cmd.phone() != null) t.setPhone(cmd.phone());
        if (cmd.email() != null) t.setEmail(cmd.email());
        return teacherRepository.save(t);
    }

    @Override
    public Teacher updateOwnPhoto(Long userId, String photoUrl) {
        Teacher t = requireByUserId(userId);
        t.setPhoto(photoUrl);
        return teacherRepository.save(t);
    }

    @Override
    public Teacher updatePhoto(Long id, String photoUrl) {
        Teacher t = load(id);
        t.setPhoto(photoUrl);
        return teacherRepository.save(t);
    }

    @Override
    public Teacher updateAllowsConcurrentClasses(Long id, boolean allowsConcurrentClasses) {
        Teacher t = load(id);
        t.setAllowsConcurrentClasses(allowsConcurrentClasses);
        return teacherRepository.save(t);
    }

    private Teacher requireByUserId(Long userId) {
        return teacherRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher (compte)", userId));
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
        return TeacherWorkload.from(teacherId, scheduleSlots(teacherId));
    }

    @Override @Transactional(readOnly = true)
    public TeacherMonthlyWorkload computeMonthlyWorkload(Long teacherId, int year, int month) {
        return TeacherMonthlyWorkload.from(teacherId, year, month, scheduleSlots(teacherId));
    }

    private List<TeacherWorkload.Slot> scheduleSlots(Long teacherId) {
        return academicsDataProvider.findScheduleByTeacher(teacherId).stream()
                .map(s -> new TeacherWorkload.Slot(s.day(), parseTime(s.startTime()), parseTime(s.endTime())))
                .toList();
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
