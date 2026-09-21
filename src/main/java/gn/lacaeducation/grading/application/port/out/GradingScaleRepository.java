package gn.lacaeducation.grading.application.port.out;

import gn.lacaeducation.grading.domain.CycleGroup;
import gn.lacaeducation.grading.domain.GradingScale;
import java.util.Optional;

public interface GradingScaleRepository {
    Optional<GradingScale> findByTenantIdAndCycleGroup(Long tenantId, CycleGroup cycleGroup);
    GradingScale save(GradingScale scale);
}
