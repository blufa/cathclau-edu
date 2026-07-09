package gn.lacaeducation.platform.application.event;

/** Published when a school is archived (terminal, read-only) — §1. */
public record TenantArchivedEvent(Long tenantId) {}
