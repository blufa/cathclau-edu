package gn.lacaeducation.attendance.application.port.out;
import gn.lacaeducation.attendance.domain.TeacherAttendance;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
public interface TeacherAttendanceRepository {
    TeacherAttendance save(TeacherAttendance attendance);
    Optional<TeacherAttendance> findById(Long id);
    List<TeacherAttendance> findByTeacherIdAndDateBetween(Long teacherId, LocalDate from, LocalDate to);
    void deleteById(Long id);
}
