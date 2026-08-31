package gn.lacaeducation.platform.application.port.out;

import gn.lacaeducation.platform.domain.PlatformFeeSetting;

import java.util.Optional;

public interface PlatformFeeSettingRepository {
    PlatformFeeSetting save(PlatformFeeSetting setting);
    Optional<PlatformFeeSetting> findActive();
    /** Désactive la ligne active courante (si présente), avant l'insertion d'une nouvelle ligne active. */
    void deactivateActive();
}
