package gn.lacaeducation.academics.adapters.out.persistence;

import gn.lacaeducation.academics.application.port.out.PrimaryTimetableSettingsRepository;
import gn.lacaeducation.academics.domain.PrimaryTimetableSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class PrimaryTimetableSettingsPersistenceAdapter implements PrimaryTimetableSettingsRepository {

    private final PrimaryTimetableSettingsJpaRepository repo;

    @Override
    public Optional<PrimaryTimetableSettings> findByTenantId(Long tenantId) {
        return repo.findByTenantId(tenantId).map(this::toDomain);
    }

    @Override
    public PrimaryTimetableSettings save(PrimaryTimetableSettings s) {
        PrimaryTimetableSettingsJpaEntity e = repo.findByTenantId(s.getTenantId())
                .orElseGet(PrimaryTimetableSettingsJpaEntity::new);
        e.setTenantId(s.getTenantId());
        e.setGridStartTime(s.getGridStartTimeOrDefault());
        e.setGridEndTime(s.getGridEndTimeOrDefault());
        e.setBandMinutes(s.getBandMinutesOrDefault());
        return toDomain(repo.save(e));
    }

    private PrimaryTimetableSettings toDomain(PrimaryTimetableSettingsJpaEntity e) {
        return PrimaryTimetableSettings.builder().id(e.getId()).tenantId(e.getTenantId())
                .gridStartTime(e.getGridStartTime()).gridEndTime(e.getGridEndTime())
                .bandMinutes(e.getBandMinutes()).build();
    }
}
