package gn.lacaeducation.accounting.adapters.out.persistence;
import gn.lacaeducation.accounting.adapters.out.persistence.mapper.AccountingMapper;
import gn.lacaeducation.accounting.application.port.out.JournalRepository;
import gn.lacaeducation.accounting.domain.Journal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
class JournalPersistenceAdapter implements JournalRepository {
    private final JournalJpaRepository jpa;
    private final AccountingMapper mapper;
    @Override public Journal save(Journal j) { return mapper.toDomain(jpa.save(mapper.toEntity(j))); }
    @Override public Optional<Journal> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<Journal> findByTenantId(Long t) { return jpa.findByTenantId(t).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
