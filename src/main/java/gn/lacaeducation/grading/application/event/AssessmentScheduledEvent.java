package gn.lacaeducation.grading.application.event;

import java.time.LocalDate;

/**
 * Published when an assessment (évaluation) is scheduled. Consumed by communication to notify every
 * student of the class on the contact captured on their student record (§7.1), and by lessons to
 * record the assessment as a teaching session counted in the teacher's worked hours.
 */
public record AssessmentScheduledEvent(Long tenantId, Long assessmentId, Long classId, Long subjectId,
                                       Long teacherId, String label, LocalDate date, Integer sessionMinutes) {}
