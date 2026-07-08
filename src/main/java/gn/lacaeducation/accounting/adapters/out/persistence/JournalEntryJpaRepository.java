package gn.lacaeducation.accounting.adapters.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface JournalEntryJpaRepository extends JpaRepository<JournalEntryJpaEntity, Long> {
    List<JournalEntryJpaEntity> findByTenantId(Long tenantId);
    List<JournalEntryJpaEntity> findByTenantIdAndFiscalYearId(Long tenantId, Long fiscalYearId);
    long countByJournalIdAndFiscalYearId(Long journalId, Long fiscalYearId);
}
