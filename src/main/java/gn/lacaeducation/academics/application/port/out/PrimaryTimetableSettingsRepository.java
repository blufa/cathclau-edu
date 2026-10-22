package gn.lacaeducation.academics.application.port.out;

import gn.lacaeducation.academics.domain.PrimaryTimetableSettings;

import java.util.Optional;

public interface PrimaryTimetableSettingsRepository {

    Optional<PrimaryTimetableSettings> findByTenantId(Long tenantId);

    PrimaryTimetableSettings save(PrimaryTimetableSettings settings);
}
