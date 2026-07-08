package gn.lacaeducation.platform.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

interface PlanModuleJpaRepository extends JpaRepository<PlanModuleJpaEntity, Long> {
    List<PlanModuleJpaEntity> findByPlanId(Long planId);
    Optional<PlanModuleJpaEntity> findByPlanIdAndModuleCode(Long planId, gn.lacaeducation.platform.domain.ModuleCode moduleCode);
}
