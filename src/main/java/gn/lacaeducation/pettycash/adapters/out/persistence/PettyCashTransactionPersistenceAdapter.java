package gn.lacaeducation.pettycash.adapters.out.persistence;
import gn.lacaeducation.pettycash.adapters.out.persistence.mapper.PettyCashMapper;
import gn.lacaeducation.pettycash.application.port.out.PettyCashTransactionRepository;
import gn.lacaeducation.pettycash.domain.PettyCashTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
class PettyCashTransactionPersistenceAdapter implements PettyCashTransactionRepository {
    private final PettyCashTransactionJpaRepository jpa;
    private final PettyCashMapper mapper;
    @Override public PettyCashTransaction save(PettyCashTransaction t) { return mapper.toDomain(jpa.save(mapper.toEntity(t))); }
    @Override public Optional<PettyCashTransaction> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<PettyCashTransaction> findByFundId(Long fid) { return jpa.findByFundIdOrderByTxDateDescIdDesc(fid).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
    @Override public void deleteByFundId(Long fid) { jpa.deleteByFundId(fid); }
}
