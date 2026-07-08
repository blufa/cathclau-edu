package gn.lacaeducation.biometric.adapters.out.persistence;

import gn.lacaeducation.biometric.adapters.out.persistence.mapper.BiometricMapper;
import gn.lacaeducation.biometric.application.port.out.BiometricDeviceRepository;
import gn.lacaeducation.biometric.domain.BiometricDevice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class BiometricDevicePersistenceAdapter implements BiometricDeviceRepository {
    private final BiometricDeviceJpaRepository jpa;
    private final BiometricMapper mapper;

    @Override public BiometricDevice save(BiometricDevice d) { return mapper.toDomain(jpa.save(mapper.toEntity(d))); }
    @Override public Optional<BiometricDevice> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<BiometricDevice> findByTenantId(Long t) { return jpa.findByTenantId(t).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
