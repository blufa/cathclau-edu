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
import java.time.LocalTime;
import java.util.List;

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
        // Idempotent comme le scan enseignant : le 1er scan du jour fait foi, un second scan d'entrée
        // ne modifie rien (la sortie se pointe via recordExit, pas un second appel ici).
        return attendanceRepository.findByEmployeeIdAndDate(employeeId, today).orElseGet(() -> {
            AttendanceSettings s = settings(TenantContext.get());
            // Marge de tolérance générale (§réglages) : une entrée dans les N minutes après l'horaire de
            // référence n'est jamais comptée en retard — même règle que pour élève/enseignant.
            LocalTime deadline = s.employeeCheckInTimeOrDefault().plusMinutes(s.transitionMarginMinutesOrDefault());
            int late = (int) Math.max(0, Duration.between(deadline, now).toMinutes());
            return attendanceRepository.save(EmployeeAttendance.builder()
                    .tenantId(TenantContext.get()).employeeId(employeeId).date(today)
                    .status(EmployeeAttendanceStatus.PRESENT).checkInTime(now).lateMinutes(late).earlyMinutes(0).build());
        });
    }

    @Override
    public EmployeeAttendance recordExit(Long employeeId) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        EmployeeAttendance a = attendanceRepository.findByEmployeeIdAndDate(employeeId, today)
                .orElseThrow(() -> new BusinessRuleException("Aucun pointage d'entrée trouvé aujourd'hui : scannez d'abord l'entrée."));
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
