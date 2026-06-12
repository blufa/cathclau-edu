package gn.lacaeducation.attendance.application.service;
import gn.lacaeducation.attendance.application.event.StudentAbsentEvent;
import gn.lacaeducation.attendance.application.port.in.StudentAttendanceUseCase;
import gn.lacaeducation.attendance.application.port.out.StudentAttendanceRepository;
import gn.lacaeducation.attendance.domain.AttendanceStatus;
import gn.lacaeducation.attendance.domain.StudentAttendance;
import gn.lacaeducation.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
@Service @Transactional @RequiredArgsConstructor
public class StudentAttendanceService implements StudentAttendanceUseCase {
    private final StudentAttendanceRepository attendanceRepository;
    private final ApplicationEventPublisher eventPublisher;
    @Override
    public StudentAttendance record(RecordAttendanceCommand cmd) {
        StudentAttendance a = attendanceRepository.save(StudentAttendance.builder()
                .studentId(cmd.studentId()).classId(cmd.classId()).date(cmd.date())
                .status(cmd.status()).reason(cmd.reason()).parentNotified(false).build());
        if (cmd.status() == AttendanceStatus.ABSENT || cmd.status() == AttendanceStatus.LATE) {
            eventPublisher.publishEvent(new StudentAbsentEvent(null, cmd.studentId(), cmd.classId(), cmd.date()));
        }
        return a;
    }
    @Override
    public List<StudentAttendance> recordBulk(BulkAttendanceCommand cmd) {
        List<StudentAttendance> results = new ArrayList<>();
        for (AttendanceEntry entry : cmd.attendances()) {
            Optional<StudentAttendance> existing = attendanceRepository.findByStudentIdAndClassIdAndDate(
                    entry.studentId(), cmd.classId(), cmd.date());
            StudentAttendance saved;
            if (existing.isPresent()) {
                StudentAttendance a = existing.get();
                a.setStatus(entry.status());
                a.setReason(entry.reason());
                saved = attendanceRepository.save(a);
            } else {
                saved = attendanceRepository.save(StudentAttendance.builder()
                        .studentId(entry.studentId()).classId(cmd.classId()).date(cmd.date())
                        .status(entry.status()).reason(entry.reason()).parentNotified(false).build());
            }
            if (entry.status() == AttendanceStatus.ABSENT || entry.status() == AttendanceStatus.LATE) {
                eventPublisher.publishEvent(new StudentAbsentEvent(null, entry.studentId(), cmd.classId(), cmd.date()));
            }
            results.add(saved);
        }
        return results;
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
    @Override @Transactional(readOnly = true)
    public AttendanceStatsResponse getStats(Long studentId, LocalDate from, LocalDate to) {
        LocalDate effectiveFrom = from != null ? from : LocalDate.now().withDayOfYear(1);
        LocalDate effectiveTo = to != null ? to : LocalDate.now();
        long total = attendanceRepository.countByStudentIdAndDateBetween(studentId, effectiveFrom, effectiveTo);
        long absent = attendanceRepository.countByStudentIdAndDateBetweenAndStatus(studentId, effectiveFrom, effectiveTo, AttendanceStatus.ABSENT);
        long late = attendanceRepository.countByStudentIdAndDateBetweenAndStatus(studentId, effectiveFrom, effectiveTo, AttendanceStatus.LATE);
        long present = total - absent - late;
        BigDecimal rate = total == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(present).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        return new AttendanceStatsResponse(studentId, total, present, absent, late, rate);
    }
    @Override public void delete(Long id) { attendanceRepository.deleteById(id); }
    @Override
    public void markParentNotified(Long id) {
        StudentAttendance a = load(id); a.setParentNotified(true); attendanceRepository.save(a);
    }
    private StudentAttendance load(Long id) { return attendanceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("StudentAttendance", id)); }
}
