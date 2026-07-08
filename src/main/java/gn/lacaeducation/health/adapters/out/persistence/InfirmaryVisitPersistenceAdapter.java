package gn.lacaeducation.health.adapters.out.persistence;

import gn.lacaeducation.health.adapters.out.persistence.mapper.HealthMapper;
import gn.lacaeducation.health.application.port.out.InfirmaryVisitRepository;
import gn.lacaeducation.health.domain.InfirmaryVisit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class InfirmaryVisitPersistenceAdapter implements InfirmaryVisitRepository {
    private final InfirmaryVisitJpaRepository jpa;
    private final HealthMapper mapper;

    @Override public InfirmaryVisit save(InfirmaryVisit v) { return mapper.toDomain(jpa.save(mapper.toEntity(v))); }
    @Override public Optional<InfirmaryVisit> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<InfirmaryVisit> findByTenantId(Long t) { return jpa.findByTenantIdOrderByVisitDateDesc(t).stream().map(mapper::toDomain).toList(); }
    @Override public List<InfirmaryVisit> findByStudentId(Long s) { return jpa.findByStudentIdOrderByVisitDateDesc(s).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
