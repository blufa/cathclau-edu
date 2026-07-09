package gn.lacaeducation.biometric.adapters.out.persistence;

import gn.lacaeducation.biometric.adapters.out.persistence.mapper.BiometricMapper;
import gn.lacaeducation.biometric.application.port.out.BiometricEnrollmentRepository;
import gn.lacaeducation.biometric.domain.BiometricEnrollment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class BiometricEnrollmentPersistenceAdapter implements BiometricEnrollmentRepository {
    private final BiometricEnrollmentJpaRepository jpa;
    private final BiometricMapper mapper;

    @Override public BiometricEnrollment save(BiometricEnrollment e) { return mapper.toDomain(jpa.save(mapper.toEntity(e))); }
    @Override public Optional<BiometricEnrollment> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<BiometricEnrollment> findByTenantId(Long t) { return jpa.findByTenantId(t).stream().map(mapper::toDomain).toList(); }
    @Override public List<BiometricEnrollment> findByDeviceId(Long d) { return jpa.findByDeviceId(d).stream().map(mapper::toDomain).toList(); }
    @Override public Optional<BiometricEnrollment> findByDeviceIdAndExternalUserId(Long d, String u) { return jpa.findByDeviceIdAndExternalUserId(d, u).map(mapper::toDomain); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
