package gn.lacaeducation.attendance.application.port.out;
import gn.lacaeducation.attendance.domain.TeacherAttendance;
import gn.lacaeducation.attendance.domain.TeacherAttendanceStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
public interface TeacherAttendanceRepository {
    TeacherAttendance save(TeacherAttendance attendance);
    Optional<TeacherAttendance> findById(Long id);
    List<TeacherAttendance> findByTeacherIdAndDateBetween(Long teacherId, LocalDate from, LocalDate to);
    long countByTeacherIdAndDateBetween(Long teacherId, LocalDate from, LocalDate to);
    long countByTeacherIdAndDateBetweenAndStatus(Long teacherId, LocalDate from, LocalDate to, TeacherAttendanceStatus status);
    void deleteById(Long id);
}
