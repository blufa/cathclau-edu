package gn.lacaeducation.accounting.adapters.out.persistence;
import gn.lacaeducation.accounting.adapters.out.persistence.mapper.AccountingMapper;
import gn.lacaeducation.accounting.application.port.out.CashFlowEntryRepository;
import gn.lacaeducation.accounting.domain.CashFlowEntry;
import gn.lacaeducation.accounting.domain.CashFlowReferenceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
class CashFlowEntryPersistenceAdapter implements CashFlowEntryRepository {
    private final CashFlowEntryJpaRepository jpa;
    private final AccountingMapper mapper;
    @Override public CashFlowEntry save(CashFlowEntry e) { return mapper.toDomain(jpa.save(mapper.toEntity(e))); }
    @Override public Optional<CashFlowEntry> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<CashFlowEntry> findByTenantId(Long t) { return jpa.findByTenantId(t).stream().map(mapper::toDomain).toList(); }
    @Override public List<CashFlowEntry> findByAccountId(Long a) { return jpa.findByAccountId(a).stream().map(mapper::toDomain).toList(); }
    @Override public Optional<CashFlowEntry> findByReference(CashFlowReferenceType rt, Long rid) { return jpa.findByReferenceTypeAndReferenceId(rt, rid).map(mapper::toDomain); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
