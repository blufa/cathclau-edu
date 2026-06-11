package gn.lacaeducation.attendance.application.port.in;
import gn.lacaeducation.attendance.domain.AttendanceStatus;
import gn.lacaeducation.attendance.domain.StudentAttendance;
import java.time.LocalDate;
import java.util.List;
public interface StudentAttendanceUseCase {
    record RecordAttendanceCommand(Long studentId, Long classId, LocalDate date, AttendanceStatus status, String reason) {}
    StudentAttendance record(RecordAttendanceCommand command);
    StudentAttendance update(Long id, AttendanceStatus status, String reason);
    StudentAttendance findById(Long id);
    List<StudentAttendance> findByClass(Long classId, LocalDate date);
    List<StudentAttendance> findByStudent(Long studentId, LocalDate from, LocalDate to);
    List<StudentAttendance> findAbsences(Long classId, LocalDate from, LocalDate to);
    void delete(Long id);
    void markParentNotified(Long id);
}
