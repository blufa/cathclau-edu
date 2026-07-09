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
}
