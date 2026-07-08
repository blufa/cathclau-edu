package gn.lacaeducation.accounting.adapters.out.persistence;
import gn.lacaeducation.accounting.adapters.out.persistence.mapper.AccountingMapper;
import gn.lacaeducation.accounting.application.port.out.ReconciliationItemRepository;
import gn.lacaeducation.accounting.domain.ReconciliationItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
class ReconciliationItemPersistenceAdapter implements ReconciliationItemRepository {
    private final ReconciliationItemJpaRepository jpa;
    private final AccountingMapper mapper;
    @Override public ReconciliationItem save(ReconciliationItem i) { return mapper.toDomain(jpa.save(mapper.toEntity(i))); }
    @Override public Optional<ReconciliationItem> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<ReconciliationItem> findByReconciliationId(Long rid) { return jpa.findByReconciliationId(rid).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
    @Override public void deleteByReconciliationId(Long rid) { jpa.deleteByReconciliationId(rid); }
}
