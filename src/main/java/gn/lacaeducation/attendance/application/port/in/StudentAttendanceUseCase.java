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
    StudentAttendance record(RecordAttendanceCommand command);
    List<StudentAttendance> recordBulk(BulkAttendanceCommand command);
    StudentAttendance update(Long id, AttendanceStatus status, String reason);
    StudentAttendance findById(Long id);
    List<StudentAttendance> findByClass(Long classId, LocalDate date);
    List<StudentAttendance> findByStudent(Long studentId, LocalDate from, LocalDate to);
    List<StudentAttendance> findAbsences(Long classId, LocalDate from, LocalDate to);
    AttendanceStatsResponse getStats(Long studentId, LocalDate from, LocalDate to);
    void delete(Long id);
    void markParentNotified(Long id);
}
