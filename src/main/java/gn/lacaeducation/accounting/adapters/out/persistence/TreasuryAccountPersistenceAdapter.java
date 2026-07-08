package gn.lacaeducation.accounting.adapters.out.persistence;
import gn.lacaeducation.accounting.adapters.out.persistence.mapper.AccountingMapper;
import gn.lacaeducation.accounting.application.port.out.TreasuryAccountRepository;
import gn.lacaeducation.accounting.domain.TreasuryAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
class TreasuryAccountPersistenceAdapter implements TreasuryAccountRepository {
    private final TreasuryAccountJpaRepository jpa;
    private final AccountingMapper mapper;
    @Override public TreasuryAccount save(TreasuryAccount a) { return mapper.toDomain(jpa.save(mapper.toEntity(a))); }
    @Override public Optional<TreasuryAccount> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<TreasuryAccount> findByTenantId(Long t) { return jpa.findByTenantId(t).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
