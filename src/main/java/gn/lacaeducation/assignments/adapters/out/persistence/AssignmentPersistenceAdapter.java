package gn.lacaeducation.assignments.adapters.out.persistence;

import gn.lacaeducation.assignments.adapters.out.persistence.mapper.AssignmentMapper;
import gn.lacaeducation.assignments.application.port.out.AssignmentRepository;
import gn.lacaeducation.assignments.domain.Assignment;
import gn.lacaeducation.assignments.domain.AssignmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class AssignmentPersistenceAdapter implements AssignmentRepository {
    private final AssignmentJpaRepository jpa;
    private final AssignmentMapper mapper;

    @Override public Assignment save(Assignment a) { return mapper.toDomain(jpa.save(mapper.toEntity(a))); }
    @Override public Optional<Assignment> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<Assignment> findByTenantId(Long t) { return jpa.findByTenantId(t).stream().map(mapper::toDomain).toList(); }
    @Override public List<Assignment> findByTenantIdAndClassId(Long t, Long c) { return jpa.findByTenantIdAndClassId(t, c).stream().map(mapper::toDomain).toList(); }
    @Override public List<Assignment> findByTenantIdAndStatus(Long t, AssignmentStatus s) { return jpa.findByTenantIdAndStatus(t, s).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
