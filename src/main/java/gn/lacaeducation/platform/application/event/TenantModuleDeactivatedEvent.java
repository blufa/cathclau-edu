package gn.lacaeducation.platform.application.event;

import gn.lacaeducation.platform.domain.ModuleCode;

/** Published when a module is deactivated for a tenant. */
public record TenantModuleDeactivatedEvent(Long tenantId, ModuleCode moduleCode) {}
