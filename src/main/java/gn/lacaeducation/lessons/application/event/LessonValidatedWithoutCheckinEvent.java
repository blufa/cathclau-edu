package gn.lacaeducation.lessons.application.event;

import java.time.LocalDate;

/**
 * Published when an admin/director validates a lesson as DONE (counted toward the teacher's paid
 * hours) despite no matching teacher check-in being found for that date/slot (§retour fondateurs,
 * contrôle anti-fraude) — a legitimate override (scanner en panne, admin présent en personne) stays
 * possible, mais doit toujours laisser une trace pour revue ultérieure. Consumed by the audit trail.
 */
public record LessonValidatedWithoutCheckinEvent(Long tenantId, Long lessonId, Long teacherId, LocalDate lessonDate) {}
