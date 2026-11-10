package gn.lacaeducation.biometric.adapters.out.persistence;

import gn.lacaeducation.biometric.adapters.out.persistence.mapper.BiometricMapper;
import gn.lacaeducation.biometric.application.port.out.BiometricPunchConflictRepository;
import gn.lacaeducation.biometric.domain.BiometricPunchConflict;
import gn.lacaeducation.biometric.domain.ConflictStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class BiometricPunchConflictPersistenceAdapter implements BiometricPunchConflictRepository {
    private final BiometricPunchConflictJpaRepository jpa;
    private final BiometricMapper mapper;

    @Override public BiometricPunchConflict save(BiometricPunchConflict c) { return mapper.toDomain(jpa.save(mapper.toEntity(c))); }
    @Override public Optional<BiometricPunchConflict> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<BiometricPunchConflict> findByTenantId(Long t) { return jpa.findByTenantIdOrderByDetectedAtDesc(t).stream().map(mapper::toDomain).toList(); }
    @Override public List<BiometricPunchConflict> findByTenantIdAndStatus(Long t, ConflictStatus s) { return jpa.findByTenantIdAndStatusOrderByDetectedAtDesc(t, s).stream().map(mapper::toDomain).toList(); }
}
