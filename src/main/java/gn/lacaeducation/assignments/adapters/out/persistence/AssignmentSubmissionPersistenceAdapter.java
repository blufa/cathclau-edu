package gn.lacaeducation.assignments.adapters.out.persistence;

import gn.lacaeducation.assignments.adapters.out.persistence.mapper.AssignmentMapper;
import gn.lacaeducation.assignments.application.port.out.AssignmentSubmissionRepository;
import gn.lacaeducation.assignments.domain.AssignmentSubmission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class AssignmentSubmissionPersistenceAdapter implements AssignmentSubmissionRepository {
    private final AssignmentSubmissionJpaRepository jpa;
    private final AssignmentMapper mapper;

    @Override public AssignmentSubmission save(AssignmentSubmission s) { return mapper.toDomain(jpa.save(mapper.toEntity(s))); }
    @Override public Optional<AssignmentSubmission> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<AssignmentSubmission> findByAssignmentId(Long a) { return jpa.findByAssignmentId(a).stream().map(mapper::toDomain).toList(); }
    @Override public Optional<AssignmentSubmission> findByAssignmentIdAndStudentId(Long a, Long s) { return jpa.findByAssignmentIdAndStudentId(a, s).map(mapper::toDomain); }
    @Override public List<AssignmentSubmission> findByStudentId(Long s) { return jpa.findByStudentId(s).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
