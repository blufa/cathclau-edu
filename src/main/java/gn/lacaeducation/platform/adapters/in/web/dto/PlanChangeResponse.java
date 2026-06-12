package gn.lacaeducation.platform.adapters.in.web.dto;

import gn.lacaeducation.platform.domain.PlanChange;
import gn.lacaeducation.platform.domain.PlanChangeType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PlanChangeResponse(
        Long id,
        Long subscriptionId,
        Long oldPlanId,
        Long newPlanId,
        PlanChangeType type,
        BigDecimal proratedAmount,
        LocalDate effectiveDate
) {
    public static PlanChangeResponse from(PlanChange c) {
        return new PlanChangeResponse(c.getId(), c.getSubscriptionId(), c.getOldPlanId(),
                c.getNewPlanId(), c.getType(), c.getProratedAmount(), c.getEffectiveDate());
    }
}
