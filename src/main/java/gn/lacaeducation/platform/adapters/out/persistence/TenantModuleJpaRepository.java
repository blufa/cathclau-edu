package gn.lacaeducation.platform.adapters.out.persistence;

import gn.lacaeducation.platform.domain.ModuleCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

interface TenantModuleJpaRepository extends JpaRepository<TenantModuleJpaEntity, Long> {
    List<TenantModuleJpaEntity> findByTenantId(Long tenantId);
    Optional<TenantModuleJpaEntity> findByTenantIdAndModuleCode(Long tenantId, ModuleCode moduleCode);
}
