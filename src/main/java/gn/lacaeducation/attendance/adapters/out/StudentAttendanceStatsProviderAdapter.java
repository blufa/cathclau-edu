package gn.lacaeducation.attendance.adapters.out;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.attendance.StudentAttendanceStatsProvider;
import gn.lacaeducation.attendance.application.port.out.StudentAttendanceRepository;
import gn.lacaeducation.attendance.domain.AttendanceStatus;
import gn.lacaeducation.attendance.domain.StudentAttendance;
import gn.lacaeducation.students.StudentDataProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
class StudentAttendanceStatsProviderAdapter implements StudentAttendanceStatsProvider {

    private final StudentAttendanceRepository studentAttendanceRepository;
    private final AcademicsDataProvider academicsDataProvider;
    private final StudentDataProvider studentDataProvider;

    @Override
    @Transactional(readOnly = true)
    public TodayAttendanceStats todayAttendanceStats(Long tenantId) {
        int expectedCount = academicsDataProvider.findClassesByTenant(tenantId).stream()
                .mapToInt(klass -> studentDataProvider.findStudentsByClass(klass.id()).size())
                .sum();

        List<StudentAttendance> today = studentAttendanceRepository.findByTenantIdAndDate(tenantId, LocalDate.now());
        int presentCount = (int) today.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
        int absentCount = (int) today.stream().filter(a -> a.getStatus() == AttendanceStatus.ABSENT).count();
        int lateCount = (int) today.stream().filter(a -> a.getStatus() == AttendanceStatus.LATE).count();

        BigDecimal presentPercentage = expectedCount == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(presentCount).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(expectedCount), 2, RoundingMode.HALF_UP);

        return new TodayAttendanceStats(expectedCount, presentCount, absentCount, lateCount, presentPercentage);
    }
}
