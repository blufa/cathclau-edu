package gn.lacaeducation.accounting.adapters.out.persistence;
import gn.lacaeducation.accounting.adapters.out.persistence.mapper.AccountingMapper;
import gn.lacaeducation.accounting.application.port.out.JournalEntryRepository;
import gn.lacaeducation.accounting.domain.JournalEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
class JournalEntryPersistenceAdapter implements JournalEntryRepository {
    private final JournalEntryJpaRepository jpa;
    private final AccountingMapper mapper;
    @Override public JournalEntry save(JournalEntry e) { return mapper.toDomain(jpa.save(mapper.toEntity(e))); }
    @Override public Optional<JournalEntry> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<JournalEntry> findByTenantId(Long t) { return jpa.findByTenantId(t).stream().map(mapper::toDomain).toList(); }
    @Override public List<JournalEntry> findByTenantIdAndFiscalYearId(Long t, Long fy) { return jpa.findByTenantIdAndFiscalYearId(t, fy).stream().map(mapper::toDomain).toList(); }
    @Override public long countByJournalIdAndFiscalYearId(Long j, Long fy) { return jpa.countByJournalIdAndFiscalYearId(j, fy); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
