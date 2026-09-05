package gn.lacaeducation.attendance.application.service;
import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.academics.ScheduleSlotPublicInfo;
import gn.lacaeducation.attendance.application.port.in.TeacherAttendanceUseCase;
import gn.lacaeducation.attendance.application.port.out.AttendanceSettingsRepository;
import gn.lacaeducation.attendance.application.port.out.RecessWindowRepository;
import gn.lacaeducation.attendance.application.port.out.TeacherAttendanceRepository;
import gn.lacaeducation.attendance.application.port.out.TeacherCheckinRepository;
import gn.lacaeducation.attendance.domain.AttendanceSettings;
import gn.lacaeducation.attendance.domain.RecessWindow;
import gn.lacaeducation.attendance.domain.TeacherAttendance;
import gn.lacaeducation.attendance.domain.TeacherAttendanceStatus;
import gn.lacaeducation.attendance.domain.TeacherCheckin;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.common.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
@Service @Transactional @RequiredArgsConstructor
public class TeacherAttendanceService implements TeacherAttendanceUseCase {
    private final TeacherAttendanceRepository attendanceRepository;
    private final TeacherCheckinRepository checkinRepository;
    private final AcademicsDataProvider academics;
    private final SessionResolver sessionResolver;
    private final AttendanceSettingsRepository settingsRepository;
    private final RecessWindowRepository recessWindowRepository;

    /**
     * Heure à partir de laquelle un retard commence à être compté pour cette séance : l'heure de
     * début prévue, repoussée si (a) elle tombe dans une fenêtre de récréation définie par l'école
     * (temps non imputable à l'enseignant) et/ou (b) la séance précédente du même jour se termine
     * pile à cette heure dans une salle différente (marge de transition, temps de trajet).
     */
    private LocalTime graceDeadline(Long teacherId, LocalDate date, ScheduleSlotPublicInfo session) {
        LocalTime start = LocalTime.parse(session.startTime());
        LocalTime deadline = start;
        for (RecessWindow w : recessWindowRepository.findByTenantId(TenantContext.get())) {
            if (w.contains(start) && w.getEndTime().isAfter(deadline)) deadline = w.getEndTime();
        }
        List<ScheduleSlotPublicInfo> daySlots = sessionResolver.slotsForDay(academics.findScheduleByTeacher(teacherId), date);
        ScheduleSlotPublicInfo previous = daySlots.stream()
                .filter(s -> !Objects.equals(s.id(), session.id()))
                .filter(s -> LocalTime.parse(s.endTime()).equals(start))
                .findFirst().orElse(null);
        if (previous != null && !Objects.equals(previous.roomId(), session.roomId())) {
            int margin = settingsRepository.findByTenantId(TenantContext.get())
                    .map(AttendanceSettings::transitionMarginMinutesOrDefault)
                    .orElseGet(() -> AttendanceSettings.defaults().transitionMarginMinutesOrDefault());
            LocalTime withMargin = start.plusMinutes(margin);
            if (withMargin.isAfter(deadline)) deadline = withMargin;
        }
        return deadline;
    }
    @Override
    public TeacherAttendance record(RecordTeacherAttendanceCommand cmd) {
        return attendanceRepository.save(TeacherAttendance.builder()
                .tenantId(TenantContext.get())
                .teacherId(cmd.teacherId()).date(cmd.date()).status(cmd.status()).reason(cmd.reason()).build());
    }

    @Override
    public TeacherCheckin recordEntry(Long teacherId) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        ScheduleSlotPublicInfo session = sessionResolver.currentSession(academics.findScheduleByTeacher(teacherId), today, now);
        if (session == null) {
            throw new gn.lacaeducation.common.BusinessRuleException("Aucune séance programmée aujourd'hui pour cet enseignant.");
        }
        TeacherCheckin checkin = checkinRepository.findByTeacherIdAndDateAndSlot(teacherId, today, session.id())
                .orElseGet(() -> {
                    LocalTime start = LocalTime.parse(session.startTime());
                    LocalTime end = LocalTime.parse(session.endTime());
                    LocalTime deadline = graceDeadline(teacherId, today, session);
                    int late = (int) Math.max(0, Duration.between(deadline, now).toMinutes());
                    return checkinRepository.save(TeacherCheckin.builder()
                            .tenantId(TenantContext.get()).teacherId(teacherId).checkinDate(today)
                            .scheduleSlotId(session.id()).sessionStart(start).sessionEnd(end)
                            .checkInTime(now).lateMinutes(late).earlyMinutes(0).createdAt(LocalDateTime.now()).build());
                });
        upsertDailyAttendance(teacherId, today);
        return checkin;
    }

    @Override
    public TeacherCheckin recordExit(Long teacherId) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        ScheduleSlotPublicInfo session = sessionResolver.currentSession(academics.findScheduleByTeacher(teacherId), today, now);
        if (session == null) {
            throw new gn.lacaeducation.common.BusinessRuleException("Aucune séance programmée aujourd'hui pour cet enseignant.");
        }
        TeacherCheckin checkin = checkinRepository.findByTeacherIdAndDateAndSlot(teacherId, today, session.id())
                .orElseThrow(() -> new gn.lacaeducation.common.BusinessRuleException(
                        "Aucun pointage d'entrée trouvé pour cette séance : scannez d'abord l'entrée."));
        checkin.setCheckOutTime(now);
        if (checkin.getSessionEnd() == null) checkin.setSessionEnd(LocalTime.parse(session.endTime()));
        // Départ anticipé : sortie pointée avant la fin prévue de la séance (symétrique du retard
        // à l'entrée). Un pointage de sortie existe forcément ici (on vient de le fixer ci-dessus).
        int early = (int) Math.max(0, Duration.between(now, checkin.getSessionEnd()).toMinutes());
        checkin.setEarlyMinutes(early);
        return checkinRepository.save(checkin);
    }

    @Override
    public TeacherCheckin recordManualEntry(Long teacherId, LocalDate date, LocalTime checkInTime) {
        // Correction manuelle (saisie a posteriori) : contrairement au scan (recordEntry, idempotent —
        // le 1er scan fait foi), ici on écrase l'heure d'entrée existante par la valeur saisie, pour
        // permettre de corriger un pointage déjà enregistré.
        ScheduleSlotPublicInfo session = sessionResolver.currentSession(academics.findScheduleByTeacher(teacherId), date, checkInTime);
        if (session == null) {
            throw new gn.lacaeducation.common.BusinessRuleException("Aucune séance programmée ce jour-là pour cet enseignant.");
        }
        LocalTime start = LocalTime.parse(session.startTime());
        LocalTime end = LocalTime.parse(session.endTime());
        LocalTime deadline = graceDeadline(teacherId, date, session);
        int late = (int) Math.max(0, Duration.between(deadline, checkInTime).toMinutes());
        TeacherCheckin checkin = checkinRepository.findByTeacherIdAndDateAndSlot(teacherId, date, session.id())
                .orElseGet(() -> TeacherCheckin.builder()
                        .tenantId(TenantContext.get()).teacherId(teacherId).checkinDate(date)
                        .scheduleSlotId(session.id()).createdAt(LocalDateTime.now()).build());
        checkin.setSessionStart(start);
        checkin.setSessionEnd(end);
        checkin.setCheckInTime(checkInTime);
        checkin.setLateMinutes(late);
        TeacherCheckin saved = checkinRepository.save(checkin);
        upsertDailyAttendance(teacherId, date);
        return saved;
    }

    @Override
    public TeacherAttendance markAbsent(Long teacherId, String reason) {
        LocalDate today = LocalDate.now();
        List<TeacherAttendance> existing = attendanceRepository.findByTeacherIdAndDateBetween(teacherId, today, today);
        if (!existing.isEmpty()) {
            TeacherAttendance a = existing.get(0);
            a.setStatus(TeacherAttendanceStatus.ABSENT);
            a.setReason(reason != null ? reason : "Absence signalée");
            return attendanceRepository.save(a);
        }
        return attendanceRepository.save(TeacherAttendance.builder()
                .tenantId(TenantContext.get()).teacherId(teacherId).date(today)
                .status(TeacherAttendanceStatus.ABSENT).reason(reason != null ? reason : "Absence signalée").build());
    }

    /** Statut du jour toujours PRESENT depuis un scan (le retard reste représenté par lateMinutes > 0 —
     * un scan ne peut jamais produire ABSENT, voir {@link #markAbsent}). Cumule le retard de toutes
     * les séances déjà pointées ce jour-là. */
    private void upsertDailyAttendance(Long teacherId, LocalDate today) {
        LocalTime firstCheckIn = checkinRepository.findByTeacherIdAndDate(teacherId, today).stream()
                .map(TeacherCheckin::getCheckInTime).filter(java.util.Objects::nonNull)
                .min(Comparator.naturalOrder()).orElse(null);
        int dayLate = checkinRepository.findByTeacherIdAndDate(teacherId, today).stream()
                .mapToInt(c -> c.getLateMinutes() != null ? c.getLateMinutes() : 0).sum();
        List<TeacherAttendance> existing = attendanceRepository.findByTeacherIdAndDateBetween(teacherId, today, today);
        if (!existing.isEmpty()) {
            TeacherAttendance a = existing.get(0);
            a.setStatus(TeacherAttendanceStatus.PRESENT);
            a.setCheckInTime(firstCheckIn);
            a.setLateMinutes(dayLate);
            attendanceRepository.save(a);
            return;
        }
        attendanceRepository.save(TeacherAttendance.builder()
                .tenantId(TenantContext.get()).teacherId(teacherId).date(today)
                .status(TeacherAttendanceStatus.PRESENT).reason("Scan carte")
                .checkInTime(firstCheckIn).lateMinutes(dayLate).build());
    }

    @Override @Transactional(readOnly = true)
    public MonthlySummary monthlySummary(Long teacherId, int year, int month) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        List<TeacherAttendance> recs = attendanceRepository.findByTeacherIdAndDateBetween(teacherId, from, to);
        long present = recs.stream().filter(r -> r.getStatus() == TeacherAttendanceStatus.PRESENT).count();
        long absent = recs.stream().filter(r -> r.getStatus() == TeacherAttendanceStatus.ABSENT).count();
        // Retard cumulé = somme des retards de chaque séance pointée (pas seulement le 1ᵉʳ cours).
        List<TeacherCheckin> checkins = checkinRepository.findByTeacherIdAndDateBetween(teacherId, from, to);
        int totalLate = checkins.stream().mapToInt(c -> c.getLateMinutes() != null ? c.getLateMinutes() : 0).sum();
        // Départ anticipé cumulé (symétrique) : 0 si la sortie n'a jamais été pointée (bénéfice du doute).
        int totalEarly = checkins.stream().mapToInt(c -> c.getEarlyMinutes() != null ? c.getEarlyMinutes() : 0).sum();
        // Minutes prévues par jour de semaine (emploi du temps hebdomadaire de l'enseignant).
        Map<String, Integer> minutesByDay = new HashMap<>();
        for (var s : academics.findScheduleByTeacher(teacherId)) {
            if (s.startTime() == null || s.endTime() == null || s.day() == null) continue;
            int dur = (int) Duration.between(LocalTime.parse(s.startTime()), LocalTime.parse(s.endTime())).toMinutes();
            minutesByDay.merge(s.day().toUpperCase(), dur, Integer::sum);
        }
        // Heures dues du mois : toutes les occurrences de chaque jour de la semaine sur le mois.
        int dueMinutes = 0;
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            dueMinutes += minutesByDay.getOrDefault(frenchDay(d.getDayOfWeek()), 0);
        }
        // Heures perdues pour absence (jours pointés ABSENT).
        int absentMinutes = recs.stream()
                .filter(r -> r.getStatus() == TeacherAttendanceStatus.ABSENT)
                .mapToInt(r -> minutesByDay.getOrDefault(frenchDay(r.getDate().getDayOfWeek()), 0))
                .sum();
        int effective = Math.max(0, dueMinutes - absentMinutes - totalLate - totalEarly);
        return new MonthlySummary(teacherId, year, month, present, absent, totalLate, totalEarly, dueMinutes, absentMinutes, effective);
    }

    private static String frenchDay(DayOfWeek dow) {
        return switch (dow) {
            case MONDAY -> "LUNDI"; case TUESDAY -> "MARDI"; case WEDNESDAY -> "MERCREDI";
            case THURSDAY -> "JEUDI"; case FRIDAY -> "VENDREDI"; case SATURDAY -> "SAMEDI"; case SUNDAY -> "DIMANCHE";
        };
    }

    @Override
    public TeacherAttendance update(Long id, TeacherAttendanceStatus status, String reason) {
        TeacherAttendance a = attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TeacherAttendance", id));
        a.setStatus(status); a.setReason(reason);
        return attendanceRepository.save(a);
    }
    @Override @Transactional(readOnly = true)
    public TeacherAttendance findById(Long id) { return attendanceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("TeacherAttendance", id)); }
    @Override @Transactional(readOnly = true)
    public List<TeacherAttendance> findByTeacher(Long teacherId, LocalDate from, LocalDate to) { return attendanceRepository.findByTeacherIdAndDateBetween(teacherId, from, to); }
    @Override @Transactional(readOnly = true)
    public TeacherAttendanceStatsResponse getStats(Long teacherId, LocalDate from, LocalDate to) {
        LocalDate effectiveFrom = from != null ? from : LocalDate.now().withDayOfYear(1);
        LocalDate effectiveTo = to != null ? to : LocalDate.now();
        long total = attendanceRepository.countByTeacherIdAndDateBetween(teacherId, effectiveFrom, effectiveTo);
        long absent = attendanceRepository.countByTeacherIdAndDateBetweenAndStatus(teacherId, effectiveFrom, effectiveTo, TeacherAttendanceStatus.ABSENT);
        long present = total - absent;
        BigDecimal rate = total == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(present).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        return new TeacherAttendanceStatsResponse(teacherId, total, present, absent, rate);
    }
    @Override @Transactional(readOnly = true)
    public List<TeacherCheckin> findCheckins(Long teacherId, LocalDate date) {
        return checkinRepository.findByTeacherIdAndDate(teacherId, date);
    }

    @Override public void delete(Long id) { attendanceRepository.deleteById(id); }
}
