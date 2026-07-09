package gn.lacaeducation.grading.adapters.out.persistence;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.grading.application.port.out.ExamSupervisionRepository;
import gn.lacaeducation.grading.domain.ExamSupervision;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class ExamSupervisionPersistenceAdapter implements ExamSupervisionRepository {

    private final ExamSupervisionJpaRepository repo;

    @Override
    public ExamSupervision save(ExamSupervision s) {
        ExamSupervisionJpaEntity e = s.getId() != null
                ? repo.findById(s.getId()).orElseGet(ExamSupervisionJpaEntity::new)
                : new ExamSupervisionJpaEntity();
        e.setTenantId(s.getTenantId() != null ? s.getTenantId() : TenantContext.get());
        e.setExamId(s.getExamId());
        e.setRoomId(s.getRoomId());
        e.setTeacherId(s.getTeacherId());
        return toDomain(repo.save(e));
    }

    @Override
    public List<ExamSupervision> findByExamId(Long examId) {
        return repo.findByExamId(examId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<ExamSupervision> findByTeacherId(Long teacherId) {
        return repo.findByTeacherId(teacherId).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsByExamIdAndTeacherId(Long examId, Long teacherId) {
        return repo.existsByExamIdAndTeacherId(examId, teacherId);
    }

    @Override
    public void deleteById(Long id) { repo.deleteById(id); }

    private ExamSupervision toDomain(ExamSupervisionJpaEntity e) {
        return ExamSupervision.builder()
                .id(e.getId()).tenantId(e.getTenantId()).examId(e.getExamId())
                .roomId(e.getRoomId()).teacherId(e.getTeacherId()).build();
    }
}
