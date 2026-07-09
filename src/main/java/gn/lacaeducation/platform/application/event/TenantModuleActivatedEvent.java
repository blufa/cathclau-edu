package gn.lacaeducation.platform.application.event;

import gn.lacaeducation.platform.domain.ModuleCode;

import java.time.LocalDate;

/** Published when a module is activated for a tenant (override or provisioning). */
public record TenantModuleActivatedEvent(Long tenantId, ModuleCode moduleCode, LocalDate expiration) {}
