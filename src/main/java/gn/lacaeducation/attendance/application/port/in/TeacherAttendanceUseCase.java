package gn.lacaeducation.attendance.application.port.in;
import gn.lacaeducation.attendance.domain.TeacherAttendance;
import gn.lacaeducation.attendance.domain.TeacherAttendanceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
public interface TeacherAttendanceUseCase {
    record RecordTeacherAttendanceCommand(Long teacherId, LocalDate date, TeacherAttendanceStatus status, String reason) {}
    record TeacherAttendanceStatsResponse(Long teacherId, long totalDays, long presentDays, long absentDays, BigDecimal attendanceRate) {}
    TeacherAttendance record(RecordTeacherAttendanceCommand command);
    TeacherAttendance findById(Long id);
    List<TeacherAttendance> findByTeacher(Long teacherId, LocalDate from, LocalDate to);
    TeacherAttendanceStatsResponse getStats(Long teacherId, LocalDate from, LocalDate to);
    void delete(Long id);
}
