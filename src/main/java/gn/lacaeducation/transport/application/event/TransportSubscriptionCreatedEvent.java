package gn.lacaeducation.transport.application.event;

/**
 * Published when a student subscribes to a transport route. Consumed by billing to invoice the student
 * against the TRANSPORT school fee already configured (with its own échéances — mensuelles,
 * trimestrielles ou annuelle, exactly like tuition) for the student's current class, via the school
 * fee's own tranche system rather than a flat price carried by the route/subscription. The transport
 * fee must be configured in Frais scolaires (type Transport) before subscribing for an invoice to be
 * generated; the route itself only describes logistics (vehicle, driver, stops), not pricing.
 */
public record TransportSubscriptionCreatedEvent(Long tenantId, Long studentId, Long routeId) {}
