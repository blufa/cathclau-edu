package gn.lacaeducation.attendance.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

interface AttendanceSettingsJpaRepository extends JpaRepository<AttendanceSettingsJpaEntity, Long> {
    Optional<AttendanceSettingsJpaEntity> findByTenantId(Long tenantId);
}
