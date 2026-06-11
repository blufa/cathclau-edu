package gn.lacaeducation.academics.application.port.out;
import gn.lacaeducation.academics.domain.Classe;
import gn.lacaeducation.academics.domain.ClasseMatiere;
import gn.lacaeducation.academics.domain.ClassStudent;
import java.util.List;
import java.util.Optional;
public interface ClasseRepository {
    Classe save(Classe schoolClass);
    Optional<Classe> findById(Long id);
    List<Classe> findByAcademicYearId(Long academicYearId);
    List<Classe> findByTenantId(Long tenantId);
    void deleteById(Long id);
    ClasseMatiere saveClassSubject(ClasseMatiere cm);
    void deleteClassSubjectById(Long id);
    List<ClasseMatiere> findSubjectsByClassId(Long classId);
    List<ClassStudent> findStudentsByClassId(Long classId);
}
