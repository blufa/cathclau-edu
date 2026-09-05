package gn.lacaeducation.courses.adapters.out.persistence;

import gn.lacaeducation.courses.application.port.out.CourseRepository;
import gn.lacaeducation.courses.domain.Course;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class CoursePersistenceAdapter implements CourseRepository {

    private final CourseJpaRepository jpa;

    @Override
    public Course save(Course c) {
        return toDomain(jpa.save(toEntity(c)));
    }

    @Override
    public Optional<Course> findById(Long id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<Course> findByTenantId(Long tenantId) {
        return jpa.findByTenantIdOrderByCreatedAtDesc(tenantId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Course> findByTenantIdAndPublishedTrue(Long tenantId) {
        return jpa.findByTenantIdAndPublishedTrueOrderByCreatedAtDesc(tenantId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Course> findByClassId(Long classId) {
        return jpa.findByClassIdOrderByCreatedAtDesc(classId).stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    private Course toDomain(CourseJpaEntity e) {
        return Course.builder()
                .id(e.getId()).tenantId(e.getTenantId()).classId(e.getClassId())
                .subjectLabel(e.getSubjectLabel()).title(e.getTitle()).description(e.getDescription())
                .price(e.getPrice()).thumbnailUrl(e.getThumbnailUrl()).schoolFeeId(e.getSchoolFeeId())
                .published(e.isPublished()).createdAt(e.getCreatedAt())
                .build();
    }

    private CourseJpaEntity toEntity(Course d) {
        CourseJpaEntity e = new CourseJpaEntity();
        e.setId(d.getId()); e.setTenantId(d.getTenantId()); e.setClassId(d.getClassId());
        e.setSubjectLabel(d.getSubjectLabel()); e.setTitle(d.getTitle()); e.setDescription(d.getDescription());
        e.setPrice(d.getPrice()); e.setThumbnailUrl(d.getThumbnailUrl()); e.setSchoolFeeId(d.getSchoolFeeId());
        e.setPublished(d.isPublished()); e.setCreatedAt(d.getCreatedAt());
        return e;
    }
}
