package gn.lacaeducation.platform.application.port.out;

import gn.lacaeducation.platform.domain.PlanChange;

import java.util.List;
import java.util.Optional;

public interface PlanChangeRepository {
    PlanChange save(PlanChange planChange);
    Optional<PlanChange> findById(Long id);
    List<PlanChange> findBySubscriptionId(Long subscriptionId);
}
