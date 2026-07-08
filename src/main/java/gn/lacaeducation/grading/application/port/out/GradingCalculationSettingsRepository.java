package gn.lacaeducation.grading.application.port.out;

import gn.lacaeducation.grading.domain.GradingCalculationSettings;

import java.util.Optional;

public interface GradingCalculationSettingsRepository {
    Optional<GradingCalculationSettings> findByTenantId(Long tenantId);
    GradingCalculationSettings save(GradingCalculationSettings settings);
}
