package gn.lacaeducation.attendance.adapters.in.web;

import gn.lacaeducation.attendance.adapters.in.web.AttendanceScanController.AttendanceOutcome;
import gn.lacaeducation.attendance.adapters.in.web.AttendanceScanController.ScanDirection;
import gn.lacaeducation.attendance.adapters.in.web.AttendanceScanController.ScanRequest;
import gn.lacaeducation.attendance.adapters.in.web.AttendanceScanController.ScanResponse;
import gn.lacaeducation.attendance.adapters.in.web.AttendanceScanController.SubjectType;
import gn.lacaeducation.attendance.application.port.in.StudentAttendanceUseCase;
import gn.lacaeducation.attendance.application.port.in.TeacherAttendanceUseCase;
import gn.lacaeducation.attendance.domain.StudentCheckin;
import gn.lacaeducation.attendance.domain.TeacherCheckin;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.staff.TeacherPublicInfoProvider;
import gn.lacaeducation.staff.TeacherPublicInfoProvider.TeacherInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceScanControllerTest {

    @Mock StudentAttendanceUseCase studentAttendance;
    @Mock TeacherAttendanceUseCase teacherAttendance;
    @Mock TeacherPublicInfoProvider teacherPublicInfoProvider;
    @InjectMocks AttendanceScanController controller;

    @BeforeEach void setTenant() { TenantContext.set(1L); }
    @AfterEach void clearTenant() { TenantContext.clear(); }

    @Test
    void scan_teacherEntry_returnsOutcome() {
        TeacherCheckin checkin = TeacherCheckin.builder().checkInTime(LocalTime.of(8, 10)).lateMinutes(10).build();
        when(teacherAttendance.recordEntry(3L)).thenReturn(checkin);

        ScanResponse res = controller.scan(new ScanRequest(SubjectType.TEACHER, 3L, ScanDirection.ENTRY), null);

        assertThat(res.outcome()).isEqualTo(AttendanceOutcome.LATE);
        assertThat(res.lateMinutes()).isEqualTo(10);
        assertThat(res.checkInTime()).isEqualTo(LocalTime.of(8, 10));
    }

    @Test
    void scan_teacherEntry_resolvesTeacherNameForDisplay() {
        TeacherCheckin checkin = TeacherCheckin.builder().checkInTime(LocalTime.of(8, 0)).lateMinutes(0).build();
        when(teacherAttendance.recordEntry(3L)).thenReturn(checkin);
        when(teacherPublicInfoProvider.findTeacher(3L))
                .thenReturn(Optional.of(new TeacherInfo(3L, "Mamadou", "Diallo", "ENS-3", null, null, null, null)));

        ScanResponse res = controller.scan(new ScanRequest(SubjectType.TEACHER, 3L, ScanDirection.ENTRY), null);

        assertThat(res.name()).isEqualTo("Mamadou Diallo");
    }

    @Test
    void scan_teacherEntry_onTime_returnsPresent() {
        TeacherCheckin checkin = TeacherCheckin.builder().checkInTime(LocalTime.of(8, 0)).lateMinutes(0).build();
        when(teacherAttendance.recordEntry(3L)).thenReturn(checkin);

        ScanResponse res = controller.scan(new ScanRequest(SubjectType.TEACHER, 3L, ScanDirection.ENTRY), null);

        assertThat(res.outcome()).isEqualTo(AttendanceOutcome.PRESENT);
    }

    @Test
    void scan_teacherExit_returnsRealizedMinutes() {
        TeacherCheckin checkin = TeacherCheckin.builder()
                .sessionStart(LocalTime.of(8, 0)).sessionEnd(LocalTime.of(10, 0))
                .checkInTime(LocalTime.of(8, 5)).checkOutTime(LocalTime.of(9, 35)).lateMinutes(5).earlyMinutes(25).build();
        when(teacherAttendance.recordExit(3L)).thenReturn(checkin);

        ScanResponse res = controller.scan(new ScanRequest(SubjectType.TEACHER, 3L, ScanDirection.EXIT), null);

        assertThat(res.checkOutTime()).isEqualTo(LocalTime.of(9, 35));
        assertThat(res.realizedMinutes()).isEqualTo(90);
        assertThat(res.scheduledMinutes()).isEqualTo(120);
        // Départ anticipé transmis tel quel (déjà calculé côté service) — le scan doit pouvoir
        // l'afficher immédiatement, pas seulement une fois consulté sur le tableau de suivi.
        assertThat(res.earlyMinutes()).isEqualTo(25);
        assertThat(res.outcome()).isNull();
    }

    @Test
    void scan_studentEntry_returnsOutcome() {
        StudentCheckin checkin = StudentCheckin.builder().checkInTime(LocalTime.of(8, 0)).lateMinutes(0).build();
        when(studentAttendance.recordEntry(10L, null)).thenReturn(checkin);
        when(studentAttendance.studentName(10L)).thenReturn("Mariama Bah");

        ScanResponse res = controller.scan(new ScanRequest(SubjectType.STUDENT, 10L, ScanDirection.ENTRY), null);

        assertThat(res.outcome()).isEqualTo(AttendanceOutcome.PRESENT);
        assertThat(res.name()).isEqualTo("Mariama Bah");
    }

    @Test
    void scan_studentExit_maternelle_flagsRequiresPickup() {
        StudentCheckin checkin = StudentCheckin.builder().classId(20L)
                .checkInTime(LocalTime.of(8, 0)).checkOutTime(LocalTime.of(12, 0)).lateMinutes(0).build();
        when(studentAttendance.recordExit(10L, null)).thenReturn(checkin);
        when(studentAttendance.requiresPickup(20L)).thenReturn(true);
        when(studentAttendance.studentName(10L)).thenReturn("Mariama Bah");

        ScanResponse res = controller.scan(new ScanRequest(SubjectType.STUDENT, 10L, ScanDirection.EXIT), null);

        assertThat(res.requiresPickupInfo()).isTrue();
    }

    @Test
    void scan_studentExit_secondaire_doesNotRequirePickup() {
        StudentCheckin checkin = StudentCheckin.builder().classId(22L)
                .checkInTime(LocalTime.of(8, 0)).checkOutTime(LocalTime.of(12, 0)).lateMinutes(0).build();
        when(studentAttendance.recordExit(10L, null)).thenReturn(checkin);
        when(studentAttendance.requiresPickup(22L)).thenReturn(false);
        when(studentAttendance.studentName(10L)).thenReturn("Mariama Bah");

        ScanResponse res = controller.scan(new ScanRequest(SubjectType.STUDENT, 10L, ScanDirection.EXIT), null);

        assertThat(res.requiresPickupInfo()).isFalse();
    }

    @Test
    void scan_teacherEntry_neverRequiresPickup() {
        TeacherCheckin checkin = TeacherCheckin.builder().checkInTime(LocalTime.of(8, 0)).lateMinutes(0).build();
        when(teacherAttendance.recordEntry(3L)).thenReturn(checkin);

        ScanResponse res = controller.scan(new ScanRequest(SubjectType.TEACHER, 3L, ScanDirection.ENTRY), null);

        assertThat(res.requiresPickupInfo()).isNull();
    }

    @Test
    void scan_missingDirection_throwsBusinessRuleException() {
        assertThatThrownBy(() -> controller.scan(new ScanRequest(SubjectType.STUDENT, 10L, null), null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void scan_missingSubjectId_throwsBusinessRuleException() {
        assertThatThrownBy(() -> controller.scan(new ScanRequest(SubjectType.STUDENT, null, ScanDirection.ENTRY), null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void scan_noTenantContext_throwsBusinessRuleException() {
        TenantContext.clear();
        assertThatThrownBy(() -> controller.scan(new ScanRequest(SubjectType.TEACHER, 3L, ScanDirection.ENTRY), null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void scan_duplicateInsertRace_retriesAndSucceeds() {
        TeacherCheckin checkin = TeacherCheckin.builder().checkInTime(LocalTime.of(8, 0)).lateMinutes(0).build();
        when(teacherAttendance.recordEntry(3L))
                .thenThrow(new DataIntegrityViolationException("duplicate"))
                .thenReturn(checkin);

        ScanResponse res = controller.scan(new ScanRequest(SubjectType.TEACHER, 3L, ScanDirection.ENTRY), null);

        assertThat(res.outcome()).isEqualTo(AttendanceOutcome.PRESENT);
    }
}
