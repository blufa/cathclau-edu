package gn.lacaeducation.platform.adapters.out.persistence;

import gn.lacaeducation.platform.adapters.out.persistence.mapper.PlatformMapper;
import gn.lacaeducation.platform.application.port.out.PlatformFeeLedgerRepository;
import gn.lacaeducation.platform.domain.PlatformFeeLedgerEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
class PlatformFeeLedgerPersistenceAdapter implements PlatformFeeLedgerRepository {

    private final PlatformFeeLedgerEntryJpaRepository jpa;
    private final PlatformMapper mapper;

    @Override
    public PlatformFeeLedgerEntry save(PlatformFeeLedgerEntry entry) {
        return mapper.toDomain(jpa.save(mapper.toEntity(entry)));
    }

    @Override
    public List<PlatformFeeLedgerEntry> findByTenantId(Long tenantId) {
        return jpa.findByTenantId(tenantId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public BigDecimal sumForCurrentTenant() {
        return jpa.sumAll();
    }

    @Override
    public BigDecimal sumByTenantId(Long tenantId) {
        return jpa.sumByTenantId(tenantId);
    }
}
