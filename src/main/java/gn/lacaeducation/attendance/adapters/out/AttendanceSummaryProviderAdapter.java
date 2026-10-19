package gn.lacaeducation.attendance.adapters.out;

import gn.lacaeducation.attendance.application.port.out.StudentAttendanceRepository;
import gn.lacaeducation.attendance.domain.AttendanceStatus;
import gn.lacaeducation.grading.AttendanceSummaryProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
class AttendanceSummaryProviderAdapter implements AttendanceSummaryProvider {

    private final StudentAttendanceRepository studentAttendanceRepository;

    @Override
    @Transactional(readOnly = true)
    public Summary summary(Long studentId, LocalDate from, LocalDate to) {
        int absentCount = (int) studentAttendanceRepository.countByStudentIdAndDateBetweenAndStatus(
                studentId, from, to, AttendanceStatus.ABSENT);
        int lateCount = (int) studentAttendanceRepository.countByStudentIdAndDateBetweenAndStatus(
                studentId, from, to, AttendanceStatus.LATE);
        return new Summary(absentCount, lateCount);
    }
}
