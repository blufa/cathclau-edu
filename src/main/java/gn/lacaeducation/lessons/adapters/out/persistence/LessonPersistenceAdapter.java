package gn.lacaeducation.lessons.adapters.out.persistence;

import gn.lacaeducation.lessons.adapters.out.persistence.mapper.LessonMapper;
import gn.lacaeducation.lessons.application.port.out.LessonRepository;
import gn.lacaeducation.lessons.domain.LessonEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class LessonPersistenceAdapter implements LessonRepository {
    private final LessonEntryJpaRepository jpa;
    private final LessonMapper mapper;

    @Override public LessonEntry save(LessonEntry l) { return mapper.toDomain(jpa.save(mapper.toEntity(l))); }
    @Override public Optional<LessonEntry> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<LessonEntry> findByTenantId(Long t) { return jpa.findByTenantId(t).stream().map(mapper::toDomain).toList(); }
    @Override public List<LessonEntry> findByTenantIdAndClassId(Long t, Long c) { return jpa.findByTenantIdAndClassId(t, c).stream().map(mapper::toDomain).toList(); }
    @Override public List<LessonEntry> findByTenantIdAndClassIdAndSubjectId(Long t, Long c, Long s) { return jpa.findByTenantIdAndClassIdAndSubjectId(t, c, s).stream().map(mapper::toDomain).toList(); }
    @Override public boolean existsByTenantIdAndSourceAssessmentId(Long t, Long a) { return jpa.existsByTenantIdAndSourceAssessmentId(t, a); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
