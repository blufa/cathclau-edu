package gn.lacaeducation.courses.application.port.out;

import gn.lacaeducation.courses.domain.CourseResource;

import java.util.List;
import java.util.Optional;

public interface CourseResourceRepository {
    CourseResource save(CourseResource resource);
    Optional<CourseResource> findById(Long id);
    List<CourseResource> findByCourseIdOrderBySortOrderAsc(Long courseId);
    long countByCourseId(Long courseId);
    void deleteById(Long id);
    void deleteByCourseId(Long courseId);
}
