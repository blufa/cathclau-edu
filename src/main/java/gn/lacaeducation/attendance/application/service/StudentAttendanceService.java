package gn.lacaeducation.attendance.application.service;
import gn.lacaeducation.attendance.application.event.StudentAbsentEvent;
import gn.lacaeducation.attendance.application.port.in.StudentAttendanceUseCase;
import gn.lacaeducation.attendance.application.port.out.StudentAttendanceRepository;
import gn.lacaeducation.attendance.domain.AttendanceStatus;
import gn.lacaeducation.attendance.domain.StudentAttendance;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
@Service @Transactional @RequiredArgsConstructor
public class StudentAttendanceService implements StudentAttendanceUseCase {
    private final StudentAttendanceRepository attendanceRepository;
    private final ApplicationEventPublisher eventPublisher;
    @Override
    public StudentAttendance record(RecordAttendanceCommand cmd) {
        StudentAttendance a = attendanceRepository.save(StudentAttendance.builder()
                .studentId(cmd.studentId()).classId(cmd.classId()).date(cmd.date())
                .status(cmd.status()).reason(cmd.reason()).parentNotified(false).build());
        if (cmd.status() == AttendanceStatus.ABSENT) {
            eventPublisher.publishEvent(new StudentAbsentEvent(null, cmd.studentId(), cmd.classId(), cmd.date()));
        }
        return a;
    }
    @Override
    public StudentAttendance update(Long id, AttendanceStatus status, String reason) {
        StudentAttendance a = load(id);
        a.setStatus(status); a.setReason(reason);
        return attendanceRepository.save(a);
    }
    @Override @Transactional(readOnly = true)
    public StudentAttendance findById(Long id) { return load(id); }
    @Override @Transactional(readOnly = true)
    public List<StudentAttendance> findByClass(Long classId, LocalDate date) { return attendanceRepository.findByClassIdAndDate(classId, date); }
    @Override @Transactional(readOnly = true)
    public List<StudentAttendance> findByStudent(Long studentId, LocalDate from, LocalDate to) { return attendanceRepository.findByStudentIdAndDateBetween(studentId, from, to); }
    @Override @Transactional(readOnly = true)
    public List<StudentAttendance> findAbsences(Long classId, LocalDate from, LocalDate to) { return attendanceRepository.findByClassIdAndDateBetweenAndStatus(classId, from, to, AttendanceStatus.ABSENT); }
    @Override public void delete(Long id) { attendanceRepository.deleteById(id); }
    @Override
    public void markParentNotified(Long id) {
        StudentAttendance a = load(id); a.setParentNotified(true); attendanceRepository.save(a);
    }
    private StudentAttendance load(Long id) { return attendanceRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Attendance not found: " + id)); }
}
