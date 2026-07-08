package gn.lacaeducation.grading.application.port.out;

import gn.lacaeducation.grading.domain.AssessmentSessionSettings;
import java.util.Optional;

public interface AssessmentSessionSettingsRepository {
    Optional<AssessmentSessionSettings> findByTenantId(Long tenantId);
    AssessmentSessionSettings save(AssessmentSessionSettings settings);
}
