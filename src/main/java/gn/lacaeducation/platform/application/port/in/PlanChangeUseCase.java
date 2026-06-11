package gn.lacaeducation.platform.application.port.in;

import gn.lacaeducation.platform.domain.BillingPeriod;
import gn.lacaeducation.platform.domain.PlanChange;

import java.util.List;

public interface PlanChangeUseCase {
    void changePlan(ChangePlanCommand command);
    List<PlanChange> findBySubscription(Long subscriptionId);

    record ChangePlanCommand(
        Long subscriptionId,
        Long newPlanId,
        BillingPeriod newBillingPeriod
    ) {}
}
