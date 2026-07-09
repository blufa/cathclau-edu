package gn.lacaeducation.expenses.application.port.out;
import gn.lacaeducation.expenses.domain.Supplier;
import java.util.List;
import java.util.Optional;
public interface SupplierRepository {
    Supplier save(Supplier s);
    Optional<Supplier> findById(Long id);
    List<Supplier> findByTenantId(Long tenantId);
    void deleteById(Long id);
}
