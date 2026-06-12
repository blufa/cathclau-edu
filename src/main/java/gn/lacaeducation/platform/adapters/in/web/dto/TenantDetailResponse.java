package gn.lacaeducation.platform.adapters.in.web.dto;

import gn.lacaeducation.platform.application.port.in.CreateTenantUseCase.TenantDetail;

/** Tenant detail with its current subscription summary and consumed student quota (§1.1). */
public record TenantDetailResponse(
        TenantResponse tenant,
        Long currentSubscriptionId,
        String subscriptionStatus,
        Long planId,
        String planName,
        Integer studentLimit,
        long consumedStudents
) {
    public static TenantDetailResponse from(TenantDetail d) {
        return new TenantDetailResponse(
                TenantResponse.from(d.tenant()),
                d.currentSubscription() == null ? null : d.currentSubscription().getId(),
                d.currentSubscription() == null ? null : d.currentSubscription().getStatus().name(),
                d.plan() == null ? null : d.plan().getId(),
                d.plan() == null ? null : d.plan().getName(),
                d.studentLimit(),
                d.consumedStudents()
        );
    }
}
