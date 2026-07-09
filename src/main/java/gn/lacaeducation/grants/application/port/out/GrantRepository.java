package gn.lacaeducation.grants.application.port.out;
import gn.lacaeducation.grants.domain.Grant;
import java.util.List;
import java.util.Optional;
public interface GrantRepository {
    Grant save(Grant g);
    Optional<Grant> findById(Long id);
    List<Grant> findByTenantId(Long tenantId);
    void deleteById(Long id);
}
