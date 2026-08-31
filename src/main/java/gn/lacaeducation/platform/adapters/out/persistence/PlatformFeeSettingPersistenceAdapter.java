package gn.lacaeducation.platform.adapters.out.persistence;

import gn.lacaeducation.platform.adapters.out.persistence.mapper.PlatformMapper;
import gn.lacaeducation.platform.application.port.out.PlatformFeeSettingRepository;
import gn.lacaeducation.platform.domain.PlatformFeeSetting;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class PlatformFeeSettingPersistenceAdapter implements PlatformFeeSettingRepository {

    private final PlatformFeeSettingJpaRepository jpa;
    private final PlatformMapper mapper;

    @Override
    public PlatformFeeSetting save(PlatformFeeSetting setting) {
        return mapper.toDomain(jpa.save(mapper.toEntity(setting)));
    }

    @Override
    public Optional<PlatformFeeSetting> findActive() {
        return jpa.findByActiveTrue().map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void deactivateActive() {
        jpa.findByActiveTrue().ifPresent(e -> {
            e.setActive(false);
            jpa.save(e);
        });
    }
}
