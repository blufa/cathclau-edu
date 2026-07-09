package gn.lacaeducation.health.application.port.out;

import gn.lacaeducation.health.domain.HealthRecord;

import java.util.Optional;

public interface HealthRecordRepository {
    HealthRecord save(HealthRecord record);
    Optional<HealthRecord> findById(Long id);
    Optional<HealthRecord> findByStudentId(Long studentId);
    void deleteById(Long id);
}
