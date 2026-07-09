package gn.lacaeducation.biometric.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface BiometricDeviceJpaRepository extends JpaRepository<BiometricDeviceJpaEntity, Long> {
    List<BiometricDeviceJpaEntity> findByTenantId(Long tenantId);
}
