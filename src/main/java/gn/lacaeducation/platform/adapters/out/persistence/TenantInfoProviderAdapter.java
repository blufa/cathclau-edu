package gn.lacaeducation.platform.adapters.out.persistence;
import gn.lacaeducation.platform.TenantInfoProvider;
import gn.lacaeducation.platform.TenantPublicInfo;
import gn.lacaeducation.platform.domain.TenantStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Optional;
@Component @RequiredArgsConstructor
public class TenantInfoProviderAdapter implements TenantInfoProvider {
    private final TenantJpaRepository jpa;
    @Override
    public Optional<TenantPublicInfo> findTenantInfo(Long tenantId) {
        return jpa.findById(tenantId).map(t ->
                new TenantPublicInfo(t.getId(), t.getSchoolName(), t.getEmail(), t.getPhone()));
    }
    @Override
    public boolean isTenantActive(Long tenantId) {
        if (tenantId == null) return true;
        return jpa.findById(tenantId)
                .map(t -> t.getStatus() == TenantStatus.ACTIVE)
                .orElse(false);
    }
}
