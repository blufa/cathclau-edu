package gn.lacaeducation.biometric.application.port.out;

import gn.lacaeducation.biometric.domain.BiometricDevice;

import java.util.List;
import java.util.Optional;

public interface BiometricDeviceRepository {
    BiometricDevice save(BiometricDevice d);
    Optional<BiometricDevice> findById(Long id);
    List<BiometricDevice> findByTenantId(Long tenantId);
    void deleteById(Long id);
}
