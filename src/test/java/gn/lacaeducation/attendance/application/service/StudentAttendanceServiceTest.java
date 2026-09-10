package gn.lacaeducation.attendance.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.academics.ClassPublicInfo;
import gn.lacaeducation.academics.ScheduleSlotPublicInfo;
import gn.lacaeducation.attendance.application.event.StudentAbsentEvent;
import gn.lacaeducation.attendance.application.port.in.StudentAttendanceUseCase.RecordAttendanceCommand;
import gn.lacaeducation.attendance.application.port.out.AttendanceSettingsRepository;
import gn.lacaeducation.attendance.application.port.out.ChildPickupRepository;
import gn.lacaeducation.attendance.application.port.out.RecessWindowRepository;
import gn.lacaeducation.attendance.application.port.out.StudentAttendanceRepository;
import gn.lacaeducation.attendance.application.port.out.StudentCheckinRepository;
import gn.lacaeducation.attendance.domain.AttendanceStatus;
import gn.lacaeducation.attendance.domain.ChildPickup;
import gn.lacaeducation.attendance.domain.StudentAttendance;
import gn.lacaeducation.attendance.domain.StudentCheckin;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ConflictException;
import gn.lacaeducation.students.ParentPublicInfo;
import gn.lacaeducation.students.StudentDataProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentAttendanceServiceTest {

    @Mock StudentAttendanceRepository repo;
    @Mock StudentCheckinRepository checkinRepository;
    @Mock ChildPickupRepository childPickupRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock StudentDataProvider studentDataProvider;
    @Mock AcademicsDataProvider academicsProvider;
    @Mock SessionResolver sessionResolver;
    @Mock AttendanceSettingsRepository settingsRepository;
    @Mock RecessWindowRepository recessWindowRepository;
    @InjectMocks StudentAttendanceService service;

    private static ScheduleSlotPublicInfo slot(String start, String end) {
        return new ScheduleSlotPublicInfo(1L, 20L, 7L, 3L, 2L, "LUNDI", start, end, null);
    }

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

    @Test
    void recordEntry_noScheduleToday_throws() {
        when(studentDataProvider.findCurrentClassId(10L)).thenReturn(Optional.of(20L));
        when(academicsProvider.findScheduleByClass(20L)).thenReturn(List.of());
        when(sessionResolver.currentSession(any(), any(), any())).thenReturn(null);

        assertThatThrownBy(() -> service.recordEntry(10L, null)).isInstanceOf(BusinessRuleException.class);
        verify(checkinRepository, never()).save(any());
    }

    @Test
    void recordEntry_noCurrentClass_throws() {
        when(studentDataProvider.findCurrentClassId(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.recordEntry(10L, null)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void recordEntry_afterSessionStart_computesLateMinutes() {
        when(studentDataProvider.findCurrentClassId(10L)).thenReturn(Optional.of(20L));
        when(academicsProvider.findScheduleByClass(20L)).thenReturn(List.of());
        when(sessionResolver.currentSession(any(), any(), any())).thenReturn(slot("00:00", "23:59"));
        when(sessionResolver.graceDeadline(any(), anyInt(), any())).thenReturn(java.time.LocalTime.of(0, 0));
        when(checkinRepository.findByStudentIdAndDateAndSlot(eq(10L), any(), eq(1L))).thenReturn(Optional.empty());
        when(checkinRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(checkinRepository.findByStudentIdAndDate(eq(10L), any())).thenReturn(List.of());
        when(repo.findByStudentIdAndClassIdAndDate(eq(10L), eq(20L), any())).thenReturn(Optional.empty());

        StudentCheckin result = service.recordEntry(10L, null);

        assertThat(result.getLateMinutes()).isGreaterThan(0);
        assertThat(result.getClassId()).isEqualTo(20L);
    }

    @Test
    void recordEntry_idempotent_secondScanSameSessionReturnsExisting() {
        StudentCheckin existing = StudentCheckin.builder().id(9L).studentId(10L).classId(20L).scheduleSlotId(1L).lateMinutes(3).build();
        when(studentDataProvider.findCurrentClassId(10L)).thenReturn(Optional.of(20L));
        when(academicsProvider.findScheduleByClass(20L)).thenReturn(List.of());
        when(sessionResolver.currentSession(any(), any(), any())).thenReturn(slot("00:00", "23:59"));
        when(checkinRepository.findByStudentIdAndDateAndSlot(eq(10L), any(), eq(1L))).thenReturn(Optional.of(existing));
        when(checkinRepository.findByStudentIdAndDate(eq(10L), any())).thenReturn(List.of(existing));
        when(repo.findByStudentIdAndClassIdAndDate(eq(10L), eq(20L), any())).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(i -> { StudentAttendance a = i.getArgument(0); a.setId(7L); return a; });

        StudentCheckin result = service.recordEntry(10L, null);

        assertThat(result).isSameAs(existing);
        verify(checkinRepository, never()).save(any());
    }

    @Test
    void recordEntry_scannerIsWrongStudent_throws() {
        when(studentDataProvider.findCurrentClassId(10L)).thenReturn(Optional.of(20L));
        when(studentDataProvider.findStudentIdByUserId(99L)).thenReturn(Optional.of(11L));
        when(academicsProvider.findClass(20L)).thenReturn(Optional.of(
                new ClassPublicInfo(20L, "6e A", null, "NORMALE", 12L, null, null)));

        assertThatThrownBy(() -> service.recordEntry(10L, 99L)).isInstanceOf(BusinessRuleException.class);
        verify(checkinRepository, never()).save(any());
    }

    @Test
    void recordEntry_scannerIsClassRepresentative_allowed() {
        when(studentDataProvider.findCurrentClassId(10L)).thenReturn(Optional.of(20L));
        when(studentDataProvider.findStudentIdByUserId(99L)).thenReturn(Optional.of(12L));
        when(academicsProvider.findClass(20L)).thenReturn(Optional.of(
                new ClassPublicInfo(20L, "6e A", null, "NORMALE", 12L, null, null)));
        when(academicsProvider.findScheduleByClass(20L)).thenReturn(List.of());
        when(sessionResolver.currentSession(any(), any(), any())).thenReturn(slot("00:00", "23:59"));
        when(sessionResolver.graceDeadline(any(), anyInt(), any())).thenReturn(java.time.LocalTime.of(0, 0));
        when(checkinRepository.findByStudentIdAndDateAndSlot(eq(10L), any(), eq(1L))).thenReturn(Optional.empty());
        when(checkinRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(checkinRepository.findByStudentIdAndDate(eq(10L), any())).thenReturn(List.of());
        when(repo.findByStudentIdAndClassIdAndDate(eq(10L), eq(20L), any())).thenReturn(Optional.empty());

        StudentCheckin result = service.recordEntry(10L, 99L);

        assertThat(result).isNotNull();
    }

    @Test
    void recordExit_withoutPriorEntry_throws() {
        when(studentDataProvider.findCurrentClassId(10L)).thenReturn(Optional.of(20L));
        when(academicsProvider.findScheduleByClass(20L)).thenReturn(List.of());
        when(sessionResolver.currentSession(any(), any(), any())).thenReturn(slot("08:00", "10:00"));
        when(checkinRepository.findByStudentIdAndDateAndSlot(eq(10L), any(), eq(1L))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.recordExit(10L, null)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void recordExit_setsCheckOutTimeAndSessionEnd() {
        StudentCheckin existing = StudentCheckin.builder().id(9L).studentId(10L).classId(20L).scheduleSlotId(1L)
                .checkInTime(java.time.LocalTime.of(8, 5)).lateMinutes(5).build();
        when(studentDataProvider.findCurrentClassId(10L)).thenReturn(Optional.of(20L));
        when(academicsProvider.findScheduleByClass(20L)).thenReturn(List.of());
        when(sessionResolver.currentSession(any(), any(), any())).thenReturn(slot("08:00", "10:00"));
        when(checkinRepository.findByStudentIdAndDateAndSlot(eq(10L), any(), eq(1L))).thenReturn(Optional.of(existing));
        when(checkinRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        StudentCheckin result = service.recordExit(10L, null);

        assertThat(result.getCheckOutTime()).isNotNull();
        assertThat(result.getSessionEnd()).isEqualTo(java.time.LocalTime.of(10, 0));
    }

    @Test
    void recordManualEntry_computesLateAndOverwritesExisting() {
        StudentCheckin existing = StudentCheckin.builder().id(9L).studentId(10L).classId(20L).scheduleSlotId(1L)
                .checkInTime(java.time.LocalTime.of(8, 0)).lateMinutes(0).build();
        when(studentDataProvider.findCurrentClassId(10L)).thenReturn(Optional.of(20L));
        when(academicsProvider.findScheduleByClass(20L)).thenReturn(List.of());
        when(sessionResolver.currentSession(any(), any(), any())).thenReturn(slot("08:00", "10:00"));
        when(sessionResolver.graceDeadline(any(), anyInt(), any())).thenReturn(java.time.LocalTime.of(8, 5));
        when(checkinRepository.findByStudentIdAndDateAndSlot(eq(10L), any(), eq(1L))).thenReturn(Optional.of(existing));
        when(checkinRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(checkinRepository.findByStudentIdAndDate(eq(10L), any())).thenReturn(List.of(existing));
        when(repo.findByStudentIdAndClassIdAndDate(eq(10L), eq(20L), any())).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(i -> { StudentAttendance a = i.getArgument(0); a.setId(7L); return a; });

        StudentCheckin result = service.recordManualEntry(10L, LocalDate.of(2026, 8, 26), java.time.LocalTime.of(8, 20));

        assertThat(result.getCheckInTime()).isEqualTo(java.time.LocalTime.of(8, 20));
        // 20 min après le début (8h00) moins la marge de tolérance générale (5 min, §réglages).
        assertThat(result.getLateMinutes()).isEqualTo(15);
    }

    @Test
    void recordManualEntry_withinToleranceMargin_notLate() {
        // Entrée à 8h05 pour un cours à 8h00 avec la marge de tolérance de 5 min : pas de retard.
        when(studentDataProvider.findCurrentClassId(10L)).thenReturn(Optional.of(20L));
        when(academicsProvider.findScheduleByClass(20L)).thenReturn(List.of());
        when(sessionResolver.currentSession(any(), any(), any())).thenReturn(slot("08:00", "10:00"));
        when(sessionResolver.graceDeadline(any(), anyInt(), any())).thenReturn(java.time.LocalTime.of(8, 5));
        when(checkinRepository.findByStudentIdAndDateAndSlot(eq(10L), any(), eq(1L))).thenReturn(Optional.empty());
        when(checkinRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(checkinRepository.findByStudentIdAndDate(eq(10L), any())).thenReturn(List.of());
        when(repo.findByStudentIdAndClassIdAndDate(eq(10L), eq(20L), any())).thenReturn(Optional.empty());

        StudentCheckin result = service.recordManualEntry(10L, LocalDate.of(2026, 8, 26), java.time.LocalTime.of(8, 5));

        assertThat(result.getLateMinutes()).isZero();
    }

    @Test
    void recordManualEntry_noScheduleThatDay_throws() {
        when(studentDataProvider.findCurrentClassId(10L)).thenReturn(Optional.of(20L));
        when(academicsProvider.findScheduleByClass(20L)).thenReturn(List.of());
        when(sessionResolver.currentSession(any(), any(), any())).thenReturn(null);

        assertThatThrownBy(() -> service.recordManualEntry(10L, LocalDate.of(2026, 8, 26), java.time.LocalTime.of(8, 20)))
                .isInstanceOf(BusinessRuleException.class);
        verify(checkinRepository, never()).save(any());
    }

    @Test
    void markAbsent_createsRecordAndPublishesEvent() {
        when(studentDataProvider.findCurrentClassId(10L)).thenReturn(Optional.of(20L));
        when(repo.findByStudentIdAndClassIdAndDate(eq(10L), eq(20L), any())).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(i -> { StudentAttendance a = i.getArgument(0); a.setId(7L); return a; });

        StudentAttendance result = service.markAbsent(10L, "Rendez-vous médical");

        assertThat(result.getStatus()).isEqualTo(AttendanceStatus.ABSENT);
        assertThat(result.getReason()).isEqualTo("Rendez-vous médical");
        verify(eventPublisher).publishEvent(any(StudentAbsentEvent.class));
    }

    @Test
    void markAbsent_updatesExistingRecord() {
        StudentAttendance existing = StudentAttendance.builder().id(7L).studentId(10L).classId(20L).status(AttendanceStatus.PRESENT).build();
        when(studentDataProvider.findCurrentClassId(10L)).thenReturn(Optional.of(20L));
        when(repo.findByStudentIdAndClassIdAndDate(eq(10L), eq(20L), any())).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        StudentAttendance result = service.markAbsent(10L, null);

        assertThat(result.getStatus()).isEqualTo(AttendanceStatus.ABSENT);
        assertThat(result.getReason()).isEqualTo("Absence signalée");
    }

    @Test
    void recordPickup_existingParent_usesParentName() {
        when(studentDataProvider.findCurrentClassId(10L)).thenReturn(Optional.of(20L));
        when(studentDataProvider.findParentsByStudent(10L)).thenReturn(List.of(
                new ParentPublicInfo(5L, "Mariama", "Bah", null, null)));
        when(childPickupRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ChildPickup result = service.recordPickup(10L, 5L, null, "Mère");

        assertThat(result.getPickerName()).isEqualTo("Mariama Bah");
        assertThat(result.getPickerParentId()).isEqualTo(5L);
        assertThat(result.getClassId()).isEqualTo(20L);
    }

    @Test
    void recordPickup_freeText_usesGivenName() {
        when(studentDataProvider.findCurrentClassId(10L)).thenReturn(Optional.of(20L));
        when(childPickupRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ChildPickup result = service.recordPickup(10L, null, "Oncle Alpha", "Oncle");

        assertThat(result.getPickerName()).isEqualTo("Oncle Alpha");
        assertThat(result.getPickerParentId()).isNull();
        verify(studentDataProvider, never()).findParentsByStudent(any());
    }

    @Test
    void recordPickup_noNameNoParent_throws() {
        when(studentDataProvider.findCurrentClassId(10L)).thenReturn(Optional.of(20L));

        assertThatThrownBy(() -> service.recordPickup(10L, null, null, null))
                .isInstanceOf(BusinessRuleException.class);
        verify(childPickupRepository, never()).save(any());
    }

    @Test
    void recordPickup_parentNotLinkedToStudent_throws() {
        when(studentDataProvider.findCurrentClassId(10L)).thenReturn(Optional.of(20L));
        when(studentDataProvider.findParentsByStudent(10L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.recordPickup(10L, 5L, null, null))
                .isInstanceOf(BusinessRuleException.class);
        verify(childPickupRepository, never()).save(any());
    }

    @Test
    void requiresPickup_trueForMaternelleAndPrimaire() {
        when(academicsProvider.findClass(20L)).thenReturn(Optional.of(
                new ClassPublicInfo(20L, "Maternelle A", null, "NORMALE", null, null, "MATERNELLE")));
        assertThat(service.requiresPickup(20L)).isTrue();

        when(academicsProvider.findClass(21L)).thenReturn(Optional.of(
                new ClassPublicInfo(21L, "CP1", null, "NORMALE", null, null, "PRIMAIRE")));
        assertThat(service.requiresPickup(21L)).isTrue();
    }

    @Test
    void requiresPickup_falseForSecondaryOrUnset() {
        when(academicsProvider.findClass(22L)).thenReturn(Optional.of(
                new ClassPublicInfo(22L, "6e A", null, "NORMALE", null, null, "COLLEGE")));
        assertThat(service.requiresPickup(22L)).isFalse();

        when(academicsProvider.findClass(23L)).thenReturn(Optional.empty());
        assertThat(service.requiresPickup(23L)).isFalse();
    }
}
