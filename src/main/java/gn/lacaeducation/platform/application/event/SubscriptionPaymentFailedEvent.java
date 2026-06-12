package gn.lacaeducation.platform.application.event;

/** Published when a subscription payment fails (PaiementAbonnementEchoue). Invoice stays unpaid; reminder sent. */
public record SubscriptionPaymentFailedEvent(Long tenantId, Long subscriptionId, Long paymentId) {}
