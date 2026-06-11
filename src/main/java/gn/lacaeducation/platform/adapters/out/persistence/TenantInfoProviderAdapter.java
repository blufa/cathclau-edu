package gn.lacaeducation.platform.adapters.out.persistence;
import gn.lacaeducation.iam.TenantInfoProvider;
import gn.lacaeducation.iam.TenantPublicInfo;
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
}
