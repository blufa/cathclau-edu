package gn.lacaeducation.biometric.application.port.out;

import gn.lacaeducation.biometric.domain.BiometricEnrollment;

import java.util.List;
import java.util.Optional;

public interface BiometricEnrollmentRepository {
    BiometricEnrollment save(BiometricEnrollment e);
    Optional<BiometricEnrollment> findById(Long id);
    List<BiometricEnrollment> findByTenantId(Long tenantId);
    List<BiometricEnrollment> findByDeviceId(Long deviceId);
    Optional<BiometricEnrollment> findByDeviceIdAndExternalUserId(Long deviceId, String externalUserId);
    void deleteById(Long id);
}
