package gn.lacaeducation.grading.application.port.out;

import gn.lacaeducation.grading.domain.GradingScale;
import java.util.Optional;

public interface GradingScaleRepository {
    Optional<GradingScale> findByTenantId(Long tenantId);
    GradingScale save(GradingScale scale);
}
