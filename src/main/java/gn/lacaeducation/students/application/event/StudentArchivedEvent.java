package gn.lacaeducation.students.application.event;

/** Published when a student is archived (soft delete, §5.6 correction remontée par le retour
 * comptable). Consumed by billing ({@code StudentArchivedInvoiceListener}) to cancel the student's
 * still-due invoices (UNPAID/PARTIAL, toutes années confondues — l'élève est parti, pas seulement une
 * inscription particulière). Amounts already collected are never touched, only what remains due. */
public record StudentArchivedEvent(Long tenantId, Long studentId) {}
