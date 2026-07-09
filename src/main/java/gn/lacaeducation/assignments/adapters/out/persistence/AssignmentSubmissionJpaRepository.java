package gn.lacaeducation.assignments.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface AssignmentSubmissionJpaRepository extends JpaRepository<AssignmentSubmissionJpaEntity, Long> {
    List<AssignmentSubmissionJpaEntity> findByAssignmentId(Long assignmentId);
    Optional<AssignmentSubmissionJpaEntity> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);
    List<AssignmentSubmissionJpaEntity> findByStudentId(Long studentId);
}
