package gn.lacaeducation.discipline.adapters.out.persistence;

import gn.lacaeducation.discipline.adapters.out.persistence.mapper.DisciplineMapper;
import gn.lacaeducation.discipline.application.port.out.DisciplineRepository;
import gn.lacaeducation.discipline.domain.DisciplineIncident;
import gn.lacaeducation.discipline.domain.DisciplineStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class DisciplinePersistenceAdapter implements DisciplineRepository {
    private final DisciplineIncidentJpaRepository jpa;
    private final DisciplineMapper mapper;

    @Override public DisciplineIncident save(DisciplineIncident i) { return mapper.toDomain(jpa.save(mapper.toEntity(i))); }
    @Override public Optional<DisciplineIncident> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<DisciplineIncident> findByTenantId(Long t) { return jpa.findByTenantId(t).stream().map(mapper::toDomain).toList(); }
    @Override public List<DisciplineIncident> findByTenantIdAndStudentId(Long t, Long s) { return jpa.findByTenantIdAndStudentId(t, s).stream().map(mapper::toDomain).toList(); }
    @Override public List<DisciplineIncident> findByTenantIdAndStatus(Long t, DisciplineStatus s) { return jpa.findByTenantIdAndStatus(t, s).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
