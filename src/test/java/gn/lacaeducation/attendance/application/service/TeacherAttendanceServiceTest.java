package gn.lacaeducation.attendance.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.academics.ScheduleSlotPublicInfo;
import gn.lacaeducation.attendance.application.event.TeacherExitEvent;
import gn.lacaeducation.attendance.application.port.in.TeacherAttendanceUseCase.LatenessDetailRow;
import gn.lacaeducation.attendance.application.port.in.TeacherAttendanceUseCase.RecordTeacherAttendanceCommand;
import gn.lacaeducation.attendance.application.port.in.TeacherAttendanceUseCase.TeacherAttendanceStatsResponse;
import gn.lacaeducation.attendance.application.port.out.AttendanceReportPdfGenerator;
import gn.lacaeducation.attendance.application.port.out.AttendanceSettingsRepository;
import gn.lacaeducation.attendance.application.port.out.RecessWindowRepository;
import gn.lacaeducation.attendance.application.port.out.TeacherAttendanceRepository;
import gn.lacaeducation.attendance.application.port.out.TeacherCheckinRepository;
import gn.lacaeducation.attendance.domain.TeacherAttendance;
import gn.lacaeducation.attendance.domain.TeacherAttendanceStatus;
import gn.lacaeducation.attendance.domain.TeacherCheckin;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.staff.TeacherPublicInfoProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
class TeacherAttendanceServiceTest {

    @Mock TeacherAttendanceRepository repo;
    @Mock TeacherCheckinRepository checkinRepository;
    @Mock AcademicsDataProvider academics;
    @Mock SessionResolver sessionResolver;
    @Mock AttendanceSettingsRepository settingsRepository;
    @Mock RecessWindowRepository recessWindowRepository;
    @Mock AttendanceReportPdfGenerator pdfGenerator;
    @Mock TeacherPublicInfoProvider teacherPublicInfoProvider;
    @Mock org.springframework.context.ApplicationEventPublisher eventPublisher;
    @InjectMocks TeacherAttendanceService service;

    private static ScheduleSlotPublicInfo slot(String start, String end) {
        return new ScheduleSlotPublicInfo(1L, 5L, 7L, 3L, 2L, "LUNDI", start, end, null);
    }

    private static SessionResolver.Block block(String start, String end) {
        return new SessionResolver.Block(List.of(slot(start, end)));
    }

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

    @Test
    void recordEntry_noScheduleToday_throws() {
        when(academics.findScheduleByTeacher(5L)).thenReturn(List.of());
        when(sessionResolver.currentBlock(any(), any(), any(), anyInt())).thenReturn(null);

        assertThatThrownBy(() -> service.recordEntry(5L)).isInstanceOf(BusinessRuleException.class);
        verify(checkinRepository, never()).save(any());
    }

    @Test
    void recordEntry_afterSessionStart_computesLateMinutes() {
        when(academics.findScheduleByTeacher(5L)).thenReturn(List.of());
        when(sessionResolver.currentBlock(any(), any(), any(), anyInt())).thenReturn(block("00:00", "23:59"));
        when(sessionResolver.graceDeadline(any(), anyInt(), any())).thenReturn(java.time.LocalTime.of(0, 0));
        when(checkinRepository.findByTeacherIdAndDateAndSlot(eq(5L), any(), eq(1L))).thenReturn(Optional.empty());
        when(checkinRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(checkinRepository.findByTeacherIdAndDate(eq(5L), any())).thenReturn(List.of());
        when(repo.findByTeacherIdAndDateBetween(eq(5L), any(), any())).thenReturn(List.of());

        TeacherCheckin result = service.recordEntry(5L);

        assertThat(result.getLateMinutes()).isGreaterThan(0);
        assertThat(result.getScheduleSlotId()).isEqualTo(1L);
    }

    @Test
    void recordEntry_beforeSessionStart_zeroLateAndUpsertsPresent() {
        when(academics.findScheduleByTeacher(5L)).thenReturn(List.of());
        when(sessionResolver.currentBlock(any(), any(), any(), anyInt())).thenReturn(block("23:59", "23:59"));
        when(sessionResolver.graceDeadline(any(), anyInt(), any())).thenReturn(java.time.LocalTime.of(23, 59));
        when(checkinRepository.findByTeacherIdAndDateAndSlot(eq(5L), any(), eq(1L))).thenReturn(Optional.empty());
        when(checkinRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(checkinRepository.findByTeacherIdAndDate(eq(5L), any()))
                .thenReturn(List.of(TeacherCheckin.builder().lateMinutes(0).checkInTime(java.time.LocalTime.of(23, 59)).build()));
        when(repo.findByTeacherIdAndDateBetween(eq(5L), any(), any())).thenReturn(List.of());

        TeacherCheckin result = service.recordEntry(5L);

        assertThat(result.getLateMinutes()).isZero();
        ArgumentCaptor<TeacherAttendance> captor = ArgumentCaptor.forClass(TeacherAttendance.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(TeacherAttendanceStatus.PRESENT);
        assertThat(captor.getValue().getLateMinutes()).isZero();
    }

    @Test
    void recordEntry_idempotent_secondScanSameSessionReturnsExisting() {
        TeacherCheckin existing = TeacherCheckin.builder().id(9L).teacherId(5L).scheduleSlotId(1L).lateMinutes(3).build();
        when(academics.findScheduleByTeacher(5L)).thenReturn(List.of());
        when(sessionResolver.currentBlock(any(), any(), any(), anyInt())).thenReturn(block("00:00", "23:59"));
        when(checkinRepository.findByTeacherIdAndDateAndSlot(eq(5L), any(), eq(1L))).thenReturn(Optional.of(existing));
        when(checkinRepository.findByTeacherIdAndDate(eq(5L), any())).thenReturn(List.of(existing));
        when(repo.findByTeacherIdAndDateBetween(eq(5L), any(), any())).thenReturn(List.of());

        TeacherCheckin result = service.recordEntry(5L);

        assertThat(result).isSameAs(existing);
        verify(checkinRepository, never()).save(any());
    }

    @Test
    void recordExit_withoutPriorEntry_throws() {
        when(academics.findScheduleByTeacher(5L)).thenReturn(List.of());
        when(sessionResolver.currentBlock(any(), any(), any(), anyInt())).thenReturn(block("08:00", "10:00"));
        when(checkinRepository.findByTeacherIdAndDateAndSlot(eq(5L), any(), eq(1L))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.recordExit(5L)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void recordExit_setsCheckOutTimeAndSessionEnd() {
        TeacherCheckin existing = TeacherCheckin.builder().id(9L).teacherId(5L).scheduleSlotId(1L)
                .checkInTime(java.time.LocalTime.of(8, 5)).lateMinutes(5).createdAt(LocalDateTime.now()).build();
        when(academics.findScheduleByTeacher(5L)).thenReturn(List.of());
        when(sessionResolver.currentBlock(any(), any(), any(), anyInt())).thenReturn(block("08:00", "10:00"));
        when(checkinRepository.findByTeacherIdAndDateAndSlot(eq(5L), any(), eq(1L))).thenReturn(Optional.of(existing));
        when(checkinRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        TeacherCheckin result = service.recordExit(5L);

        assertThat(result.getCheckOutTime()).isNotNull();
        assertThat(result.getSessionEnd()).isEqualTo(java.time.LocalTime.of(10, 0));
    }

    // §retour fondateurs : même mécanique côté enseignant, mais sans SMS (voir TeacherExitEvent).

    @Test
    void recordExit_publishesTeacherExitEvent_withMinutesSinceFirstCheckinOfDay() {
        TeacherCheckin existing = TeacherCheckin.builder().id(9L).teacherId(5L).scheduleSlotId(1L)
                .checkInTime(java.time.LocalTime.of(8, 5)).lateMinutes(5).createdAt(LocalDateTime.now()).build();
        when(academics.findScheduleByTeacher(5L)).thenReturn(List.of());
        when(sessionResolver.currentBlock(any(), any(), any(), anyInt())).thenReturn(block("08:00", "10:00"));
        when(checkinRepository.findByTeacherIdAndDateAndSlot(eq(5L), any(), eq(1L))).thenReturn(Optional.of(existing));
        when(checkinRepository.findByTeacherIdAndDate(eq(5L), any())).thenReturn(List.of(existing));
        when(checkinRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.recordExit(5L);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, org.mockito.Mockito.atLeastOnce()).publishEvent(captor.capture());
        TeacherExitEvent event = captor.getAllValues().stream()
                .filter(TeacherExitEvent.class::isInstance).map(TeacherExitEvent.class::cast).findFirst().orElseThrow();
        assertThat(event.checkInTime()).isEqualTo(java.time.LocalTime.of(8, 5));
        assertThat(event.minutesAtSchool()).isGreaterThanOrEqualTo(0);
        assertThat(event.teacherId()).isEqualTo(5L);
    }

    @Test
    void recordManualExit_noEntryEverRecorded_publishesEventWithZeroMinutes_neverThrows() {
        when(academics.findScheduleByTeacher(5L)).thenReturn(List.of());
        when(sessionResolver.currentBlock(any(), any(), any(), anyInt())).thenReturn(block("08:00", "10:00"));
        when(checkinRepository.findByTeacherIdAndDateAndSlot(eq(5L), any(), eq(1L))).thenReturn(Optional.empty());
        when(checkinRepository.findByTeacherIdAndDate(eq(5L), any())).thenReturn(List.of());
        when(checkinRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(repo.findByTeacherIdAndDateBetween(eq(5L), any(), any())).thenReturn(List.of());

        service.recordManualExit(5L, LocalDate.now(), java.time.LocalTime.of(9, 30));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, org.mockito.Mockito.atLeastOnce()).publishEvent(captor.capture());
        TeacherExitEvent event = captor.getAllValues().stream()
                .filter(TeacherExitEvent.class::isInstance).map(TeacherExitEvent.class::cast).findFirst().orElseThrow();
        assertThat(event.checkInTime()).isNull();
        assertThat(event.minutesAtSchool()).isZero();
    }

    @Test
    void recordManualEntry_computesLateAndOverwritesExisting() {
        TeacherCheckin existing = TeacherCheckin.builder().id(9L).teacherId(5L).scheduleSlotId(1L)
                .checkInTime(java.time.LocalTime.of(8, 0)).lateMinutes(0).build();
        when(academics.findScheduleByTeacher(5L)).thenReturn(List.of());
        when(sessionResolver.currentBlock(any(), any(), any(), anyInt())).thenReturn(block("08:00", "10:00"));
        when(sessionResolver.graceDeadline(any(), anyInt(), any())).thenReturn(java.time.LocalTime.of(8, 5));
        when(checkinRepository.findByTeacherIdAndDateAndSlot(eq(5L), any(), eq(1L))).thenReturn(Optional.of(existing));
        when(checkinRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(checkinRepository.findByTeacherIdAndDate(eq(5L), any())).thenReturn(List.of(existing));
        when(repo.findByTeacherIdAndDateBetween(eq(5L), any(), any())).thenReturn(List.of());

        TeacherCheckin result = service.recordManualEntry(5L, LocalDate.of(2026, 8, 26), java.time.LocalTime.of(8, 20));

        assertThat(result.getCheckInTime()).isEqualTo(java.time.LocalTime.of(8, 20));
        // 20 min après le début (8h00) moins la marge de tolérance générale (5 min, §réglages).
        assertThat(result.getLateMinutes()).isEqualTo(15);
    }

    @Test
    void recordManualEntry_withinToleranceMargin_notLate() {
        // Entrée à 8h05 pour un cours à 8h00 avec une marge de tolérance de 5 min : pas de retard —
        // le cas concret signalé : un enseignant pointant à 14h05 pour un cours à 14h00 avec 5 min de
        // marge configurée ne doit jamais être compté en retard.
        when(academics.findScheduleByTeacher(5L)).thenReturn(List.of());
        when(sessionResolver.currentBlock(any(), any(), any(), anyInt())).thenReturn(block("08:00", "10:00"));
        when(sessionResolver.graceDeadline(any(), anyInt(), any())).thenReturn(java.time.LocalTime.of(8, 5));
        when(checkinRepository.findByTeacherIdAndDateAndSlot(eq(5L), any(), eq(1L))).thenReturn(Optional.empty());
        when(checkinRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(checkinRepository.findByTeacherIdAndDate(eq(5L), any())).thenReturn(List.of());
        when(repo.findByTeacherIdAndDateBetween(eq(5L), any(), any())).thenReturn(List.of());

        TeacherCheckin result = service.recordManualEntry(5L, LocalDate.of(2026, 8, 26), java.time.LocalTime.of(8, 5));

        assertThat(result.getLateMinutes()).isZero();
    }

    @Test
    void recordManualEntry_noScheduleThatDay_throws() {
        when(academics.findScheduleByTeacher(5L)).thenReturn(List.of());
        when(sessionResolver.currentBlock(any(), any(), any(), anyInt())).thenReturn(null);

        assertThatThrownBy(() -> service.recordManualEntry(5L, LocalDate.of(2026, 8, 26), java.time.LocalTime.of(8, 20)))
                .isInstanceOf(BusinessRuleException.class);
        verify(checkinRepository, never()).save(any());
    }

    @Test
    void markAbsent_createsRecordWhenNoneExists() {
        when(repo.findByTeacherIdAndDateBetween(eq(5L), any(), any())).thenReturn(List.of());
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        TeacherAttendance result = service.markAbsent(5L, "Grippe");

        assertThat(result.getStatus()).isEqualTo(TeacherAttendanceStatus.ABSENT);
        assertThat(result.getReason()).isEqualTo("Grippe");
    }

    @Test
    void markAbsent_updatesExistingRecord() {
        TeacherAttendance existing = TeacherAttendance.builder().id(1L).status(TeacherAttendanceStatus.PRESENT).build();
        when(repo.findByTeacherIdAndDateBetween(eq(5L), any(), any())).thenReturn(List.of(existing));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        TeacherAttendance result = service.markAbsent(5L, null);

        assertThat(result.getStatus()).isEqualTo(TeacherAttendanceStatus.ABSENT);
        assertThat(result.getReason()).isEqualTo("Absence signalée");
    }

    @Test
    void latenessDetail_keepsOnlySessionsWithLateOrEarlyMinutes_resolvedToClassAndSubject() {
        when(academics.findScheduleByTeacher(5L)).thenReturn(List.of(
                new ScheduleSlotPublicInfo(1L, 20L, 30L, 5L, 2L, "LUNDI", "08:00", "10:00", null),
                new ScheduleSlotPublicInfo(2L, 21L, 31L, 5L, 2L, "MARDI", "08:00", "10:00", null)));
        TeacherCheckin onTime = TeacherCheckin.builder().teacherId(5L).checkinDate(LocalDate.of(2026, 9, 7))
                .scheduleSlotId(1L).lateMinutes(0).earlyMinutes(0).build();
        TeacherCheckin late = TeacherCheckin.builder().teacherId(5L).checkinDate(LocalDate.of(2026, 9, 8))
                .scheduleSlotId(2L).lateMinutes(12).earlyMinutes(0).build();
        when(checkinRepository.findByTeacherIdAndDateBetween(eq(5L), any(), any())).thenReturn(List.of(onTime, late));

        List<LatenessDetailRow> result = service.latenessDetail(5L, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).date()).isEqualTo(LocalDate.of(2026, 9, 8));
        assertThat(result.get(0).classId()).isEqualTo(21L);
        assertThat(result.get(0).subjectId()).isEqualTo(31L);
        assertThat(result.get(0).lateMinutes()).isEqualTo(12);
    }

    @Test
    void latenessReportPdf_combinesLatenessAndAbsenceRowsChronologically_resolvesTeacherName() {
        when(academics.findScheduleByTeacher(5L)).thenReturn(List.of(
                new ScheduleSlotPublicInfo(1L, 20L, 30L, 5L, 2L, "LUNDI", "08:00", "10:00", null)));
        TeacherCheckin late = TeacherCheckin.builder().teacherId(5L).checkinDate(LocalDate.of(2026, 9, 14))
                .scheduleSlotId(1L).lateMinutes(15).earlyMinutes(0).build();
        when(checkinRepository.findByTeacherIdAndDateBetween(eq(5L), any(), any())).thenReturn(List.of(late));
        when(academics.subjectLabels(any())).thenReturn(java.util.Map.of(30L, "Mathématiques"));
        when(academics.findClass(20L)).thenReturn(Optional.empty());
        TeacherAttendance absent = TeacherAttendance.builder().teacherId(5L)
                .date(LocalDate.of(2026, 9, 10)).status(TeacherAttendanceStatus.ABSENT).reason("Grippe").build();
        when(repo.findByTeacherIdAndDateBetween(eq(5L), any(), any())).thenReturn(List.of(absent));
        when(teacherPublicInfoProvider.findTeacher(5L)).thenReturn(Optional.of(
                new TeacherPublicInfoProvider.TeacherInfo(5L, "Aboubacar", "Diallo", "M001", null, null, null)));
        ArgumentCaptor<AttendanceReportPdfGenerator.TeacherLatenessReport> captor =
                ArgumentCaptor.forClass(AttendanceReportPdfGenerator.TeacherLatenessReport.class);
        when(pdfGenerator.teacherLatenessReport(any())).thenReturn(new byte[]{1, 2, 3});

        byte[] pdf = service.latenessReportPdf(5L, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        assertThat(pdf).containsExactly(1, 2, 3);
        verify(pdfGenerator).teacherLatenessReport(captor.capture());
        AttendanceReportPdfGenerator.TeacherLatenessReport report = captor.getValue();
        assertThat(report.teacherName()).isEqualTo("Aboubacar Diallo");
        assertThat(report.rows()).hasSize(2);
        // Chronologique : l'absence du 10 avant le retard du 14.
        assertThat(report.rows().get(0).date()).isEqualTo(LocalDate.of(2026, 9, 10));
        assertThat(report.rows().get(0).absent()).isTrue();
        assertThat(report.rows().get(0).absenceReason()).isEqualTo("Grippe");
        assertThat(report.rows().get(1).date()).isEqualTo(LocalDate.of(2026, 9, 14));
        assertThat(report.rows().get(1).absent()).isFalse();
        assertThat(report.rows().get(1).subjectLabel()).isEqualTo("Mathématiques");
        assertThat(report.rows().get(1).lateMinutes()).isEqualTo(15);
    }
}
