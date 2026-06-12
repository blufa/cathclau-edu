package gn.lacaeducation.attendance.application.port.out;
import gn.lacaeducation.attendance.domain.AttendanceStatus;
import gn.lacaeducation.attendance.domain.StudentAttendance;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
public interface StudentAttendanceRepository {
    StudentAttendance save(StudentAttendance attendance);
    Optional<StudentAttendance> findById(Long id);
    Optional<StudentAttendance> findByStudentIdAndClassIdAndDate(Long studentId, Long classId, LocalDate date);
    List<StudentAttendance> findByClassIdAndDate(Long classId, LocalDate date);
    List<StudentAttendance> findByStudentIdAndDateBetween(Long studentId, LocalDate from, LocalDate to);
    List<StudentAttendance> findByClassIdAndDateBetweenAndStatus(Long classId, LocalDate from, LocalDate to, AttendanceStatus status);
    long countByStudentIdAndDateBetweenAndStatus(Long studentId, LocalDate from, LocalDate to, AttendanceStatus status);
    long countByStudentIdAndDateBetween(Long studentId, LocalDate from, LocalDate to);
    void deleteById(Long id);
}
