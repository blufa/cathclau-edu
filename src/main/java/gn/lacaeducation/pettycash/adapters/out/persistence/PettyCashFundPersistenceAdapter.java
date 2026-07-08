package gn.lacaeducation.pettycash.adapters.out.persistence;
import gn.lacaeducation.pettycash.adapters.out.persistence.mapper.PettyCashMapper;
import gn.lacaeducation.pettycash.application.port.out.PettyCashFundRepository;
import gn.lacaeducation.pettycash.domain.PettyCashFund;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
class PettyCashFundPersistenceAdapter implements PettyCashFundRepository {
    private final PettyCashFundJpaRepository jpa;
    private final PettyCashMapper mapper;
    @Override public PettyCashFund save(PettyCashFund f) { return mapper.toDomain(jpa.save(mapper.toEntity(f))); }
    @Override public Optional<PettyCashFund> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<PettyCashFund> findByTenantId(Long t) { return jpa.findByTenantId(t).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
