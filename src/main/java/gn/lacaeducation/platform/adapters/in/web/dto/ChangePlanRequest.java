package gn.lacaeducation.platform.adapters.in.web.dto;

import gn.lacaeducation.platform.domain.BillingPeriod;
import jakarta.validation.constraints.NotNull;

public record ChangePlanRequest(
    @NotNull Long newPlanId,
    @NotNull BillingPeriod newBillingPeriod
) {}
