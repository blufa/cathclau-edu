package gn.lacaeducation.attendance.adapters.out;

import gn.lacaeducation.attendance.TeacherWorkProvider;
import gn.lacaeducation.attendance.application.port.in.TeacherAttendanceUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class TeacherWorkProviderAdapter implements TeacherWorkProvider {

    private final TeacherAttendanceUseCase useCase;

    @Override
    public MonthlyWork monthlyWork(Long teacherId, int year, int month) {
        TeacherAttendanceUseCase.MonthlySummary s = useCase.monthlySummary(teacherId, year, month);
        return new MonthlyWork(s.dueMinutes(), s.totalLateMinutes(), s.totalEarlyMinutes(), s.absentMinutes(),
                s.effectiveMinutes(), s.presentDays(), s.absentDays());
    }
}
