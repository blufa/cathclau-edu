package gn.lacaeducation.platform.application.event;

import gn.lacaeducation.platform.domain.PlanChangeType;

/** Published on an upgrade/downgrade. Triggers recomputation of the allowed student quota. */
public record PlanChangedEvent(Long subscriptionId, Long oldPlanId, Long newPlanId, PlanChangeType type) {}
