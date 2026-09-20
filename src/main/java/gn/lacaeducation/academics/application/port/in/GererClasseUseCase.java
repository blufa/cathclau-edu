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
    /** {@code specialty} : {@code null} = ne pas modifier la valeur existante à la mise à jour (false à
     * la création) ; sinon impose explicitement la valeur (ex. décocher « matière de spécialité »). */
    ClasseMatiere assignSubject(Long classId, Long subjectId, Long teacherId, java.math.BigDecimal coefficient,
                                String code, Boolean specialty);
    void removeSubject(Long classSubjectId);
    List<ClasseMatiere> findSubjects(Long classId);
    /** Affectations existantes d'une matière (tenant-wide) — pour lister les codes enseignants déjà
     * attribués (ex. CH1, CH2), toutes classes confondues. */
    List<ClasseMatiere> findTeacherCodesForSubject(Long subjectId);
    void delete(Long id);
    List<ClassStudent> findStudentsByClassId(Long classId);
    EnrollmentCountInfo getEnrollmentCount(Long classId);
    /** Désigne (ou retire, avec studentId=null) le chef de classe — doit être un élève inscrit
     * (inscription validée) dans cette classe. Séparé de {@link #update} pour ne jamais être écrasé
     * par inadvertance par le formulaire général d'édition de classe. */
    Classe setClassRepresentative(Long classId, Long studentId);
    record CreateClassCommand(Long tenantId, Long levelId, Long academicYearId,
                               Long headTeacherId, Long roomId, String label, Integer maxCapacity,
                               ClassType classType) {}
    /** {@code available} est {@code null} quand la classe n'a pas de capacité maximale définie (pas de
     * limite), plutôt qu'un entier sentinelle (ex. {@code Long.MAX_VALUE}) qui s'afficherait tel quel,
     * de façon absurde, côté frontend. */
    record EnrollmentCountInfo(Long classId, long currentCount, Integer maxCapacity, Long available) {}
}
