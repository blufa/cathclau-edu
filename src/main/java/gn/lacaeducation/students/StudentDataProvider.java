package gn.lacaeducation.students;
import java.util.List;
import java.util.Optional;
public interface StudentDataProvider {
    Optional<StudentPublicInfo> findStudent(Long studentId);
    List<ParentPublicInfo> findParentsByStudent(Long studentId);
}
