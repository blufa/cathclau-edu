package gn.lacaeducation.grading.application.port.out;

import gn.lacaeducation.grading.domain.AssessmentTypeConfig;

import java.util.List;
import java.util.Optional;

public interface AssessmentTypeConfigRepository {
    AssessmentTypeConfig save(AssessmentTypeConfig config);
    Optional<AssessmentTypeConfig> findById(Long id);
    List<AssessmentTypeConfig> findByTenantId(Long tenantId);
    boolean existsByTenantIdAndCode(Long tenantId, String code);
    void deleteById(Long id);
}
