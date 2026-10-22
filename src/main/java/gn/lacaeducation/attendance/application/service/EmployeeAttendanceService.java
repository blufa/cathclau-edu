package gn.lacaeducation.attendance.application.service;

import gn.lacaeducation.attendance.application.port.in.EmployeeAttendanceUseCase;
import gn.lacaeducation.attendance.application.port.out.AttendanceSettingsRepository;
import gn.lacaeducation.attendance.application.port.out.EmployeeAttendanceRepository;
import gn.lacaeducation.attendance.domain.AttendanceSettings;
import gn.lacaeducation.attendance.domain.EmployeeAttendance;
import gn.lacaeducation.attendance.domain.EmployeeAttendanceStatus;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.common.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Présence du personnel non-enseignant (§staff.Employee) : un seul pointage par jour, retard/départ
 * anticipé calculés vs l'horaire de référence configuré par l'école (pas de créneau de cours par
 * individu, contrairement aux enseignants).
 */
@Service @Transactional @RequiredArgsConstructor
public class EmployeeAttendanceService implements EmployeeAttendanceUseCase {

    private final EmployeeAttendanceRepository attendanceRepository;
    private final AttendanceSettingsRepository settingsRepository;

    private AttendanceSettings settings(Long tenantId) {
        return settingsRepository.findByTenantId(tenantId).orElse(AttendanceSettings.defaults());
    }

    /** Fenêtre de course réseau (§16) : même mécanisme que
     * {@code StudentAttendanceService#createdWithinRaceWindow} — une ligne créée il y a moins de
     * {@code RACE_WINDOW_SECONDS} vient très probablement de la retentative du MÊME tap physique,
     * pas d'un second scan humain. */
    private static final long RACE_WINDOW_SECONDS = 5;

    private boolean createdWithinRaceWindow(EmployeeAttendance a) {
        return a.getCreatedAt() != null
                && Duration.between(a.getCreatedAt(), LocalDateTime.now()).getSeconds() < RACE_WINDOW_SECONDS;
    }

    @Override
    public EmployeeAttendance record(RecordEmployeeAttendanceCommand cmd) {
        return attendanceRepository.save(EmployeeAttendance.builder()
                .tenantId(TenantContext.get()).employeeId(cmd.employeeId()).date(cmd.date())
                .status(cmd.status()).reason(cmd.reason()).build());
    }

    @Override
    public EmployeeAttendance recordEntry(Long employeeId) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        // Scan d'entrée répété avant toute sortie (§16, même règle que pour l'élève/l'enseignant — voir
        // StudentAttendanceService#recordEntry pour le détail de la fenêtre de course réseau) : refus
        // explicite plutôt que de recalculer/écraser silencieusement le premier pointage. Un seul
        // pointage par jour ici (pas par séance comme l'enseignant) : "déjà scanné à l'entrée" se
        // vérifie donc sur checkOutTime == null du pointage du jour, pas sur un créneau précis.
        Optional<EmployeeAttendance> existing = attendanceRepository.findByEmployeeIdAndDate(employeeId, today);
        if (existing.isPresent() && existing.get().getCheckOutTime() == null && !createdWithinRaceWindow(existing.get())) {
            throw new BusinessRuleException(
                    "Ce badge a déjà été scanné à l'entrée. Scannez-le à la sortie avant un nouvel accès.");
        }
        return existing.orElseGet(() -> {
            AttendanceSettings s = settings(TenantContext.get());
            // Marge de tolérance générale (§réglages) : une entrée dans les N minutes après l'horaire de
            // référence n'est jamais comptée en retard — même règle que pour élève/enseignant.
            LocalTime deadline = s.employeeCheckInTimeOrDefault().plusMinutes(s.transitionMarginMinutesOrDefault());
            int late = (int) Math.max(0, Duration.between(deadline, now).toMinutes());
            return attendanceRepository.save(EmployeeAttendance.builder()
                    .tenantId(TenantContext.get()).employeeId(employeeId).date(today)
                    .status(EmployeeAttendanceStatus.PRESENT).checkInTime(now).lateMinutes(late).earlyMinutes(0)
                    .createdAt(LocalDateTime.now()).build());
        });
    }

    @Override
    public EmployeeAttendance recordExit(Long employeeId) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        // Verrou pessimiste (§15/§16) : voir StudentAttendanceService#recordExit — sérialise deux
        // scans de sortie quasi simultanés du même badge.
        EmployeeAttendance a = attendanceRepository.lockByEmployeeIdAndDate(employeeId, today)
                .orElseThrow(() -> new BusinessRuleException("Aucun pointage d'entrée trouvé aujourd'hui : scannez d'abord l'entrée."));
        if (a.getCheckOutTime() != null) {
            // Déjà sorti aujourd'hui (second scan de sortie, retentative réseau…) : renvoie le
            // pointage existant sans écraser l'heure de sortie.
            return a;
        }
        AttendanceSettings s = settings(TenantContext.get());
        int early = (int) Math.max(0, Duration.between(now, s.employeeCheckOutTimeOrDefault()).toMinutes());
        a.setCheckOutTime(now);
        a.setEarlyMinutes(early);
        return attendanceRepository.save(a);
    }

    @Override
    public EmployeeAttendance recordManualEntry(Long employeeId, LocalDate date, LocalTime checkInTime) {
        AttendanceSettings s = settings(TenantContext.get());
        LocalTime deadline = s.employeeCheckInTimeOrDefault().plusMinutes(s.transitionMarginMinutesOrDefault());
        int late = (int) Math.max(0, Duration.between(deadline, checkInTime).toMinutes());
        EmployeeAttendance a = attendanceRepository.findByEmployeeIdAndDate(employeeId, date)
                .orElseGet(() -> EmployeeAttendance.builder()
                        .tenantId(TenantContext.get()).employeeId(employeeId).date(date).earlyMinutes(0).build());
        a.setStatus(EmployeeAttendanceStatus.PRESENT);
        a.setCheckInTime(checkInTime);
        a.setLateMinutes(late);
        return attendanceRepository.save(a);
    }

    @Override
    public EmployeeAttendance recordManualExit(Long employeeId, LocalDate date, LocalTime checkOutTime) {
        AttendanceSettings s = settings(TenantContext.get());
        int early = (int) Math.max(0, Duration.between(checkOutTime, s.employeeCheckOutTimeOrDefault()).toMinutes());
        EmployeeAttendance a = attendanceRepository.findByEmployeeIdAndDate(employeeId, date)
                .orElseGet(() -> EmployeeAttendance.builder()
                        .tenantId(TenantContext.get()).employeeId(employeeId).date(date)
                        .status(EmployeeAttendanceStatus.PRESENT).lateMinutes(0).build());
        a.setCheckOutTime(checkOutTime);
        a.setEarlyMinutes(early);
        return attendanceRepository.save(a);
    }

    @Override
    public EmployeeAttendance markAbsent(Long employeeId, String reason) {
        LocalDate today = LocalDate.now();
        EmployeeAttendance a = attendanceRepository.findByEmployeeIdAndDate(employeeId, today)
                .orElseGet(() -> EmployeeAttendance.builder()
                        .tenantId(TenantContext.get()).employeeId(employeeId).date(today)
                        .lateMinutes(0).earlyMinutes(0).build());
        a.setStatus(EmployeeAttendanceStatus.ABSENT);
        a.setReason(reason != null ? reason : "Absence signalée");
        return attendanceRepository.save(a);
    }

    @Override
    @Transactional(readOnly = true)
    public MonthlySummary monthlySummary(Long employeeId, int year, int month) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        List<EmployeeAttendance> recs = attendanceRepository.findByEmployeeIdAndDateBetween(employeeId, from, to);
        long present = recs.stream().filter(r -> r.getStatus() == EmployeeAttendanceStatus.PRESENT).count();
        long absent = recs.stream().filter(r -> r.getStatus() == EmployeeAttendanceStatus.ABSENT).count();
        int totalLate = recs.stream().mapToInt(r -> r.getLateMinutes() != null ? r.getLateMinutes() : 0).sum();
        int totalEarly = recs.stream().mapToInt(r -> r.getEarlyMinutes() != null ? r.getEarlyMinutes() : 0).sum();
        return new MonthlySummary(employeeId, year, month, present, absent, totalLate, totalEarly);
    }

    @Override
    public EmployeeAttendance update(Long id, EmployeeAttendanceStatus status, String reason) {
        EmployeeAttendance a = attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EmployeeAttendance", id));
        a.setStatus(status); a.setReason(reason);
        return attendanceRepository.save(a);
    }

    @Override @Transactional(readOnly = true)
    public EmployeeAttendance findById(Long id) {
        return attendanceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("EmployeeAttendance", id));
    }

    @Override @Transactional(readOnly = true)
    public List<EmployeeAttendance> findByEmployee(Long employeeId, LocalDate from, LocalDate to) {
        return attendanceRepository.findByEmployeeIdAndDateBetween(employeeId, from, to);
    }

    @Override @Transactional(readOnly = true)
    public EmployeeAttendanceStatsResponse getStats(Long employeeId, LocalDate from, LocalDate to) {
        LocalDate effectiveFrom = from != null ? from : LocalDate.now().withDayOfYear(1);
        LocalDate effectiveTo = to != null ? to : LocalDate.now();
        long total = attendanceRepository.countByEmployeeIdAndDateBetween(employeeId, effectiveFrom, effectiveTo);
        long absent = attendanceRepository.countByEmployeeIdAndDateBetweenAndStatus(employeeId, effectiveFrom, effectiveTo, EmployeeAttendanceStatus.ABSENT);
        long present = total - absent;
        BigDecimal rate = total == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(present).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        return new EmployeeAttendanceStatsResponse(employeeId, total, present, absent, rate);
    }

    @Override public void delete(Long id) { attendanceRepository.deleteById(id); }
}
