package gn.lacaeducation.students.application.port.out;

import gn.lacaeducation.students.domain.EnrollmentStatus;
import gn.lacaeducation.students.domain.Gender;
import gn.lacaeducation.students.domain.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface StudentRepository {
    Student save(Student student);
    Optional<Student> findById(Long id);
    Optional<Student> findByUserId(Long userId);
    List<Student> findByTenantId(Long tenantId);
    Page<Student> search(Long tenantId, Gender gender, String q, Long classId, EnrollmentStatus status, boolean notEnrolled, Pageable pageable);
    void deleteById(Long id);
    boolean existsById(Long id);
    long countByTenantId(Long tenantId);
    boolean existsByTenantIdAndStudentNumber(Long tenantId, String studentNumber);
}
