package gn.lacaeducation.grading.application.port.in;

import gn.lacaeducation.grading.domain.CycleGroup;
import gn.lacaeducation.grading.domain.GradingScale;
import java.math.BigDecimal;

public interface GradingScaleUseCase {
    record UpdateScaleCommand(BigDecimal passThreshold, BigDecimal passableMin,
                              BigDecimal assezBienMin, BigDecimal bienMin, BigDecimal tresBienMin,
                              BigDecimal felicitationsMin, BigDecimal complimentsMin,
                              BigDecimal distinctionNoFailingThreshold) {}

    /** Returns the tenant's configured scale for this cycle group, or the default scale if none has been set. */
    GradingScale getForTenant(Long tenantId, CycleGroup cycleGroup);
    GradingScale update(Long tenantId, CycleGroup cycleGroup, UpdateScaleCommand command);
}
