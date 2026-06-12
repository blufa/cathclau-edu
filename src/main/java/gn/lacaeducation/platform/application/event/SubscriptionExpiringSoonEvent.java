package gn.lacaeducation.platform.application.event;

import java.time.LocalDate;

/** Published J-7 before a subscription's end date. Consumed by communication to remind the admin. */
public record SubscriptionExpiringSoonEvent(Long tenantId, Long subscriptionId, LocalDate endDate) {}
