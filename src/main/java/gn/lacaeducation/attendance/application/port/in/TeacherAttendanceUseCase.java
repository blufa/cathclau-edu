package gn.lacaeducation.attendance.application.port.in;
import gn.lacaeducation.attendance.domain.TeacherAttendance;
import gn.lacaeducation.attendance.domain.TeacherAttendanceStatus;
import gn.lacaeducation.attendance.domain.TeacherCheckin;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
public interface TeacherAttendanceUseCase {
    record RecordTeacherAttendanceCommand(Long teacherId, LocalDate date, TeacherAttendanceStatus status, String reason) {}
    record TeacherAttendanceStatsResponse(Long teacherId, long totalDays, long presentDays, long absentDays, BigDecimal attendanceRate) {}
    /**
     * Récapitulatif mensuel. dueMinutes = heures dues du mois (emploi du temps × occurrences) ;
     * absentMinutes = heures perdues pour absence ; effectiveMinutes = dues − absences − retard cumulé.
     */
    record MonthlySummary(Long teacherId, int year, int month, long presentDays, long absentDays,
                          int totalLateMinutes, int dueMinutes, int absentMinutes, int effectiveMinutes) {}
    MonthlySummary monthlySummary(Long teacherId, int year, int month);
    TeacherAttendance record(RecordTeacherAttendanceCommand command);
    /** Pointage d'entrée sur la séance en cours : calcule automatiquement le retard vs l'heure de
     * début prévue et upsert la présence du jour. */
    TeacherCheckin recordEntry(Long teacherId);
    /** Pointage de sortie sur la séance en cours : renseigne l'heure de sortie réelle du pointage
     * d'entrée déjà enregistré pour cette séance. */
    TeacherCheckin recordExit(Long teacherId);
    /** Correction manuelle (sans scan) : saisie de l'heure d'entrée pour une date donnée — le retard
     * est calculé automatiquement vs l'emploi du temps, comme pour un scan. Écrase la valeur si un
     * pointage existait déjà pour cette séance (contrairement au scan, idempotent). */
    TeacherCheckin recordManualEntry(Long teacherId, LocalDate date, LocalTime checkInTime);
    /** Marque manuellement l'enseignant absent aujourd'hui (un scan ne peut jamais produire ce statut). */
    TeacherAttendance markAbsent(Long teacherId, String reason);
    TeacherAttendance update(Long id, TeacherAttendanceStatus status, String reason);
    TeacherAttendance findById(Long id);
    List<TeacherAttendance> findByTeacher(Long teacherId, LocalDate from, LocalDate to);
    TeacherAttendanceStatsResponse getStats(Long teacherId, LocalDate from, LocalDate to);
    /** Pointages (par séance) d'un enseignant à une date donnée — heure d'arrivée et retard par créneau,
     * pour retrouver à quelle heure il a pointé pour une séance précise (ex. cahier de textes). */
    List<TeacherCheckin> findCheckins(Long teacherId, LocalDate date);
    void delete(Long id);
}
