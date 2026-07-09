package gn.lacaeducation.assignments.application.port.out;

import gn.lacaeducation.assignments.domain.AssignmentSubmission;

import java.util.List;
import java.util.Optional;

public interface AssignmentSubmissionRepository {
    AssignmentSubmission save(AssignmentSubmission s);
    Optional<AssignmentSubmission> findById(Long id);
    List<AssignmentSubmission> findByAssignmentId(Long assignmentId);
    Optional<AssignmentSubmission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);
    List<AssignmentSubmission> findByStudentId(Long studentId);
    void deleteById(Long id);
}
