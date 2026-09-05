package gn.lacaeducation.courses.application.port.in;

import gn.lacaeducation.courses.domain.Course;
import gn.lacaeducation.courses.domain.CourseResource;
import gn.lacaeducation.courses.domain.CourseResourceType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CourseUseCase {
    record CreateCourseCommand(Long tenantId, Long classId, String subjectLabel, String title,
                               String description, BigDecimal price, String thumbnailUrl) {}
    record UpdateCourseCommand(String subjectLabel, String title, String description,
                               BigDecimal price, String thumbnailUrl) {}
    record AddResourceCommand(CourseResourceType type, String title, String fileUrl,
                              Integer sortOrder, Integer durationSeconds) {}

    Course create(CreateCourseCommand command);
    Course update(Long id, UpdateCourseCommand command);
    Course publish(Long id);
    Course unpublish(Long id);
    Optional<Course> findById(Long id);
    List<Course> findByTenantId(Long tenantId);
    List<Course> findPublishedByTenantId(Long tenantId);
    List<Course> findByClassId(Long classId);
    void delete(Long id);

    CourseResource addResource(Long courseId, AddResourceCommand command);
    /** Toutes les ressources avec leur URL — réservé au personnel gestionnaire. */
    List<CourseResource> findResourcesByCourseId(Long courseId);
    void deleteResource(Long resourceId);
}
