package gn.lacaeducation.grading.adapters.out.persistence;

import gn.lacaeducation.grading.application.port.out.GradingCalculationSettingsRepository;
import gn.lacaeducation.grading.domain.GradingCalculationSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class GradingCalculationSettingsPersistenceAdapter implements GradingCalculationSettingsRepository {

    private final GradingCalculationSettingsJpaRepository repo;

    @Override
    public Optional<GradingCalculationSettings> findByTenantId(Long tenantId) {
        return repo.findByTenantId(tenantId).map(this::toDomain);
    }

    @Override
    public GradingCalculationSettings save(GradingCalculationSettings s) {
        GradingCalculationSettingsJpaEntity e = repo.findByTenantId(s.getTenantId())
                .orElseGet(GradingCalculationSettingsJpaEntity::new);
        e.setTenantId(s.getTenantId());
        e.setAverageMethod(s.methodOrDefault());
        e.setDecimals(s.decimalsOrDefault());
        e.setRounding(s.roundingOrDefault());
        e.setGeneralCoursWeight(s.getGeneralCoursWeight());
        e.setGeneralCompoWeight(s.getGeneralCompoWeight());
        return toDomain(repo.save(e));
    }

    private GradingCalculationSettings toDomain(GradingCalculationSettingsJpaEntity e) {
        return GradingCalculationSettings.builder()
                .id(e.getId()).tenantId(e.getTenantId()).averageMethod(e.getAverageMethod())
                .decimals(e.getDecimals()).rounding(e.getRounding())
                .generalCoursWeight(e.getGeneralCoursWeight()).generalCompoWeight(e.getGeneralCompoWeight())
                .build();
    }
}
