package gn.lacaeducation.courses.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface CourseResourceJpaRepository extends JpaRepository<CourseResourceJpaEntity, Long> {
    List<CourseResourceJpaEntity> findByCourseIdOrderBySortOrderAsc(Long courseId);
    long countByCourseId(Long courseId);
    void deleteByCourseId(Long courseId);
}
