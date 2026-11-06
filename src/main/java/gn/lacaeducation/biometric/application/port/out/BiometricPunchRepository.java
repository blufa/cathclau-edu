package gn.lacaeducation.biometric.application.port.out;

import gn.lacaeducation.biometric.domain.BiometricPunch;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BiometricPunchRepository {
    BiometricPunch save(BiometricPunch p);
    List<BiometricPunch> findByTenantId(Long tenantId);
    List<BiometricPunch> findByTenantIdAndDeviceId(Long tenantId, Long deviceId);
    List<BiometricPunch> findByTenantIdAndPunchTimeBetween(Long tenantId, LocalDateTime from, LocalDateTime to);
    Optional<BiometricPunch> findByDeviceIdAndSyncId(Long deviceId, String syncId);
}
