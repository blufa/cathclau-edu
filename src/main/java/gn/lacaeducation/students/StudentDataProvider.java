package gn.lacaeducation.students;
import java.util.List;
import java.util.Optional;
public interface StudentDataProvider {
    Optional<StudentPublicInfo> findStudent(Long studentId);
    List<ParentPublicInfo> findParentsByStudent(Long studentId);
    /** Students with a non-cancelled enrollment in the class, with their contact (phone/email). */
    List<StudentPublicInfo> findStudentsByClass(Long classId);
    /** La classe de l'inscription validée la plus récente de l'élève (sa classe courante), si elle existe. */
    Optional<Long> findCurrentClassId(Long studentId);
    /** Identifiant élève lié au compte utilisateur (portail élève), si {@code userId} en a un. */
    Optional<Long> findStudentIdByUserId(Long userId);
    /** Tous les parents du tenant (diffusion de masse « tous les parents »). */
    List<ParentPublicInfo> findAllParents(Long tenantId);
}
