package gn.lacaeducation.accounting.application.port.out;
import gn.lacaeducation.accounting.domain.JournalEntry;
import java.util.List;
import java.util.Optional;
public interface JournalEntryRepository {
    JournalEntry save(JournalEntry e);
    Optional<JournalEntry> findById(Long id);
    List<JournalEntry> findByTenantId(Long tenantId);
    List<JournalEntry> findByTenantIdAndFiscalYearId(Long tenantId, Long fiscalYearId);
    long countByJournalIdAndFiscalYearId(Long journalId, Long fiscalYearId);
    void deleteById(Long id);
}
