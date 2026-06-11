package gn.lacaeducation.platform.adapters.out.persistence;

import gn.lacaeducation.platform.adapters.out.persistence.mapper.PlatformMapper;
import gn.lacaeducation.platform.application.port.out.PlanRepository;
import gn.lacaeducation.platform.domain.Plan;
import gn.lacaeducation.platform.domain.PlanCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class PlanPersistenceAdapter implements PlanRepository {

    private final PlanJpaRepository jpaRepository;
    private final PlatformMapper mapper;

    @Override public Optional<Plan> findById(Long id) { return jpaRepository.findById(id).map(mapper::toDomain); }
    @Override public Optional<Plan> findByCode(PlanCode code) { return jpaRepository.findByCode(code).map(mapper::toDomain); }
    @Override public List<Plan> findAll() { return jpaRepository.findAll().stream().map(mapper::toDomain).toList(); }
    @Override public Plan save(Plan plan) { return mapper.toDomain(jpaRepository.save(mapper.toEntity(plan))); }
    @Override public void deleteById(Long id) { jpaRepository.deleteById(id); }
    @Override public boolean existsById(Long id) { return jpaRepository.existsById(id); }
}
