package gn.lacaeducation.accounting.adapters.out.persistence;
import gn.lacaeducation.accounting.adapters.out.persistence.mapper.AccountingMapper;
import gn.lacaeducation.accounting.application.port.out.BankReconciliationRepository;
import gn.lacaeducation.accounting.domain.BankReconciliation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
class BankReconciliationPersistenceAdapter implements BankReconciliationRepository {
    private final BankReconciliationJpaRepository jpa;
    private final AccountingMapper mapper;
    @Override public BankReconciliation save(BankReconciliation r) { return mapper.toDomain(jpa.save(mapper.toEntity(r))); }
    @Override public Optional<BankReconciliation> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<BankReconciliation> findByTenantId(Long t) { return jpa.findByTenantIdOrderByStatementDateDesc(t).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
