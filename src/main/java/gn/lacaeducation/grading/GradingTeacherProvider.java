package gn.lacaeducation.grading;

/**
 * Public API of the grading module letting other modules (staff) check whether a teacher is
 * referenced by grading data (evaluations), to forbid deleting a teacher who owns evaluations.
 */
public interface GradingTeacherProvider {
    boolean teacherHasEvaluations(Long teacherId);
}
