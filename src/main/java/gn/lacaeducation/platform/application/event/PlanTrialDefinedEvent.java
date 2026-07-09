package gn.lacaeducation.platform.application.event;

/** Published when a plan is designated as the default trial. */
public record PlanTrialDefinedEvent(Long planId) {}
