package gn.lacaeducation.iam;
import java.util.Optional;
public interface TenantInfoProvider {
    Optional<TenantPublicInfo> findTenantInfo(Long tenantId);
}
