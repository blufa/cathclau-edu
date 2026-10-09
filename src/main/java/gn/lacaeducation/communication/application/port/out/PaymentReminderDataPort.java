package gn.lacaeducation.communication.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Lecture (mirroir, pas de dépendance Java vers le module {@code billing} — voir
 * {@code students.StudentsInvoiceMirrorEntity} pour le même principe déjà utilisé ailleurs, imposé
 * par la frontière de module Spring Modulith) des factures impayées rattachées à une échéance, pour
 * cibler une relance SMS "retard de paiement" ou "échéance qui approche" (§communication).
 */
public interface PaymentReminderDataPort {

    record DueLine(Long studentId, Long classId, BigDecimal remainingBalance, LocalDate dueDate) {}

    /** Lignes impayées (hors annulées) d'un tenant dont l'échéance liée est déjà passée. */
    List<DueLine> findOverdueLines(Long tenantId, LocalDate today);

    /** Lignes impayées (hors annulées) d'un tenant dont l'échéance liée tombe dans [from, to]. */
    List<DueLine> findUpcomingLines(Long tenantId, LocalDate from, LocalDate to);

    /** Ligne de facture (tous statuts) rattachée à la mensualité (frais MONTHLY) d'une classe — pour
     * calculer, élève par élève, le nombre de tranches déjà payées et le statut de paiement courant
     * (écran "Relances paiement" filtrable par classe). {@code status} : chaîne brute billing
     * ("UNPAID"/"PARTIAL"/"PAID"/"CANCELLED"). */
    record ClassFeeLine(Long studentId, Long installmentId, BigDecimal remainingBalance, String status, LocalDate dueDate) {}

    /** Toutes les lignes de facture de la mensualité d'une classe, tous élèves et tous statuts
     * confondus — agrégées par élève côté appelant. Liste vide si la classe n'a pas de frais MONTHLY. */
    List<ClassFeeLine> findClassMonthlyFeeLines(Long tenantId, Long classId);

    /** Nombre de tranches (échéances) configurées pour la mensualité de cette classe — 0 si aucun
     * frais MONTHLY n'est configuré pour cette classe. */
    int countMonthlyTranches(Long tenantId, Long classId);
}
