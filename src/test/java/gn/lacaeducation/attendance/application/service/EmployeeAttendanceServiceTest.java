package gn.lacaeducation.attendance.application.service;

import gn.lacaeducation.attendance.application.port.in.EmployeeAttendanceUseCase.RecordEmployeeAttendanceCommand;
import gn.lacaeducation.attendance.application.port.out.AttendanceSettingsRepository;
import gn.lacaeducation.attendance.application.port.out.EmployeeAttendanceRepository;
import gn.lacaeducation.attendance.domain.AttendanceSettings;
import gn.lacaeducation.attendance.domain.EmployeeAttendance;
import gn.lacaeducation.attendance.domain.EmployeeAttendanceStatus;
import gn.lacaeducation.common.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeAttendanceServiceTest {

    @Mock EmployeeAttendanceRepository repo;
    @Mock AttendanceSettingsRepository settingsRepository;
    @InjectMocks EmployeeAttendanceService service;

    @Test
    void record_persists() {
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        EmployeeAttendance result = service.record(
                new RecordEmployeeAttendanceCommand(5L, LocalDate.of(2026, 1, 15), EmployeeAttendanceStatus.PRESENT, null));

        assertThat(result.getEmployeeId()).isEqualTo(5L);
    }

    @Test
    void recordEntry_noneToday_createsAndComputesLate() {
        when(repo.findByEmployeeIdAndDate(eq(5L), any())).thenReturn(List.of());
        when(settingsRepository.findByTenantId(any())).thenReturn(Optional.of(
                AttendanceSettings.builder().employeeCheckInTime(LocalTime.of(8, 0))
                        .employeeCheckOutTime(LocalTime.of(17, 0)).transitionMarginMinutes(0).build()));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        EmployeeAttendance result = service.recordEntry(5L);

        assertThat(result.getStatus()).isEqualTo(EmployeeAttendanceStatus.PRESENT);
        assertThat(result.getCreatedAt()).isNotNull();
    }

    /** §16 cahier des charges, étendu au personnel non-enseignant (même règle que l'élève/
     * l'enseignant) : un badge déjà scanné à l'entrée (pas de sortie pointée) doit être refusé
     * explicitement — createdAt ancien pour exclure la fenêtre de course réseau. */
    @Test
    void recordEntry_secondScanBeforeExit_rejectedWithClearMessage() {
        EmployeeAttendance existing = EmployeeAttendance.builder().id(9L).employeeId(5L)
                .checkInTime(LocalTime.of(8, 0)).createdAt(LocalDateTime.now().minusMinutes(10)).build();
        when(repo.findByEmployeeIdAndDate(eq(5L), any())).thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.recordEntry(5L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("déjà été scanné");
        verify(repo, never()).save(any());
    }

    @Test
    void recordEntry_raceRetryJustCreated_returnsExistingWithoutRejecting() {
        EmployeeAttendance existing = EmployeeAttendance.builder().id(9L).employeeId(5L)
                .checkInTime(LocalTime.of(8, 0)).createdAt(LocalDateTime.now()).build();
        when(repo.findByEmployeeIdAndDate(eq(5L), any())).thenReturn(List.of(existing));

        EmployeeAttendance result = service.recordEntry(5L);

        assertThat(result).isSameAs(existing);
        assertThat(result.isDuplicate()).isTrue();
        verify(repo, never()).save(any());
    }

    @Test
    void recordEntry_reEntryAfterExitSameDay_createsNewCycle() {
        // §retour terrain : cycles entrée/sortie illimités par jour, même règle que l'élève/
        // l'enseignant — une ré-entrée après une sortie déjà enregistrée le même jour crée un
        // nouveau cycle (nouvelle ligne), jamais un rejet.
        EmployeeAttendance existing = EmployeeAttendance.builder().id(9L).employeeId(5L)
                .checkInTime(LocalTime.of(8, 0)).checkOutTime(LocalTime.of(12, 0))
                .createdAt(LocalDateTime.now().minusHours(2)).build();
        when(repo.findByEmployeeIdAndDate(eq(5L), any())).thenReturn(List.of(existing));
        when(settingsRepository.findByTenantId(any())).thenReturn(Optional.of(
                AttendanceSettings.builder().employeeCheckInTime(LocalTime.of(8, 0))
                        .employeeCheckOutTime(LocalTime.of(17, 0)).transitionMarginMinutes(0).build()));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        EmployeeAttendance result = service.recordEntry(5L);

        assertThat(result).isNotSameAs(existing);
        assertThat(result.isDuplicate()).isFalse();
        verify(repo).save(any());
    }

    @Test
    void recordExit_withoutPriorEntry_throws() {
        when(repo.lockLatestByEmployeeIdAndDate(eq(5L), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.recordExit(5L)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void recordExit_setsCheckOutTimeAndEarlyMinutes() {
        EmployeeAttendance existing = EmployeeAttendance.builder().id(9L).employeeId(5L)
                .checkInTime(LocalTime.of(8, 0)).createdAt(LocalDateTime.now()).build();
        when(repo.lockLatestByEmployeeIdAndDate(eq(5L), any())).thenReturn(Optional.of(existing));
        when(settingsRepository.findByTenantId(any())).thenReturn(Optional.of(
                AttendanceSettings.builder().employeeCheckInTime(LocalTime.of(8, 0))
                        .employeeCheckOutTime(LocalTime.of(17, 0)).build()));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        EmployeeAttendance result = service.recordExit(5L);

        assertThat(result.getCheckOutTime()).isNotNull();
    }

    /** §16 : un second scan de sortie (retentative réseau…) ne doit jamais écraser l'heure de sortie
     * déjà enregistrée — même règle que l'élève/l'enseignant. */
    @Test
    void recordExit_alreadyExited_returnsExistingWithoutOverwriting() {
        EmployeeAttendance existing = EmployeeAttendance.builder().id(9L).employeeId(5L)
                .checkInTime(LocalTime.of(8, 0)).checkOutTime(LocalTime.of(16, 50)).earlyMinutes(10)
                .createdAt(LocalDateTime.now()).build();
        when(repo.lockLatestByEmployeeIdAndDate(eq(5L), any())).thenReturn(Optional.of(existing));

        EmployeeAttendance result = service.recordExit(5L);

        assertThat(result).isSameAs(existing);
        assertThat(result.isDuplicate()).isTrue();
        verify(repo, never()).save(any());
    }

    @Test
    void markAbsent_createsRecordWhenNoneExists() {
        when(repo.findByEmployeeIdAndDate(eq(5L), any())).thenReturn(List.of());
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        EmployeeAttendance result = service.markAbsent(5L, "Grippe");

        assertThat(result.getStatus()).isEqualTo(EmployeeAttendanceStatus.ABSENT);
        assertThat(result.getReason()).isEqualTo("Grippe");
    }

    @Test
    void monthlySummary_twoCyclesSameDay_countsOnlyOnePresentDay() {
        // §retour terrain : un jour avec 2 cycles entrée/sortie (sorti puis revenu) ne doit compter
        // que pour UN jour présent, pas deux — avant ce correctif, le comptage supposait une seule
        // ligne par jour.
        LocalDate day = LocalDate.of(2026, 3, 10);
        EmployeeAttendance cycle1 = EmployeeAttendance.builder().id(1L).employeeId(5L).date(day)
                .status(EmployeeAttendanceStatus.PRESENT).lateMinutes(5).earlyMinutes(0).build();
        EmployeeAttendance cycle2 = EmployeeAttendance.builder().id(2L).employeeId(5L).date(day)
                .status(EmployeeAttendanceStatus.PRESENT).lateMinutes(0).earlyMinutes(3).build();
        when(repo.findByEmployeeIdAndDateBetween(eq(5L), any(), any())).thenReturn(List.of(cycle1, cycle2));

        var summary = service.monthlySummary(5L, 2026, 3);

        assertThat(summary.presentDays()).isEqualTo(1);
        assertThat(summary.absentDays()).isZero();
        assertThat(summary.totalLateMinutes()).isEqualTo(5);
        assertThat(summary.totalEarlyMinutes()).isEqualTo(3);
    }
}
