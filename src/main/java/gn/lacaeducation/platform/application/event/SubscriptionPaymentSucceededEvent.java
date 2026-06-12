package gn.lacaeducation.platform.application.event;

/** Published when a subscription payment succeeds (PaiementAbonnementReussi). Triggers receipt + notification. */
public record SubscriptionPaymentSucceededEvent(Long tenantId, Long subscriptionId, Long paymentId) {}
