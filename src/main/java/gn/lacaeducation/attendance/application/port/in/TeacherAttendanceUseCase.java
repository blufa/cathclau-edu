package gn.lacaeducation.attendance.application.port.in;
import gn.lacaeducation.attendance.domain.TeacherAttendance;
import gn.lacaeducation.attendance.domain.TeacherAttendanceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
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
    /** Pointage par scan : upsert de la présence du jour de l'enseignant. */
    TeacherAttendance recordByScan(Long teacherId, TeacherAttendanceStatus status);
    TeacherAttendance update(Long id, TeacherAttendanceStatus status, String reason);
    TeacherAttendance findById(Long id);
    List<TeacherAttendance> findByTeacher(Long teacherId, LocalDate from, LocalDate to);
    TeacherAttendanceStatsResponse getStats(Long teacherId, LocalDate from, LocalDate to);
    void delete(Long id);
}
