package gn.lacaeducation.students.application.port.out;

import gn.lacaeducation.students.domain.Parent;
import gn.lacaeducation.students.domain.StudentParent;
import java.util.List;

public interface StudentParentRepository {
    StudentParent save(StudentParent studentParent);
    List<Parent> findParentsByStudentId(Long studentId);
    List<StudentParent> findLinksByStudentId(Long studentId);
    void unlinkStudentParent(Long id);
    void unlinkByStudentAndParent(Long studentId, Long parentId);
    List<Long> findStudentIdsByParentId(Long parentId);
    /** Tous les parentId ayant au moins un enfant lié (toutes écoles) — à croiser côté appelant avec
     * les parents de l'école courante (§7 : distinguer un parent lié d'un prospect sans enfant). */
    List<Long> findDistinctLinkedParentIds();
}
