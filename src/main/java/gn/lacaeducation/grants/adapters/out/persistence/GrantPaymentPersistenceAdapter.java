package gn.lacaeducation.grants.adapters.out.persistence;
import gn.lacaeducation.grants.adapters.out.persistence.mapper.GrantsMapper;
import gn.lacaeducation.grants.application.port.out.GrantPaymentRepository;
import gn.lacaeducation.grants.domain.GrantPayment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
class GrantPaymentPersistenceAdapter implements GrantPaymentRepository {
    private final GrantPaymentJpaRepository jpa;
    private final GrantsMapper mapper;
    @Override public GrantPayment save(GrantPayment p) { return mapper.toDomain(jpa.save(mapper.toEntity(p))); }
    @Override public Optional<GrantPayment> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<GrantPayment> findByGrantId(Long gid) { return jpa.findByGrantIdOrderByPaymentDateDesc(gid).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
    @Override public void deleteByGrantId(Long gid) { jpa.deleteByGrantId(gid); }
}
