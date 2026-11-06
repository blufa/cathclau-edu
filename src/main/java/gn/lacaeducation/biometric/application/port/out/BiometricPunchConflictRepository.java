package gn.lacaeducation.biometric.application.port.out;

import gn.lacaeducation.biometric.domain.BiometricPunchConflict;
import gn.lacaeducation.biometric.domain.ConflictStatus;

import java.util.List;
import java.util.Optional;

public interface BiometricPunchConflictRepository {
    BiometricPunchConflict save(BiometricPunchConflict c);
    Optional<BiometricPunchConflict> findById(Long id);
    List<BiometricPunchConflict> findByTenantId(Long tenantId);
    List<BiometricPunchConflict> findByTenantIdAndStatus(Long tenantId, ConflictStatus status);
}
