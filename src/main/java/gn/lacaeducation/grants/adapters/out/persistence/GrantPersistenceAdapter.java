package gn.lacaeducation.grants.adapters.out.persistence;
import gn.lacaeducation.grants.adapters.out.persistence.mapper.GrantsMapper;
import gn.lacaeducation.grants.application.port.out.GrantRepository;
import gn.lacaeducation.grants.domain.Grant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
class GrantPersistenceAdapter implements GrantRepository {
    private final GrantJpaRepository jpa;
    private final GrantsMapper mapper;
    @Override public Grant save(Grant g) { return mapper.toDomain(jpa.save(mapper.toEntity(g))); }
    @Override public Optional<Grant> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<Grant> findByTenantId(Long t) { return jpa.findByTenantIdOrderByCreatedAtDesc(t).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
