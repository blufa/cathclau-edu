package gn.lacaeducation.biometric.adapters.out.persistence;

import gn.lacaeducation.biometric.domain.ConflictStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface BiometricPunchConflictJpaRepository extends JpaRepository<BiometricPunchConflictJpaEntity, Long> {
    List<BiometricPunchConflictJpaEntity> findByTenantIdOrderByDetectedAtDesc(Long tenantId);
    List<BiometricPunchConflictJpaEntity> findByTenantIdAndStatusOrderByDetectedAtDesc(Long tenantId, ConflictStatus status);
}
