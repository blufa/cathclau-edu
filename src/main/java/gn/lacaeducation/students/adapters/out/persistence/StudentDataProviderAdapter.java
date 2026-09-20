package gn.lacaeducation.students.adapters.out.persistence;
import gn.lacaeducation.students.AbandonedStudentInfo;
import gn.lacaeducation.students.StudentDataProvider;
import gn.lacaeducation.students.ParentPublicInfo;
import gn.lacaeducation.students.StudentPublicInfo;
import gn.lacaeducation.students.application.port.out.EnrollmentRepository;
import gn.lacaeducation.students.domain.EnrollmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
public class StudentDataProviderAdapter implements StudentDataProvider {
    private final EleveJpaRepository eleveRepo;
    private final ParentJpaRepository parentRepo;
    private final EleveParentJpaRepository eleveParentRepo;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentsMapper mapper;

    @Override
    public Optional<StudentPublicInfo> findStudent(Long studentId) {
        return eleveRepo.findById(studentId).map(e ->
                new StudentPublicInfo(e.getId(), e.getTenantId(), e.getFirstName(), e.getLastName(), e.getEmail(), e.getPhone(),
                        e.getStudentNumber(), e.getBirthDate(), e.getBirthPlace(),
                        e.getGender() != null ? e.getGender().name() : null));
    }

    @Override
    public List<ParentPublicInfo> findParentsByStudent(Long studentId) {
        List<EleveParentJpaEntity> links = eleveParentRepo.findByStudentId(studentId);
        // Destinataire par défaut des factures / relances / notifications : le responsable principal
        // (« responsable 1 ») s'il est désigné ; sinon, à défaut, tous les parents liés.
        List<EleveParentJpaEntity> primary = links.stream()
                .filter(EleveParentJpaEntity::isPrimaryGuardian).toList();
        return (primary.isEmpty() ? links : primary).stream()
                .map(ep -> parentRepo.findById(ep.getParentId()).orElse(null))
                .filter(p -> p != null)
                .map(p -> new ParentPublicInfo(p.getId(), p.getFirstName(), p.getLastName(), p.getEmail(), p.getPhone()))
                .toList();
    }

    @Override
    public List<StudentPublicInfo> findStudentsByClass(Long classId) {
        // ABANDONED (élève parti en cours d'année) est exclu comme CANCELLED : ces listes alimentent
        // l'appel/pointage (attendance) et la saisie de notes (grading), qui ne concernent que les
        // élèves encore présents — contrairement à la facturation/au recouvrement, qui doivent au
        // contraire continuer à compter les factures d'un abandon (voir EnrollmentQueryPort côté
        // billing, non touché par ce filtre).
        return enrollmentRepository.findByClassId(classId).stream()
                .filter(e -> e.getStatus() != EnrollmentStatus.CANCELLED && e.getStatus() != EnrollmentStatus.ABANDONED)
                .map(e -> findStudent(e.getStudentId()).orElse(null))
                .filter(s -> s != null)
                .distinct()
                .toList();
    }

    @Override
    public Optional<Long> findCurrentClassId(Long studentId) {
        return currentEnrollment(studentId).map(gn.lacaeducation.students.domain.Enrollment::getClassId);
    }

    @Override
    public boolean hasPlatformAccessOptIn(Long studentId) {
        return currentEnrollment(studentId)
                .map(e -> Boolean.TRUE.equals(e.getPlatformAccessOptIn()))
                .orElse(false);
    }

    /** L'inscription validée la plus récente de l'élève (par année scolaire puis par id). */
    private Optional<gn.lacaeducation.students.domain.Enrollment> currentEnrollment(Long studentId) {
        return enrollmentRepository.findByStudentId(studentId).stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.VALIDATED)
                .max(java.util.Comparator
                        .comparing(gn.lacaeducation.students.domain.Enrollment::getAcademicYearId,
                                java.util.Comparator.nullsFirst(Long::compareTo))
                        .thenComparing(gn.lacaeducation.students.domain.Enrollment::getId,
                                java.util.Comparator.nullsFirst(Long::compareTo)));
    }

    @Override
    public Optional<Long> findStudentIdByUserId(Long userId) {
        return eleveRepo.findByUserId(userId).map(EleveJpaEntity::getId);
    }

    @Override
    public List<ParentPublicInfo> findAllParents(Long tenantId) {
        return parentRepo.findByTenantId(tenantId).stream()
                .map(p -> new ParentPublicInfo(p.getId(), p.getFirstName(), p.getLastName(), p.getEmail(), p.getPhone()))
                .toList();
    }

    @Override
    public List<ParentPublicInfo> findAllParentsWithPlatformAccess(Long tenantId) {
        return parentRepo.findByTenantId(tenantId).stream()
                .filter(p -> eleveParentRepo.findByParentId(p.getId()).stream()
                        .anyMatch(link -> hasPlatformAccessOptIn(link.getStudentId())))
                .map(p -> new ParentPublicInfo(p.getId(), p.getFirstName(), p.getLastName(), p.getEmail(), p.getPhone()))
                .toList();
    }

    @Override
    public List<AbandonedStudentInfo> findAbandonedStudents(Long academicYearId) {
        return enrollmentRepository.findByAcademicYearId(academicYearId).stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ABANDONED)
                .map(e -> findStudent(e.getStudentId())
                        .map(s -> new AbandonedStudentInfo(s.id(),
                                ((s.firstName() != null ? s.firstName() : "") + " "
                                        + (s.lastName() != null ? s.lastName() : "")).trim(),
                                e.getClassId()))
                        .orElse(null))
                .filter(s -> s != null)
                .toList();
    }
}
