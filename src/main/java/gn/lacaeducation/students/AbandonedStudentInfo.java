package gn.lacaeducation.students;

/** Élève dont l'inscription est ABANDONED sur une année scolaire (départ en cours d'année) — pour la
 * Vision Fondateur (§14), qui doit pouvoir justifier pourquoi d'anciennes factures impayées ont
 * disparu des statistiques de recouvrement (annulées automatiquement à l'abandon, voir
 * EnrollmentAbandonedInvoiceListener côté billing). */
public record AbandonedStudentInfo(Long studentId, String studentName, Long classId) {}
