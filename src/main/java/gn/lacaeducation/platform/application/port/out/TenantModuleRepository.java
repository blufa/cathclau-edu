package gn.lacaeducation.platform.application.port.out;

import gn.lacaeducation.platform.domain.ModuleCode;
import gn.lacaeducation.platform.domain.TenantModule;

import java.util.List;
import java.util.Optional;

public interface TenantModuleRepository {
    TenantModule save(TenantModule tm);
    List<TenantModule> findByTenantId(Long tenantId);
    Optional<TenantModule> findByTenantIdAndModuleCode(Long tenantId, ModuleCode moduleCode);
}
