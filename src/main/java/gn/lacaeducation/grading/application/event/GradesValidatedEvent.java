package gn.lacaeducation.grading.application.event;

/**
 * Published when grades are validated (NotesValidees). Carries the assessment's class and period
 * so listeners can recompute the cached averages / report cards for that class + period.
 */
public record GradesValidatedEvent(Long assessmentId, Long classId, Long periodId) {}
