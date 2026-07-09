package gn.lacaeducation.attendance.application.port.in;
import gn.lacaeducation.attendance.domain.AttendanceStatus;
import gn.lacaeducation.attendance.domain.StudentAttendance;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
public interface StudentAttendanceUseCase {
    record RecordAttendanceCommand(Long studentId, Long classId, LocalDate date, AttendanceStatus status, String reason) {}
    record AttendanceEntry(Long studentId, AttendanceStatus status, String reason) {}
    record BulkAttendanceCommand(Long classId, LocalDate date, List<AttendanceEntry> attendances) {}
    record AttendanceStatsResponse(Long studentId, long totalDays, long presentDays, long absentDays, long lateDays, BigDecimal attendanceRate) {}
    /** Élève dépassant le seuil d'absences non justifiées sur la période. */
    record UnjustifiedAlert(Long studentId, long unjustifiedCount) {}
    StudentAttendance record(RecordAttendanceCommand command);
    /** Pointage par scan de la carte scolaire : upsert de la présence du jour (classe résolue). */
    StudentAttendance recordByScan(Long studentId, AttendanceStatus status);
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
    /** Liste des absents d'une classe pour une date (PDF). */
    byte[] absenteeListPdf(Long classId, LocalDate date);
    /** Rapport des retards d'une classe sur une période (PDF, agrégé par élève). */
    byte[] latenessReportPdf(Long classId, LocalDate from, LocalDate to);
}
