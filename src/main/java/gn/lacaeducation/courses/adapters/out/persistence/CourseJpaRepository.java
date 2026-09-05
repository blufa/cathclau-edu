package gn.lacaeducation.courses.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface CourseJpaRepository extends JpaRepository<CourseJpaEntity, Long> {
    List<CourseJpaEntity> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
    List<CourseJpaEntity> findByTenantIdAndPublishedTrueOrderByCreatedAtDesc(Long tenantId);
    List<CourseJpaEntity> findByClassIdOrderByCreatedAtDesc(Long classId);
}
