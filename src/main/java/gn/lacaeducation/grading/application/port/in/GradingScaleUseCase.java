package gn.lacaeducation.grading.application.port.in;

import gn.lacaeducation.grading.domain.GradingScale;
import java.math.BigDecimal;

public interface GradingScaleUseCase {
    record UpdateScaleCommand(BigDecimal passThreshold, BigDecimal passableMin,
                              BigDecimal assezBienMin, BigDecimal bienMin, BigDecimal tresBienMin,
                              BigDecimal felicitationsMin, BigDecimal complimentsMin,
                              BigDecimal distinctionNoFailingThreshold) {}

    /** Returns the tenant's configured scale, or the default scale if none has been set. */
    GradingScale getForTenant(Long tenantId);
    GradingScale update(Long tenantId, UpdateScaleCommand command);
}
