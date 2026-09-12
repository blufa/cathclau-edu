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
        // saveAndFlush (pas save) : sans ça, Hibernate exécute par défaut les INSERT avant les UPDATE
        // au flush de la transaction, quel que soit l'ordre d'appel dans le code — l'INSERT de la
        // nouvelle ligne active=true partait donc avant l'UPDATE désactivant l'ancienne, violant
        // l'index unique partiel idx_platform_fee_settings_one_active (deux lignes actives à la fois).
        // Le flush explicite force l'UPDATE à s'exécuter immédiatement, avant que setAmount() ne
        // déclenche l'INSERT suivant.
        jpa.findByActiveTrue().ifPresent(e -> {
            e.setActive(false);
            jpa.saveAndFlush(e);
        });
    }
}
