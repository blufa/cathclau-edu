package gn.lacaeducation.platform.adapters.out.persistence;

import gn.lacaeducation.platform.adapters.out.persistence.mapper.PlatformMapper;
import gn.lacaeducation.platform.application.port.out.TenantRepository;
import gn.lacaeducation.platform.domain.Tenant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class TenantPersistenceAdapter implements TenantRepository {

    private final TenantJpaRepository jpaRepository;
    private final PlatformMapper mapper;

    @Override
    public Tenant save(Tenant tenant) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(tenant)));
    }

    @Override
    public Optional<Tenant> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Tenant> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public Optional<Tenant> findBySlug(String slug) {
        return jpaRepository.findBySlug(slug).map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return jpaRepository.existsBySlug(slug);
    }

    @Override
    public List<Tenant> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public List<Tenant> findByPrefectureIdIn(Collection<Long> prefectureIds) {
        if (prefectureIds == null || prefectureIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findByPrefectureIdIn(prefectureIds).stream().map(mapper::toDomain).toList();
    }
}
