package gn.lacaeducation.accounting.adapters.out.persistence;
import gn.lacaeducation.accounting.adapters.out.persistence.mapper.AccountingMapper;
import gn.lacaeducation.accounting.application.port.out.ChartOfAccountRepository;
import gn.lacaeducation.accounting.domain.ChartOfAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
class ChartOfAccountPersistenceAdapter implements ChartOfAccountRepository {
    private final ChartOfAccountJpaRepository jpa;
    private final AccountingMapper mapper;
    @Override public ChartOfAccount save(ChartOfAccount a) { return mapper.toDomain(jpa.save(mapper.toEntity(a))); }
    @Override public Optional<ChartOfAccount> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<ChartOfAccount> findByTenantId(Long t) { return jpa.findByTenantId(t).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
