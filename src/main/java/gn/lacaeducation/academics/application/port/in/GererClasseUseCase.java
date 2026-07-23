package gn.lacaeducation.academics.application.port.in;
import gn.lacaeducation.academics.domain.Classe;
import gn.lacaeducation.academics.domain.ClasseMatiere;
import gn.lacaeducation.academics.domain.ClassStudent;
import gn.lacaeducation.academics.domain.valueobject.ClassType;
import java.util.List;
import java.util.Optional;
public interface GererClasseUseCase {
    Classe create(CreateClassCommand command);
    Classe update(Long id, CreateClassCommand command);
    Optional<Classe> findById(Long id);
    List<Classe> findByAcademicYear(Long academicYearId);
    List<Classe> findByTenantId(Long tenantId);
    ClasseMatiere assignSubject(Long classId, Long subjectId, Long teacherId, java.math.BigDecimal coefficient, String code);
    void removeSubject(Long classSubjectId);
    List<ClasseMatiere> findSubjects(Long classId);
    /** Affectations existantes d'une matière (tenant-wide) — pour lister les codes enseignants déjà
     * attribués (ex. CH1, CH2), toutes classes confondues. */
    List<ClasseMatiere> findTeacherCodesForSubject(Long subjectId);
    void delete(Long id);
    List<ClassStudent> findStudentsByClassId(Long classId);
    EnrollmentCountInfo getEnrollmentCount(Long classId);
    record CreateClassCommand(Long tenantId, Long levelId, Long academicYearId,
                               Long headTeacherId, Long roomId, String label, Integer maxCapacity,
                               ClassType classType) {}
    record EnrollmentCountInfo(Long classId, long currentCount, Integer maxCapacity, long available) {}
}
