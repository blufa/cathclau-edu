package gn.lacaeducation.students.application.port.out;

import gn.lacaeducation.students.domain.Student;
import java.util.List;
import java.util.Optional;

public interface StudentRepository {
    Student save(Student student);
    Optional<Student> findById(Long id);
    List<Student> findByTenantId(Long tenantId);
    void deleteById(Long id);
    boolean existsById(Long id);
    long countByTenantId(Long tenantId);
}
