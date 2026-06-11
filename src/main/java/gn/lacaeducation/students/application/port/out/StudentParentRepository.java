package gn.lacaeducation.students.application.port.out;

import gn.lacaeducation.students.domain.Parent;
import gn.lacaeducation.students.domain.StudentParent;
import java.util.List;

public interface StudentParentRepository {
    StudentParent save(StudentParent studentParent);
    List<Parent> findParentsByStudentId(Long studentId);
    void unlinkStudentParent(Long id);
    List<Long> findStudentIdsByParentId(Long parentId);
}
