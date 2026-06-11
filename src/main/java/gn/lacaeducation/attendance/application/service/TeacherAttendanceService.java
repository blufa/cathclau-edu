package gn.lacaeducation.attendance.application.service;
import gn.lacaeducation.attendance.application.port.in.TeacherAttendanceUseCase;
import gn.lacaeducation.attendance.application.port.out.TeacherAttendanceRepository;
import gn.lacaeducation.attendance.domain.TeacherAttendance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
@Service @Transactional @RequiredArgsConstructor
public class TeacherAttendanceService implements TeacherAttendanceUseCase {
    private final TeacherAttendanceRepository attendanceRepository;
    @Override
    public TeacherAttendance record(RecordTeacherAttendanceCommand cmd) {
        return attendanceRepository.save(TeacherAttendance.builder()
                .teacherId(cmd.teacherId()).date(cmd.date()).status(cmd.status()).reason(cmd.reason()).build());
    }
    @Override @Transactional(readOnly = true)
    public TeacherAttendance findById(Long id) { return attendanceRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Attendance not found: " + id)); }
    @Override @Transactional(readOnly = true)
    public List<TeacherAttendance> findByTeacher(Long teacherId, LocalDate from, LocalDate to) { return attendanceRepository.findByTeacherIdAndDateBetween(teacherId, from, to); }
    @Override public void delete(Long id) { attendanceRepository.deleteById(id); }
}
