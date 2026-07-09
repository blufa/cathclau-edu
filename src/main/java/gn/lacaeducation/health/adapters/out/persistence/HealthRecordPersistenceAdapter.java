package gn.lacaeducation.health.adapters.out.persistence;

import gn.lacaeducation.health.adapters.out.persistence.mapper.HealthMapper;
import gn.lacaeducation.health.application.port.out.HealthRecordRepository;
import gn.lacaeducation.health.domain.HealthRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class HealthRecordPersistenceAdapter implements HealthRecordRepository {
    private final HealthRecordJpaRepository jpa;
    private final HealthMapper mapper;

    @Override public HealthRecord save(HealthRecord r) { return mapper.toDomain(jpa.save(mapper.toEntity(r))); }
    @Override public Optional<HealthRecord> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public Optional<HealthRecord> findByStudentId(Long sid) { return jpa.findByStudentId(sid).map(mapper::toDomain); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
