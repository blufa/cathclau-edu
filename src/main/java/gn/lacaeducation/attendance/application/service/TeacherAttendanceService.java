package gn.lacaeducation.attendance.application.service;
import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.academics.ScheduleSlotPublicInfo;
import gn.lacaeducation.attendance.application.event.TeacherExitEvent;
import gn.lacaeducation.attendance.application.port.in.TeacherAttendanceUseCase;
import gn.lacaeducation.attendance.application.port.out.AttendanceReportPdfGenerator;
import gn.lacaeducation.attendance.application.port.out.AttendanceSettingsRepository;
import gn.lacaeducation.attendance.application.port.out.RecessWindowRepository;
import gn.lacaeducation.attendance.application.port.out.TeacherAttendanceRepository;
import gn.lacaeducation.attendance.application.port.out.TeacherCheckinRepository;
import gn.lacaeducation.attendance.domain.AttendanceSettings;
import gn.lacaeducation.attendance.domain.TeacherAttendance;
import gn.lacaeducation.attendance.domain.TeacherAttendanceStatus;
import gn.lacaeducation.attendance.domain.TeacherCheckin;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.staff.TeacherPublicInfoProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;
    private final SessionResolver sessionResolver;
    private final AttendanceSettingsRepository settingsRepository;
    private final RecessWindowRepository recessWindowRepository;
    private final AttendanceReportPdfGenerator pdfGenerator;
    private final TeacherPublicInfoProvider teacherPublicInfoProvider;

    /**
     * Heure à partir de laquelle un retard commence à être compté pour cette séance : l'heure de
     * début prévue, repoussée par la marge de tolérance générale configurée par l'école (§réglages —
     * s'applique à TOUTE entrée, pas seulement en cas de changement de salle : une tolérance de 5 min
     * signifie qu'une entrée à l'heure + 5 min n'est jamais comptée en retard, quelle qu'en soit la
     * raison) et/ou par une fenêtre de récréation (temps non imputable à l'enseignant).
     */
    private LocalTime graceDeadline(LocalTime sessionStart) {
        return sessionResolver.graceDeadline(sessionStart, transitionMargin(), recessWindowRepository.findByTenantId(TenantContext.get()));
    }

    private int transitionMargin() {
        return settingsRepository.findByTenantId(TenantContext.get())
                .map(AttendanceSettings::transitionMarginMinutesOrDefault)
                .orElseGet(() -> AttendanceSettings.defaults().transitionMarginMinutesOrDefault());
    }

    /** Fenêtre de course réseau (§16) : même mécanisme que
     * {@code StudentAttendanceService#createdWithinRaceWindow} — une ligne créée il y a moins de
     * {@code RACE_WINDOW_SECONDS} vient très probablement de la retentative du MÊME tap physique,
     * pas d'un second scan humain. */
    private static final long RACE_WINDOW_SECONDS = 5;

    private boolean createdWithinRaceWindow(TeacherCheckin checkin) {
        return checkin.getCreatedAt() != null
                && Duration.between(checkin.getCreatedAt(), LocalDateTime.now()).getSeconds() < RACE_WINDOW_SECONDS;
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
        SessionResolver.Block block = sessionResolver.currentBlock(academics.findScheduleByTeacher(teacherId), today, now, transitionMargin());
        if (block == null) {
            throw new gn.lacaeducation.common.BusinessRuleException("Aucune séance programmée aujourd'hui pour cet enseignant.");
        }
        // Scan d'entrée répété avant toute sortie (§16, même règle que pour l'élève — voir
        // StudentAttendanceService#recordEntry pour le détail de la fenêtre de course réseau) : refus
        // explicite plutôt que de recalculer/écraser silencieusement le premier pointage.
        java.util.Optional<TeacherCheckin> existing = checkinRepository.findByTeacherIdAndDateAndSlot(teacherId, today, block.firstSlotId());
        if (existing.isPresent() && existing.get().getCheckOutTime() == null && !createdWithinRaceWindow(existing.get())) {
            throw new gn.lacaeducation.common.BusinessRuleException(
                    "Ce badge a déjà été scanné à l'entrée. Scannez-le à la sortie avant un nouvel accès.");
        }
        TeacherCheckin checkin = existing.orElseGet(() -> {
                    LocalTime start = block.start();
                    LocalTime end = block.end();
                    LocalTime deadline = graceDeadline(start);
                    int late = (int) Math.max(0, Duration.between(deadline, now).toMinutes());
                    return checkinRepository.save(TeacherCheckin.builder()
                            .tenantId(TenantContext.get()).teacherId(teacherId).checkinDate(today)
                            .scheduleSlotId(block.firstSlotId()).sessionStart(start).sessionEnd(end)
                            .checkInTime(now).lateMinutes(late).earlyMinutes(0).createdAt(LocalDateTime.now()).build());
                });
        upsertDailyAttendance(teacherId, today);
        return checkin;
    }

    @Override
    public TeacherCheckin recordExit(Long teacherId) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        SessionResolver.Block block = sessionResolver.currentBlock(academics.findScheduleByTeacher(teacherId), today, now, transitionMargin());
        if (block == null) {
            throw new gn.lacaeducation.common.BusinessRuleException("Aucune séance programmée aujourd'hui pour cet enseignant.");
        }
        // Verrou pessimiste (§15/§16) : voir StudentAttendanceService#recordExit — sérialise deux
        // scans de sortie quasi simultanés du même badge.
        TeacherCheckin checkin = checkinRepository.lockByTeacherIdAndDateAndSlot(teacherId, today, block.firstSlotId())
                .orElseThrow(() -> new gn.lacaeducation.common.BusinessRuleException(
                        "Aucun pointage d'entrée trouvé pour cette séance : scannez d'abord l'entrée."));
        if (checkin.getCheckOutTime() != null) {
            // Déjà sorti pour cette séance (second scan de sortie, retentative réseau…) : renvoie le
            // pointage existant sans écraser l'heure de sortie ni republier l'événement.
            return checkin;
        }
        checkin.setCheckOutTime(now);
        if (checkin.getSessionEnd() == null) checkin.setSessionEnd(block.end());
        // Départ anticipé : sortie pointée avant la fin prévue de la séance (symétrique du retard
        // à l'entrée). Un pointage de sortie existe forcément ici (on vient de le fixer ci-dessus).
        int early = (int) Math.max(0, Duration.between(now, checkin.getSessionEnd()).toMinutes());
        checkin.setEarlyMinutes(early);
        TeacherCheckin saved = checkinRepository.save(checkin);

        // §retour fondateurs : même mécanique que StudentAttendanceService#recordExit (durée depuis
        // le premier pointage du jour), mais SANS notification SMS — un enseignant n'a pas de parent.
        LocalTime firstCheckInTime = checkinRepository.findByTeacherIdAndDate(teacherId, today).stream()
                .map(TeacherCheckin::getCheckInTime).filter(Objects::nonNull)
                .min(LocalTime::compareTo).orElse(null);
        int minutesAtSchool = firstCheckInTime != null
                ? (int) Math.max(0, Duration.between(firstCheckInTime, now).toMinutes()) : 0;
        eventPublisher.publishEvent(new TeacherExitEvent(TenantContext.get(), teacherId, today,
                firstCheckInTime, now, minutesAtSchool));
        return saved;
    }

    @Override
    public TeacherCheckin recordManualEntry(Long teacherId, LocalDate date, LocalTime checkInTime) {
        // Correction manuelle (saisie a posteriori) : contrairement au scan (recordEntry, idempotent —
        // le 1er scan fait foi), ici on écrase l'heure d'entrée existante par la valeur saisie, pour
        // permettre de corriger un pointage déjà enregistré.
        SessionResolver.Block block = sessionResolver.currentBlock(academics.findScheduleByTeacher(teacherId), date, checkInTime, transitionMargin());
        if (block == null) {
            throw new gn.lacaeducation.common.BusinessRuleException("Aucune séance programmée ce jour-là pour cet enseignant.");
        }
        LocalTime start = block.start();
        LocalTime end = block.end();
        LocalTime deadline = graceDeadline(start);
        int late = (int) Math.max(0, Duration.between(deadline, checkInTime).toMinutes());
        TeacherCheckin checkin = checkinRepository.findByTeacherIdAndDateAndSlot(teacherId, date, block.firstSlotId())
                .orElseGet(() -> TeacherCheckin.builder()
                        .tenantId(TenantContext.get()).teacherId(teacherId).checkinDate(date)
                        .scheduleSlotId(block.firstSlotId()).createdAt(LocalDateTime.now()).build());
        checkin.setSessionStart(start);
        checkin.setSessionEnd(end);
        checkin.setCheckInTime(checkInTime);
        checkin.setLateMinutes(late);
        TeacherCheckin saved = checkinRepository.save(checkin);
        upsertDailyAttendance(teacherId, date);
        return saved;
    }

    @Override
    public TeacherCheckin recordManualExit(Long teacherId, LocalDate date, LocalTime checkOutTime) {
        // Symétrique de recordManualEntry : crée le pointage si absent (badge oublié toute la séance),
        // sinon complète le pointage d'entrée déjà existant avec l'heure de sortie saisie.
        SessionResolver.Block block = sessionResolver.currentBlock(academics.findScheduleByTeacher(teacherId), date, checkOutTime, transitionMargin());
        if (block == null) {
            throw new gn.lacaeducation.common.BusinessRuleException("Aucune séance programmée ce jour-là pour cet enseignant.");
        }
        LocalTime start = block.start();
        LocalTime end = block.end();
        TeacherCheckin checkin = checkinRepository.findByTeacherIdAndDateAndSlot(teacherId, date, block.firstSlotId())
                .orElseGet(() -> TeacherCheckin.builder()
                        .tenantId(TenantContext.get()).teacherId(teacherId).checkinDate(date)
                        .scheduleSlotId(block.firstSlotId()).sessionStart(start).lateMinutes(0).createdAt(LocalDateTime.now()).build());
        checkin.setSessionEnd(end);
        checkin.setCheckOutTime(checkOutTime);
        int early = (int) Math.max(0, Duration.between(checkOutTime, end).toMinutes());
        checkin.setEarlyMinutes(early);
        TeacherCheckin saved = checkinRepository.save(checkin);
        upsertDailyAttendance(teacherId, date);

        LocalTime firstCheckInTime = checkinRepository.findByTeacherIdAndDate(teacherId, date).stream()
                .map(TeacherCheckin::getCheckInTime).filter(Objects::nonNull)
                .min(LocalTime::compareTo).orElse(null);
        int minutesAtSchool = firstCheckInTime != null
                ? (int) Math.max(0, Duration.between(firstCheckInTime, checkOutTime).toMinutes()) : 0;
        eventPublisher.publishEvent(new TeacherExitEvent(TenantContext.get(), teacherId, date,
                firstCheckInTime, checkOutTime, minutesAtSchool));
        return saved;
    }

    @Override
    public TeacherAttendance markAbsent(Long teacherId, String reason) {
        LocalDate today = LocalDate.now();
        List<TeacherAttendance> existing = attendanceRepository.findByTeacherIdAndDateBetween(teacherId, today, today);
        TeacherAttendance saved;
        if (!existing.isEmpty()) {
            TeacherAttendance a = existing.get(0);
            a.setStatus(TeacherAttendanceStatus.ABSENT);
            a.setReason(reason != null ? reason : "Absence signalée");
            saved = attendanceRepository.save(a);
        } else {
            saved = attendanceRepository.save(TeacherAttendance.builder()
                    .tenantId(TenantContext.get()).teacherId(teacherId).date(today)
                    .status(TeacherAttendanceStatus.ABSENT).reason(reason != null ? reason : "Absence signalée").build());
        }
        publishTeacherAlert(teacherId, today, "ABSENT", null, null);
        return saved;
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
        // §retour fondateurs : alerte uniquement au PREMIER scan du jour (jamais redéclenchée par les
        // scans suivants, même si le retard cumulé continue d'augmenter pour le reporting RH/paie).
        boolean firstScanOfDay = existing.isEmpty();
        if (!existing.isEmpty()) {
            TeacherAttendance a = existing.get(0);
            a.setStatus(TeacherAttendanceStatus.PRESENT);
            a.setCheckInTime(firstCheckIn);
            a.setLateMinutes(dayLate);
            attendanceRepository.save(a);
        } else {
            attendanceRepository.save(TeacherAttendance.builder()
                    .tenantId(TenantContext.get()).teacherId(teacherId).date(today)
                    .status(TeacherAttendanceStatus.PRESENT).reason("Scan carte")
                    .checkInTime(firstCheckIn).lateMinutes(dayLate).build());
        }
        if (firstScanOfDay && dayLate > 0) {
            publishTeacherAlert(teacherId, today, "LATE", dayLate, firstCheckIn);
        }
    }

    /** Résout nom/téléphone ici (module {@code attendance}) plutôt que de faire dépendre
     * {@code communication} de {@code staff} pour ce seul besoin — voir Javadoc de
     * {@link gn.lacaeducation.attendance.application.event.TeacherAbsentEvent}. */
    private void publishTeacherAlert(Long teacherId, LocalDate date, String status, Integer lateMinutes, LocalTime checkInTime) {
        var info = teacherPublicInfoProvider.findTeacher(teacherId).orElse(null);
        String name = info != null ? (info.firstName() + " " + info.lastName()).trim() : "Enseignant #" + teacherId;
        String phone = info != null ? info.phone() : null;
        eventPublisher.publishEvent(new gn.lacaeducation.attendance.application.event.TeacherAbsentEvent(
                TenantContext.get(), teacherId, name, phone, date, status, lateMinutes, checkInTime));
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
        // Manque à gagner RÉEL par séance, pour le calcul des heures effectives (paie) — distinct de
        // totalLate/totalEarly ci-dessus qui restent affichés tels quels pour le rapport de ponctualité
        // (RH, voir latenessDetail/latenessReportPdf). §retour fondateurs : un enseignant arrivé à 8h30
        // (30 min de retard) qui reste jusqu'à 14h30 (30 min après la fin prévue) a bien fait ses heures
        // — compare directement la présence nette (sortie - entrée) à la durée prévue de la séance,
        // plutôt que de soustraire retard et départ anticipé indépendamment (ce qui pénalisait deux fois
        // un enseignant ayant compensé). Sans pointage de sortie, impossible de savoir s'il a compensé :
        // on retombe sur le seul retard connu (même comportement qu'avant pour ce cas).
        int totalShortfall = checkins.stream().mapToInt(c -> {
            if (c.getSessionStart() == null || c.getSessionEnd() == null) return 0;
            int scheduled = (int) Duration.between(c.getSessionStart(), c.getSessionEnd()).toMinutes();
            if (c.getCheckInTime() != null && c.getCheckOutTime() != null) {
                int presentMinutes = (int) Math.max(0, Duration.between(c.getCheckInTime(), c.getCheckOutTime()).toMinutes());
                return Math.max(0, scheduled - presentMinutes);
            }
            return c.getLateMinutes() != null ? c.getLateMinutes() : 0;
        }).sum();
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
        int effective = Math.max(0, dueMinutes - absentMinutes - totalShortfall);
        return new MonthlySummary(teacherId, year, month, present, absent, totalLate, totalEarly, dueMinutes, absentMinutes, effective);
    }

    @Override @Transactional(readOnly = true)
    public Map<Long, ClassLateEarly> monthlyLateEarlyByClass(Long teacherId, int year, int month) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        // Résout chaque pointage (par scheduleSlotId) vers la classe concernée, pour cumuler retard et
        // départ anticipé séparément par classe (§paie — tarif normal/examen).
        Map<Long, Long> classIdBySlot = new HashMap<>();
        for (ScheduleSlotPublicInfo s : academics.findScheduleByTeacher(teacherId)) {
            if (s.id() != null && s.classId() != null) classIdBySlot.put(s.id(), s.classId());
        }
        Map<Long, int[]> accByClass = new HashMap<>(); // [lateMinutes, earlyMinutes]
        for (TeacherCheckin c : checkinRepository.findByTeacherIdAndDateBetween(teacherId, from, to)) {
            Long classId = c.getScheduleSlotId() != null ? classIdBySlot.get(c.getScheduleSlotId()) : null;
            if (classId == null) continue; // créneau non résolu (supprimé depuis) : ignoré du détail par classe
            int[] acc = accByClass.computeIfAbsent(classId, k -> new int[2]);
            acc[0] += c.getLateMinutes() != null ? c.getLateMinutes() : 0;
            acc[1] += c.getEarlyMinutes() != null ? c.getEarlyMinutes() : 0;
        }
        Map<Long, ClassLateEarly> result = new HashMap<>();
        for (var e : accByClass.entrySet()) result.put(e.getKey(), new ClassLateEarly(e.getValue()[0], e.getValue()[1]));
        return result;
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

    @Override
    @Transactional(readOnly = true)
    public List<LatenessDetailRow> latenessDetail(Long teacherId, LocalDate from, LocalDate to) {
        Map<Long, ScheduleSlotPublicInfo> slotById = new HashMap<>();
        for (ScheduleSlotPublicInfo s : academics.findScheduleByTeacher(teacherId)) {
            if (s.id() != null) slotById.put(s.id(), s);
        }
        return checkinRepository.findByTeacherIdAndDateBetween(teacherId, from, to).stream()
                .filter(c -> (c.getLateMinutes() != null && c.getLateMinutes() > 0)
                        || (c.getEarlyMinutes() != null && c.getEarlyMinutes() > 0))
                .map(c -> {
                    ScheduleSlotPublicInfo slot = c.getScheduleSlotId() != null ? slotById.get(c.getScheduleSlotId()) : null;
                    return new LatenessDetailRow(c.getCheckinDate(),
                            slot != null ? slot.classId() : null, slot != null ? slot.subjectId() : null,
                            c.getLateMinutes() != null ? c.getLateMinutes() : 0,
                            c.getEarlyMinutes() != null ? c.getEarlyMinutes() : 0);
                })
                .sorted(Comparator.comparing(LatenessDetailRow::date))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] latenessReportPdf(Long teacherId, LocalDate from, LocalDate to) {
        List<LatenessDetailRow> detail = latenessDetail(teacherId, from, to);
        java.util.Set<Long> subjectIds = detail.stream().map(LatenessDetailRow::subjectId)
                .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        Map<Long, String> subjectLabels = academics.subjectLabels(subjectIds);
        Map<Long, String> classLabels = new HashMap<>();
        for (Long classId : detail.stream().map(LatenessDetailRow::classId).filter(java.util.Objects::nonNull).toList()) {
            classLabels.computeIfAbsent(classId, id -> academics.findClass(id).map(c -> c.label()).orElse("Classe #" + id));
        }
        List<AttendanceReportPdfGenerator.TeacherReportRow> rows = new java.util.ArrayList<>();
        for (LatenessDetailRow r : detail) {
            rows.add(new AttendanceReportPdfGenerator.TeacherReportRow(r.date(),
                    r.subjectId() != null ? subjectLabels.getOrDefault(r.subjectId(), "Matière #" + r.subjectId()) : null,
                    r.classId() != null ? classLabels.get(r.classId()) : null,
                    r.lateMinutes(), r.earlyMinutes(), false, null));
        }
        for (TeacherAttendance a : attendanceRepository.findByTeacherIdAndDateBetween(teacherId, from, to)) {
            if (a.getStatus() == TeacherAttendanceStatus.ABSENT) {
                rows.add(new AttendanceReportPdfGenerator.TeacherReportRow(a.getDate(), null, null, 0, 0, true, a.getReason()));
            }
        }
        rows.sort(Comparator.comparing(AttendanceReportPdfGenerator.TeacherReportRow::date));
        String teacherName = teacherPublicInfoProvider.findTeacher(teacherId)
                .map(t -> (t.firstName() + " " + t.lastName()).trim())
                .orElse("Enseignant #" + teacherId);
        return pdfGenerator.teacherLatenessReport(new AttendanceReportPdfGenerator.TeacherLatenessReport(
                TenantContext.get(), teacherName, from, to, rows));
    }
}
