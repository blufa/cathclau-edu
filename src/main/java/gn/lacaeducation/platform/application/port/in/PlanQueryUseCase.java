package gn.lacaeducation.platform.application.port.in;

import gn.lacaeducation.platform.domain.Plan;

import java.util.List;
import java.util.Optional;

public interface PlanQueryUseCase {
    Plan findById(Long id);
    List<Plan> findAll();
}
