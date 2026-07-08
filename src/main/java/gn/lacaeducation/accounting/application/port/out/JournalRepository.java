package gn.lacaeducation.accounting.application.port.out;
import gn.lacaeducation.accounting.domain.Journal;
import java.util.List;
import java.util.Optional;
public interface JournalRepository {
    Journal save(Journal j);
    Optional<Journal> findById(Long id);
    List<Journal> findByTenantId(Long tenantId);
    void deleteById(Long id);
}
