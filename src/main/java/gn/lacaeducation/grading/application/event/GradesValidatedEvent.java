package gn.lacaeducation.grading.application.event;

/** Published when grades are validated (NotesValidees). Allows recomputing cached averages. */
public record GradesValidatedEvent(Long assessmentId) {}
