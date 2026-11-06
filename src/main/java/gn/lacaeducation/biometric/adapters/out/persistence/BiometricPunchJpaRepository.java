package gn.lacaeducation.biometric.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

interface BiometricPunchJpaRepository extends JpaRepository<BiometricPunchJpaEntity, Long> {
    List<BiometricPunchJpaEntity> findByTenantIdOrderByPunchTimeDesc(Long tenantId);
    List<BiometricPunchJpaEntity> findByTenantIdAndDeviceIdOrderByPunchTimeDesc(Long tenantId, Long deviceId);
    List<BiometricPunchJpaEntity> findByTenantIdAndPunchTimeBetweenOrderByPunchTimeDesc(Long tenantId, LocalDateTime from, LocalDateTime to);
    Optional<BiometricPunchJpaEntity> findByDeviceIdAndSyncId(Long deviceId, String syncId);
}
