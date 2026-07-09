package gn.lacaeducation.lessons.application.port.out;

import gn.lacaeducation.lessons.domain.LessonEntry;

import java.util.List;
import java.util.Optional;

public interface LessonRepository {
    LessonEntry save(LessonEntry l);
    Optional<LessonEntry> findById(Long id);
    List<LessonEntry> findByTenantId(Long tenantId);
    List<LessonEntry> findByTenantIdAndClassId(Long tenantId, Long classId);
    List<LessonEntry> findByTenantIdAndClassIdAndSubjectId(Long tenantId, Long classId, Long subjectId);
    boolean existsByTenantIdAndSourceAssessmentId(Long tenantId, Long sourceAssessmentId);
    void deleteById(Long id);
}
