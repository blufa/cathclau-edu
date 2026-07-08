package gn.lacaeducation.accounting.application.port.out;
import gn.lacaeducation.accounting.domain.BankReconciliation;
import java.util.List;
import java.util.Optional;
public interface BankReconciliationRepository {
    BankReconciliation save(BankReconciliation r);
    Optional<BankReconciliation> findById(Long id);
    List<BankReconciliation> findByTenantId(Long tenantId);
    void deleteById(Long id);
}
