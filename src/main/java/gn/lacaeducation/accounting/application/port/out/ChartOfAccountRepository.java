package gn.lacaeducation.accounting.application.port.out;
import gn.lacaeducation.accounting.domain.ChartOfAccount;
import java.util.List;
import java.util.Optional;
public interface ChartOfAccountRepository {
    ChartOfAccount save(ChartOfAccount a);
    Optional<ChartOfAccount> findById(Long id);
    List<ChartOfAccount> findByTenantId(Long tenantId);
    void deleteById(Long id);
}
