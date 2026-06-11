package gn.lacaeducation.platform.adapters.out.persistence;

import gn.lacaeducation.platform.domain.PlanCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

interface PlanJpaRepository extends JpaRepository<PlanJpaEntity, Long> {
    Optional<PlanJpaEntity> findByCode(PlanCode code);
}
