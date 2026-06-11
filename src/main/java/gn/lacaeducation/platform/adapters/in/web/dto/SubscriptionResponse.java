package gn.lacaeducation.platform.adapters.in.web.dto;

import gn.lacaeducation.platform.domain.Subscription;
import gn.lacaeducation.platform.domain.SubscriptionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubscriptionResponse(
    Long id,
    Long tenantId,
    Long planId,
    LocalDate startDate,
    LocalDate endDate,
    SubscriptionStatus status,
    Boolean autoRenewal,
    BigDecimal amount
) {
    public static SubscriptionResponse from(Subscription subscription) {
        return new SubscriptionResponse(
            subscription.getId(), subscription.getTenantId(), subscription.getPlanId(),
            subscription.getStartDate(), subscription.getEndDate(), subscription.getStatus(),
            subscription.getAutoRenewal(), subscription.getAmount()
        );
    }
}
