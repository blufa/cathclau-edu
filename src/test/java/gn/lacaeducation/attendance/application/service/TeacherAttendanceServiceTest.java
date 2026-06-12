package gn.lacaeducation.attendance.application.service;

import gn.lacaeducation.attendance.application.port.in.TeacherAttendanceUseCase.RecordTeacherAttendanceCommand;
import gn.lacaeducation.attendance.application.port.in.TeacherAttendanceUseCase.TeacherAttendanceStatsResponse;
import gn.lacaeducation.attendance.application.port.out.TeacherAttendanceRepository;
import gn.lacaeducation.attendance.domain.TeacherAttendance;
import gn.lacaeducation.attendance.domain.TeacherAttendanceStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherAttendanceServiceTest {

    @Mock TeacherAttendanceRepository repo;
    @InjectMocks TeacherAttendanceService service;

    @Test
    void record_persists() {
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        TeacherAttendance result = service.record(new RecordTeacherAttendanceCommand(
                5L, LocalDate.of(2026, 1, 15), TeacherAttendanceStatus.PRESENT, null));
        assertThat(result.getTeacherId()).isEqualTo(5L);
    }

    @Test
    void update_changesStatus() {
        TeacherAttendance a = TeacherAttendance.builder().id(1L).status(TeacherAttendanceStatus.PRESENT).build();
        when(repo.findById(1L)).thenReturn(Optional.of(a));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        TeacherAttendance result = service.update(1L, TeacherAttendanceStatus.ABSENT, "leave");

        assertThat(result.getStatus()).isEqualTo(TeacherAttendanceStatus.ABSENT);
        assertThat(result.getReason()).isEqualTo("leave");
    }

    @Test
    void getStats_computesRate() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);
        when(repo.countByTeacherIdAndDateBetween(5L, from, to)).thenReturn(10L);
        when(repo.countByTeacherIdAndDateBetweenAndStatus(eq(5L), eq(from), eq(to), eq(TeacherAttendanceStatus.ABSENT))).thenReturn(2L);

        TeacherAttendanceStatsResponse stats = service.getStats(5L, from, to);

        assertThat(stats.presentDays()).isEqualTo(8L);
        assertThat(stats.absentDays()).isEqualTo(2L);
        assertThat(stats.attendanceRate()).isEqualByComparingTo(BigDecimal.valueOf(80.00));
    }
}
