package gn.lacaeducation.grading.application.port.in;

import gn.lacaeducation.grading.domain.AssessmentTypeConfig;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AssessmentTypeConfigUseCase {
    record TypeCommand(String code, String label, BigDecimal defaultCoefficient,
                       BigDecimal weight, String color, Boolean active) {}

    AssessmentTypeConfig create(Long tenantId, TypeCommand command);
    AssessmentTypeConfig update(Long id, TypeCommand command);
    List<AssessmentTypeConfig> findByTenant(Long tenantId);
    Optional<AssessmentTypeConfig> findById(Long id);
    void delete(Long id);
}
