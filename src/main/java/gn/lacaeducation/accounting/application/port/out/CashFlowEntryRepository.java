package gn.lacaeducation.accounting.application.port.out;
import gn.lacaeducation.accounting.domain.CashFlowEntry;
import gn.lacaeducation.accounting.domain.CashFlowReferenceType;
import java.util.List;
import java.util.Optional;
public interface CashFlowEntryRepository {
    CashFlowEntry save(CashFlowEntry e);
    Optional<CashFlowEntry> findById(Long id);
    List<CashFlowEntry> findByTenantId(Long tenantId);
    List<CashFlowEntry> findByAccountId(Long accountId);
    Optional<CashFlowEntry> findByReference(CashFlowReferenceType referenceType, Long referenceId);
    void deleteById(Long id);
}
