package gn.lacaeducation.accounting.application.port.out;
import gn.lacaeducation.accounting.domain.FiscalYear;
import java.util.List;
import java.util.Optional;
public interface FiscalYearRepository {
    FiscalYear save(FiscalYear f);
    Optional<FiscalYear> findById(Long id);
    List<FiscalYear> findByTenantId(Long tenantId);
}
