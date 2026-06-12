package gn.lacaeducation.attendance.application.service;

import gn.lacaeducation.attendance.application.event.StudentAbsentEvent;
import gn.lacaeducation.attendance.application.port.in.StudentAttendanceUseCase.RecordAttendanceCommand;
import gn.lacaeducation.attendance.application.port.out.StudentAttendanceRepository;
import gn.lacaeducation.attendance.domain.AttendanceStatus;
import gn.lacaeducation.attendance.domain.StudentAttendance;
import gn.lacaeducation.common.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentAttendanceServiceTest {

    @Mock StudentAttendanceRepository repo;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks StudentAttendanceService service;

    private RecordAttendanceCommand cmd(AttendanceStatus status) {
        return new RecordAttendanceCommand(10L, 20L, LocalDate.of(2026, 1, 15), status, "sick");
    }

    @Test
    void record_duplicate_throwsConflict() {
        when(repo.findByStudentIdAndClassIdAndDate(10L, 20L, LocalDate.of(2026, 1, 15)))
                .thenReturn(Optional.of(StudentAttendance.builder().id(1L).build()));

        assertThatThrownBy(() -> service.record(cmd(AttendanceStatus.PRESENT)))
                .isInstanceOf(ConflictException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void record_absent_publishesEvent() {
        when(repo.findByStudentIdAndClassIdAndDate(any(), any(), any())).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(i -> { StudentAttendance a = i.getArgument(0); a.setId(7L); return a; });

        service.record(cmd(AttendanceStatus.ABSENT));

        verify(eventPublisher).publishEvent(any(StudentAbsentEvent.class));
    }

    @Test
    void record_present_doesNotPublishEvent() {
        when(repo.findByStudentIdAndClassIdAndDate(any(), any(), any())).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(i -> { StudentAttendance a = i.getArgument(0); a.setId(7L); return a; });

        service.record(cmd(AttendanceStatus.PRESENT));

        verify(eventPublisher, never()).publishEvent(any(StudentAbsentEvent.class));
    }

    @Test
    void markParentNotified_setsFlag() {
        StudentAttendance a = StudentAttendance.builder().id(7L).parentNotified(false).build();
        when(repo.findById(7L)).thenReturn(Optional.of(a));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.markParentNotified(7L);

        org.assertj.core.api.Assertions.assertThat(a.getParentNotified()).isTrue();
    }
}
