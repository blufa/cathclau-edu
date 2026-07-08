package gn.lacaeducation.accounting.application.port.out;
import gn.lacaeducation.accounting.domain.ReconciliationItem;
import java.util.List;
import java.util.Optional;
public interface ReconciliationItemRepository {
    ReconciliationItem save(ReconciliationItem i);
    Optional<ReconciliationItem> findById(Long id);
    List<ReconciliationItem> findByReconciliationId(Long reconciliationId);
    void deleteById(Long id);
    void deleteByReconciliationId(Long reconciliationId);
}
