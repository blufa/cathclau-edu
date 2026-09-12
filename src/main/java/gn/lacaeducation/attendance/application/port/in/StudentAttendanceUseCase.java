package gn.lacaeducation.attendance.application.port.in;
import gn.lacaeducation.attendance.domain.AttendanceStatus;
import gn.lacaeducation.attendance.domain.ChildPickup;
import gn.lacaeducation.attendance.domain.StudentAttendance;
import gn.lacaeducation.attendance.domain.StudentCheckin;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
public interface StudentAttendanceUseCase {
    record RecordAttendanceCommand(Long studentId, Long classId, LocalDate date, AttendanceStatus status, String reason) {}
    record AttendanceEntry(Long studentId, AttendanceStatus status, String reason) {}
    record BulkAttendanceCommand(Long classId, LocalDate date, List<AttendanceEntry> attendances) {}
    record AttendanceStatsResponse(Long studentId, long totalDays, long presentDays, long absentDays, long lateDays, BigDecimal attendanceRate) {}
    /** Élève dépassant le seuil d'absences non justifiées sur la période. */
    record UnjustifiedAlert(Long studentId, long unjustifiedCount) {}
    StudentAttendance record(RecordAttendanceCommand command);
    /** Pointage d'entrée sur la séance en cours (classe résolue automatiquement) : calcule le retard
     * vs l'heure de début prévue et upsert la présence du jour. {@code scannerUserId} est le compte
     * qui effectue le scan (JWT) — si ce compte est lui-même un élève, il doit être le chef de classe
     * désigné de la classe de l'élève scanné, sinon rejeté ; le personnel n'a pas cette restriction. */
    StudentCheckin recordEntry(Long studentId, Long scannerUserId);
    /** Pointage de sortie sur la séance en cours : renseigne l'heure de sortie réelle du pointage
     * d'entrée déjà enregistré pour cette séance. Mêmes règles d'autorisation que {@link #recordEntry}. */
    StudentCheckin recordExit(Long studentId, Long scannerUserId);
    /** Correction manuelle (sans scan, ex. appel papier) : saisie de l'heure d'entrée pour une date
     * donnée — le retard est calculé automatiquement vs l'emploi du temps, comme pour un scan. Écrase
     * la valeur si un pointage existait déjà pour cette séance. Aucune vérification chef-de-classe
     * (action staff, pas un scan). */
    StudentCheckin recordManualEntry(Long studentId, LocalDate date, LocalTime checkInTime);
    /** Correction manuelle (sans scan) : saisie de l'heure de sortie pour une date donnée — le départ
     * anticipé est calculé automatiquement vs l'emploi du temps, comme pour un scan. Crée le pointage
     * s'il n'existait pas encore (ex. badge oublié toute la séance, saisi a posteriori par le chef de
     * classe ou la direction). */
    StudentCheckin recordManualExit(Long studentId, LocalDate date, LocalTime checkOutTime);
    /** Marque manuellement l'élève absent aujourd'hui (un scan ne peut jamais produire ce statut). */
    StudentAttendance markAbsent(Long studentId, String reason);
    /** Pointages (par séance) d'un élève à une date donnée. */
    List<StudentCheckin> findCheckins(Long studentId, LocalDate date);
    /** Pointages (par séance, tous élèves confondus) d'une classe à une date donnée — pour que le
     * tableau de suivi du personnel affiche l'heure de sortie/départ anticipé comme pour les
     * enseignants/personnel, un élève pouvant avoir plusieurs séances (donc plusieurs sorties) par jour. */
    List<StudentCheckin> findCheckinsByClass(Long classId, LocalDate date);
    /** Enregistre qui est venu récupérer l'enfant (badge de retrait, maternelle/primaire) : un parent
     * déjà lié ({@code pickerParentId}) ou une personne saisie librement ({@code pickerName} +
     * {@code pickerRelation}). */
    ChildPickup recordPickup(Long studentId, Long pickerParentId, String pickerName, String pickerRelation);
    /** Retraits d'un élève à une date donnée (historique/audit). */
    List<ChildPickup> findPickups(Long studentId, LocalDate date);
    /** True si la classe est de cycle maternelle/primaire — déclenche la demande obligatoire du badge
     * de retrait à la sortie. False (jamais d'erreur) si le cycle n'est pas renseigné. */
    boolean requiresPickup(Long classId);
    /** Nom complet d'un élève (pour la confirmation de scan), ou null. */
    String studentName(Long studentId);
    /** Marque une absence comme justifiée/non, avec motif et pièce justificative. */
    StudentAttendance justify(Long id, Boolean justified, String reason, String justificationDoc);
    /** Élèves d'une classe ayant ≥ seuil absences non justifiées sur la période (alerte convocation). */
    List<UnjustifiedAlert> unjustifiedAlerts(Long classId, LocalDate from, LocalDate to, int threshold);
    List<StudentAttendance> recordBulk(BulkAttendanceCommand command);
    StudentAttendance update(Long id, AttendanceStatus status, String reason);
    StudentAttendance findById(Long id);
    List<StudentAttendance> findByClass(Long classId, LocalDate date);
    List<StudentAttendance> findByStudent(Long studentId, LocalDate from, LocalDate to);
    List<StudentAttendance> findAbsences(Long classId, LocalDate from, LocalDate to);
    AttendanceStatsResponse getStats(Long studentId, LocalDate from, LocalDate to);
    void delete(Long id);
    void markParentNotified(Long id);
    /** Redéclenche l'envoi de la notification d'absence/retard (SMS + email, via les mêmes canaux que
     * l'envoi automatique) puis marque la ligne comme traitée — utile si l'envoi automatique a échoué
     * ou n'a pas eu lieu (ex. fournisseur SMS non configuré au moment des faits) : contrairement à
     * {@link #markParentNotified}, qui se contente de cocher la case, ceci déclenche un nouvel essai. */
    void notifyParentNow(Long id);
    /** Liste des absents d'une classe pour une date (PDF). */
    byte[] absenteeListPdf(Long classId, LocalDate date);
    /** Rapport des retards d'une classe sur une période (PDF, agrégé par élève). */
    byte[] latenessReportPdf(Long classId, LocalDate from, LocalDate to);
}
