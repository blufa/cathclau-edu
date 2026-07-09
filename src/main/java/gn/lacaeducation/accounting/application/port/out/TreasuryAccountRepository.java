package gn.lacaeducation.accounting.application.port.out;
import gn.lacaeducation.accounting.domain.TreasuryAccount;
import java.util.List;
import java.util.Optional;
public interface TreasuryAccountRepository {
    TreasuryAccount save(TreasuryAccount a);
    Optional<TreasuryAccount> findById(Long id);
    List<TreasuryAccount> findByTenantId(Long tenantId);
    void deleteById(Long id);
}
