package gn.lacaeducation.attendance.application.service;

import gn.lacaeducation.attendance.application.event.UndeclaredAttendanceEvent;
import gn.lacaeducation.attendance.application.port.out.EnrollmentQueryPort;
import gn.lacaeducation.attendance.application.port.out.StudentAttendanceRepository;
import gn.lacaeducation.attendance.domain.AttendanceStatus;
import gn.lacaeducation.attendance.domain.StudentAttendance;
import gn.lacaeducation.platform.TenantListPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Détecteur B1 (§retour fondateurs, contrôle anti-fraude comptable). */
@ExtendWith(MockitoExtension.class)
class UndeclaredEnrollmentDetectionSchedulerTest {

    @Mock TenantListPort tenantListPort;
    @Mock StudentAttendanceRepository studentAttendanceRepository;
    @Mock EnrollmentQueryPort enrollmentQueryPort;
    @Mock org.springframework.context.ApplicationEventPublisher eventPublisher;
    @InjectMocks UndeclaredEnrollmentDetectionScheduler scheduler;

    @Test
    void detectUndeclaredAttendance_presentWithoutActiveEnrollment_publishesEvent() {
        when(tenantListPort.listActiveTenantIds()).thenReturn(List.of(new TenantListPort.TenantRef(10L)));
        StudentAttendance present = StudentAttendance.builder().studentId(50L).status(AttendanceStatus.PRESENT).build();
        when(studentAttendanceRepository.findByTenantIdAndDate(eq(10L), any())).thenReturn(List.of(present));
        when(enrollmentQueryPort.hasActiveEnrollment(50L)).thenReturn(false);

        scheduler.detectUndeclaredAttendance();

        ArgumentCaptor<UndeclaredAttendanceEvent> captor = ArgumentCaptor.forClass(UndeclaredAttendanceEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().tenantId()).isEqualTo(10L);
        assertThat(captor.getValue().studentId()).isEqualTo(50L);
        assertThat(captor.getValue().date()).isEqualTo(LocalDate.now());
    }

    @Test
    void detectUndeclaredAttendance_presentWithActiveEnrollment_noEvent() {
        when(tenantListPort.listActiveTenantIds()).thenReturn(List.of(new TenantListPort.TenantRef(10L)));
        StudentAttendance present = StudentAttendance.builder().studentId(50L).status(AttendanceStatus.PRESENT).build();
        when(studentAttendanceRepository.findByTenantIdAndDate(eq(10L), any())).thenReturn(List.of(present));
        when(enrollmentQueryPort.hasActiveEnrollment(50L)).thenReturn(true);

        scheduler.detectUndeclaredAttendance();

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void detectUndeclaredAttendance_absentStudent_neverChecked() {
        when(tenantListPort.listActiveTenantIds()).thenReturn(List.of(new TenantListPort.TenantRef(10L)));
        StudentAttendance absent = StudentAttendance.builder().studentId(50L).status(AttendanceStatus.ABSENT).build();
        when(studentAttendanceRepository.findByTenantIdAndDate(eq(10L), any())).thenReturn(List.of(absent));

        scheduler.detectUndeclaredAttendance();

        verify(enrollmentQueryPort, never()).hasActiveEnrollment(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void detectUndeclaredAttendance_oneTenantFails_othersStillProcessed() {
        when(tenantListPort.listActiveTenantIds())
                .thenReturn(List.of(new TenantListPort.TenantRef(10L), new TenantListPort.TenantRef(20L)));
        when(studentAttendanceRepository.findByTenantIdAndDate(eq(10L), any()))
                .thenThrow(new RuntimeException("boom"));
        StudentAttendance present = StudentAttendance.builder().studentId(60L).status(AttendanceStatus.LATE).build();
        when(studentAttendanceRepository.findByTenantIdAndDate(eq(20L), any())).thenReturn(List.of(present));
        when(enrollmentQueryPort.hasActiveEnrollment(60L)).thenReturn(false);

        scheduler.detectUndeclaredAttendance();

        ArgumentCaptor<UndeclaredAttendanceEvent> captor = ArgumentCaptor.forClass(UndeclaredAttendanceEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().tenantId()).isEqualTo(20L);
    }
}
