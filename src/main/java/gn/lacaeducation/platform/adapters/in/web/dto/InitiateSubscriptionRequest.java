package gn.lacaeducation.platform.adapters.in.web.dto;
import gn.lacaeducation.platform.domain.BillingPeriod;
import jakarta.validation.constraints.NotNull;
public record InitiateSubscriptionRequest(
        @NotNull Long tenantId, @NotNull Long planId,
        @NotNull BillingPeriod billingPeriod, Boolean autoRenewal) {}
