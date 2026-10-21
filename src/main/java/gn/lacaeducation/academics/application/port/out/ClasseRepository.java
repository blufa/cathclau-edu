package gn.lacaeducation.academics.application.port.out;
import gn.lacaeducation.academics.domain.Classe;
import gn.lacaeducation.academics.domain.ClasseMatiere;
import gn.lacaeducation.academics.domain.ClassStudent;
import java.util.List;
import java.util.Optional;
public interface ClasseRepository {
    Classe save(Classe schoolClass);
    Optional<Classe> findById(Long id);
    /** Comme {@link #findById}, mais pose un verrou pessimiste (SELECT ... FOR UPDATE) sur la ligne,
     * dans la transaction appelante — voir {@code ClassCapacityPort#lockAndGetMaxCapacity}. */
    Optional<Classe> lockById(Long id);
    List<Classe> findByAcademicYearId(Long academicYearId);
    List<Classe> findByTenantId(Long tenantId);
    void deleteById(Long id);
    ClasseMatiere saveClassSubject(ClasseMatiere cm);
    void deleteClassSubjectById(Long id);
    List<ClasseMatiere> findSubjectsByClassId(Long classId);
    /** Toutes les affectations existantes d'une matière (tenant-wide) — sert à lister les codes
     * enseignants déjà attribués pour cette matière (ex. CH1, CH2), toutes classes confondues. */
    List<ClasseMatiere> findSubjectsBySubjectId(Long subjectId);
    List<ClassStudent> findStudentsByClassId(Long classId);
}
