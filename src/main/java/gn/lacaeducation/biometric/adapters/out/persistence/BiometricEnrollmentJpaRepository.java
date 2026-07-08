package gn.lacaeducation.biometric.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface BiometricEnrollmentJpaRepository extends JpaRepository<BiometricEnrollmentJpaEntity, Long> {
    List<BiometricEnrollmentJpaEntity> findByTenantId(Long tenantId);
    List<BiometricEnrollmentJpaEntity> findByDeviceId(Long deviceId);
    Optional<BiometricEnrollmentJpaEntity> findByDeviceIdAndExternalUserId(Long deviceId, String externalUserId);
}
