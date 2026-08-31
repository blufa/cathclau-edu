package gn.lacaeducation.platform.adapters.out.persistence;

import gn.lacaeducation.platform.adapters.out.persistence.mapper.PlatformMapper;
import gn.lacaeducation.platform.application.port.out.PlatformFeeSettlementRepository;
import gn.lacaeducation.platform.domain.PaymentStatus;
import gn.lacaeducation.platform.domain.PlatformFeeSettlement;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class PlatformFeeSettlementPersistenceAdapter implements PlatformFeeSettlementRepository {

    private final PlatformFeeSettlementJpaRepository jpa;
    private final PlatformMapper mapper;

    @Override
    public PlatformFeeSettlement save(PlatformFeeSettlement settlement) {
        return mapper.toDomain(jpa.save(mapper.toEntity(settlement)));
    }

    @Override
    public Optional<PlatformFeeSettlement> findById(Long id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<PlatformFeeSettlement> findByTenantId(Long tenantId) {
        return jpa.findByTenantId(tenantId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public BigDecimal sumSucceededForCurrentTenant() {
        return jpa.sumByStatus(PaymentStatus.SUCCESSFUL);
    }

    @Override
    public BigDecimal sumSucceededByTenantId(Long tenantId) {
        return jpa.sumByTenantIdAndStatus(tenantId, PaymentStatus.SUCCESSFUL);
    }
}
