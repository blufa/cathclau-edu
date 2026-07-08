package gn.lacaeducation.biometric.adapters.out.persistence;

import gn.lacaeducation.biometric.adapters.out.persistence.mapper.BiometricMapper;
import gn.lacaeducation.biometric.application.port.out.BiometricPunchRepository;
import gn.lacaeducation.biometric.domain.BiometricPunch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
class BiometricPunchPersistenceAdapter implements BiometricPunchRepository {
    private final BiometricPunchJpaRepository jpa;
    private final BiometricMapper mapper;

    @Override public BiometricPunch save(BiometricPunch p) { return mapper.toDomain(jpa.save(mapper.toEntity(p))); }
    @Override public List<BiometricPunch> findByTenantId(Long t) { return jpa.findByTenantIdOrderByPunchTimeDesc(t).stream().map(mapper::toDomain).toList(); }
    @Override public List<BiometricPunch> findByTenantIdAndDeviceId(Long t, Long d) { return jpa.findByTenantIdAndDeviceIdOrderByPunchTimeDesc(t, d).stream().map(mapper::toDomain).toList(); }
    @Override public List<BiometricPunch> findByTenantIdAndPunchTimeBetween(Long t, LocalDateTime f, LocalDateTime to) { return jpa.findByTenantIdAndPunchTimeBetweenOrderByPunchTimeDesc(t, f, to).stream().map(mapper::toDomain).toList(); }
}
