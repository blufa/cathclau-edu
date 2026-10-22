package gn.lacaeducation.attendance.application.port.in;
import gn.lacaeducation.attendance.domain.EmployeeAttendance;
import gn.lacaeducation.attendance.domain.EmployeeAttendanceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Présence du personnel non-enseignant (§staff.Employee) : un seul pointage par jour (pas de créneau
 * de cours comme les enseignants), retard/départ anticipé calculés vs l'horaire de référence
 * configuré par l'école (voir AttendanceSettingsUseCase#updateEmployeeHours).
 */
public interface EmployeeAttendanceUseCase {
    record RecordEmployeeAttendanceCommand(Long employeeId, LocalDate date, EmployeeAttendanceStatus status, String reason) {}
    record EmployeeAttendanceStatsResponse(Long employeeId, long totalDays, long presentDays, long absentDays, BigDecimal attendanceRate) {}
    record MonthlySummary(Long employeeId, int year, int month, long presentDays, long absentDays,
                          int totalLateMinutes, int totalEarlyMinutes) {}

    MonthlySummary monthlySummary(Long employeeId, int year, int month);
    EmployeeAttendance record(RecordEmployeeAttendanceCommand command);
    /** Pointage d'entrée (scan) : calcule automatiquement le retard vs l'horaire configuré et upsert
     * la présence du jour. Rejette explicitement un second scan tant qu'aucune sortie valide n'a été
     * pointée (§16), sauf retentative réseau du même tap (fenêtre de course de quelques secondes). */
    EmployeeAttendance recordEntry(Long employeeId);
    /** Pointage de sortie (scan) : renseigne l'heure de sortie réelle du pointage d'entrée déjà
     * enregistré pour aujourd'hui. */
    EmployeeAttendance recordExit(Long employeeId);
    /** Correction manuelle (sans scan) : saisie de l'heure d'entrée pour une date donnée — le retard
     * est calculé automatiquement vs l'horaire configuré, comme pour un scan. Écrase la valeur si un
     * pointage existait déjà pour cette date. */
    EmployeeAttendance recordManualEntry(Long employeeId, LocalDate date, LocalTime checkInTime);
    /** Correction manuelle (sans scan) : saisie de l'heure de sortie pour une date donnée — crée le
     * pointage s'il n'existait pas encore (badge oublié toute la journée). */
    EmployeeAttendance recordManualExit(Long employeeId, LocalDate date, LocalTime checkOutTime);
    /** Marque manuellement le membre du personnel absent aujourd'hui (un scan ne peut jamais produire
     * ce statut). */
    EmployeeAttendance markAbsent(Long employeeId, String reason);
    EmployeeAttendance update(Long id, EmployeeAttendanceStatus status, String reason);
    EmployeeAttendance findById(Long id);
    List<EmployeeAttendance> findByEmployee(Long employeeId, LocalDate from, LocalDate to);
    EmployeeAttendanceStatsResponse getStats(Long employeeId, LocalDate from, LocalDate to);
    void delete(Long id);
}
