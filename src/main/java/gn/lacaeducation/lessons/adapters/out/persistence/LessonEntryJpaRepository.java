package gn.lacaeducation.lessons.adapters.out.persistence;

import gn.lacaeducation.lessons.domain.LessonStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

interface LessonEntryJpaRepository extends JpaRepository<LessonEntryJpaEntity, Long> {
    List<LessonEntryJpaEntity> findByTenantId(Long tenantId);
    List<LessonEntryJpaEntity> findByTenantIdAndClassId(Long tenantId, Long classId);
    List<LessonEntryJpaEntity> findByTenantIdAndClassIdAndSubjectId(Long tenantId, Long classId, Long subjectId);
    List<LessonEntryJpaEntity> findByTeacherIdAndStatusAndLessonDateBetween(Long teacherId, LessonStatus status, LocalDate from, LocalDate to);
    boolean existsByTenantIdAndSourceAssessmentId(Long tenantId, Long sourceAssessmentId);
}
