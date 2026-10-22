package gn.lacaeducation.academics.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

interface PrimaryTimetableSettingsJpaRepository extends JpaRepository<PrimaryTimetableSettingsJpaEntity, Long> {
    Optional<PrimaryTimetableSettingsJpaEntity> findByTenantId(Long tenantId);
}
