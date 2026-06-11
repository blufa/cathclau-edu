package gn.lacaeducation.academics.application.port.in;
import gn.lacaeducation.academics.domain.Classe;
import gn.lacaeducation.academics.domain.ClasseMatiere;
import gn.lacaeducation.academics.domain.ClassStudent;
import java.util.List;
import java.util.Optional;
public interface GererClasseUseCase {
    Classe create(CreateClassCommand command);
    Classe update(Long id, CreateClassCommand command);
    Optional<Classe> findById(Long id);
    List<Classe> findByAcademicYear(Long academicYearId);
    List<Classe> findByTenantId(Long tenantId);
    ClasseMatiere assignSubject(Long classId, Long subjectId, Long teacherId);
    void removeSubject(Long classSubjectId);
    List<ClasseMatiere> findSubjects(Long classId);
    void delete(Long id);
    List<ClassStudent> findStudentsByClassId(Long classId);
    EnrollmentCountInfo getEnrollmentCount(Long classId);
    record CreateClassCommand(Long tenantId, Long levelId, Long academicYearId,
                               Long headTeacherId, Long roomId, String label, Integer maxCapacity) {}
    record EnrollmentCountInfo(Long classId, long currentCount, Integer maxCapacity, long available) {}
}
