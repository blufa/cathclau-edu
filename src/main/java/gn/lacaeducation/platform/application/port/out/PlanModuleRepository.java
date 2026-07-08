package gn.lacaeducation.platform.application.port.out;

import gn.lacaeducation.platform.domain.ModuleCode;
import gn.lacaeducation.platform.domain.PlanModule;

import java.util.List;
import java.util.Optional;

public interface PlanModuleRepository {
    PlanModule save(PlanModule pm);
    List<PlanModule> findByPlanId(Long planId);
    Optional<PlanModule> findByPlanIdAndModuleCode(Long planId, ModuleCode moduleCode);
    void delete(Long id);
}
