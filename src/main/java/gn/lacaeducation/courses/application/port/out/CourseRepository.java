package gn.lacaeducation.courses.application.port.out;

import gn.lacaeducation.courses.domain.Course;

import java.util.List;
import java.util.Optional;

public interface CourseRepository {
    Course save(Course course);
    Optional<Course> findById(Long id);
    List<Course> findByTenantId(Long tenantId);
    List<Course> findByTenantIdAndPublishedTrue(Long tenantId);
    List<Course> findByClassId(Long classId);
    void deleteById(Long id);
}
