package gn.lacaeducation.grading.application.port.in;

import gn.lacaeducation.grading.domain.AverageMethod;
import gn.lacaeducation.grading.domain.GradingCalculationSettings;
import gn.lacaeducation.grading.domain.RoundingPolicy;

import java.math.BigDecimal;

public interface GradingCalculationSettingsUseCase {
    record UpdateCommand(AverageMethod averageMethod, Integer decimals, RoundingPolicy rounding,
                         BigDecimal generalCoursWeight, BigDecimal generalCompoWeight) {}

    /** Réglage du tenant, ou les valeurs par défaut si non configuré. */
    GradingCalculationSettings getForTenant(Long tenantId);

    GradingCalculationSettings update(Long tenantId, UpdateCommand command);
}
