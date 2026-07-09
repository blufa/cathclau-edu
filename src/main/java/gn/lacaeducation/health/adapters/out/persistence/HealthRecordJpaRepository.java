package gn.lacaeducation.health.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface HealthRecordJpaRepository extends JpaRepository<HealthRecordJpaEntity, Long> {
    Optional<HealthRecordJpaEntity> findByStudentId(Long studentId);
}
