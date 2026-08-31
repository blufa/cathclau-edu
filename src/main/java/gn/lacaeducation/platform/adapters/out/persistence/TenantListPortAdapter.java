package gn.lacaeducation.platform.adapters.out.persistence;

import gn.lacaeducation.platform.TenantListPort;
import gn.lacaeducation.platform.application.port.out.TenantRepository;
import gn.lacaeducation.platform.domain.TenantStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class TenantListPortAdapter implements TenantListPort {
    private final TenantRepository tenantRepository;

    @Override
    public List<TenantRef> listActiveTenantIds() {
        return tenantRepository.findAll().stream()
                .filter(t -> t.getStatus() == TenantStatus.ACTIVE || t.getStatus() == TenantStatus.TRIAL)
                .map(t -> new TenantRef(t.getId()))
                .toList();
    }
}
