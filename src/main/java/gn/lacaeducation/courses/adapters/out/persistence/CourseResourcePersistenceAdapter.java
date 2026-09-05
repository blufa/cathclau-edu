package gn.lacaeducation.courses.adapters.out.persistence;

import gn.lacaeducation.courses.application.port.out.CourseResourceRepository;
import gn.lacaeducation.courses.domain.CourseResource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class CourseResourcePersistenceAdapter implements CourseResourceRepository {

    private final CourseResourceJpaRepository jpa;

    @Override
    public CourseResource save(CourseResource r) {
        return toDomain(jpa.save(toEntity(r)));
    }

    @Override
    public Optional<CourseResource> findById(Long id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<CourseResource> findByCourseIdOrderBySortOrderAsc(Long courseId) {
        return jpa.findByCourseIdOrderBySortOrderAsc(courseId).stream().map(this::toDomain).toList();
    }

    @Override
    public long countByCourseId(Long courseId) {
        return jpa.countByCourseId(courseId);
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    @Override
    public void deleteByCourseId(Long courseId) {
        jpa.deleteByCourseId(courseId);
    }

    private CourseResource toDomain(CourseResourceJpaEntity e) {
        return CourseResource.builder()
                .id(e.getId()).courseId(e.getCourseId()).type(e.getType()).title(e.getTitle())
                .fileUrl(e.getFileUrl()).sortOrder(e.getSortOrder()).durationSeconds(e.getDurationSeconds())
                .build();
    }

    private CourseResourceJpaEntity toEntity(CourseResource d) {
        CourseResourceJpaEntity e = new CourseResourceJpaEntity();
        e.setId(d.getId()); e.setCourseId(d.getCourseId()); e.setType(d.getType()); e.setTitle(d.getTitle());
        e.setFileUrl(d.getFileUrl()); e.setSortOrder(d.getSortOrder()); e.setDurationSeconds(d.getDurationSeconds());
        return e;
    }
}
