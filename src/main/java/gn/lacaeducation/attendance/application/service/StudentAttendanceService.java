package gn.lacaeducation.attendance.application.service;
import gn.lacaeducation.attendance.StudentAttendanceNotifier;
import gn.lacaeducation.attendance.application.event.StudentAbsentEvent;
import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.attendance.application.port.in.StudentAttendanceUseCase;
import gn.lacaeducation.attendance.application.port.out.AttendanceReportPdfGenerator;
import gn.lacaeducation.attendance.application.port.out.AttendanceReportPdfGenerator.AbsenteeList;
import gn.lacaeducation.attendance.application.port.out.AttendanceReportPdfGenerator.AbsenteeRow;
import gn.lacaeducation.attendance.application.port.out.AttendanceReportPdfGenerator.LatenessReport;
import gn.lacaeducation.attendance.application.port.out.AttendanceReportPdfGenerator.LatenessRow;
import gn.lacaeducation.academics.ScheduleSlotPublicInfo;
import gn.lacaeducation.attendance.application.port.out.ChildPickupRepository;
import gn.lacaeducation.attendance.application.port.out.StudentAttendanceRepository;
import gn.lacaeducation.attendance.application.port.out.StudentCheckinRepository;
import gn.lacaeducation.attendance.domain.AttendanceStatus;
import gn.lacaeducation.attendance.domain.ChildPickup;
import gn.lacaeducation.attendance.domain.StudentAttendance;
import gn.lacaeducation.attendance.domain.StudentCheckin;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ConflictException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.students.StudentDataProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
@Service @Transactional @RequiredArgsConstructor
public class StudentAttendanceService implements StudentAttendanceUseCase, StudentAttendanceNotifier {
    private final StudentAttendanceRepository attendanceRepository;
    private final StudentCheckinRepository checkinRepository;
    private final ChildPickupRepository childPickupRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final StudentDataProvider studentDataProvider;
    private final AcademicsDataProvider academicsProvider;
    private final AttendanceReportPdfGenerator reportPdfGenerator;
    private final SessionResolver sessionResolver;

    @Override
    public StudentCheckin recordEntry(Long studentId, Long scannerUserId) {
        Long classId = studentDataProvider.findCurrentClassId(studentId)
                .orElseThrow(() -> new BusinessRuleException("Élève sans classe courante : impossible de pointer"));
        assertScannerAllowed(classId, scannerUserId);

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        ScheduleSlotPublicInfo session = sessionResolver.currentSession(academicsProvider.findScheduleByClass(classId), today, now);
        if (session == null) {
            throw new BusinessRuleException("Aucun cours programmé aujourd'hui pour cette classe.");
        }
        StudentCheckin checkin = checkinRepository.findByStudentIdAndDateAndSlot(studentId, today, session.id())
                .orElseGet(() -> {
                    LocalTime start = LocalTime.parse(session.startTime());
                    LocalTime end = LocalTime.parse(session.endTime());
                    int late = (int) Math.max(0, Duration.between(start, now).toMinutes());
                    return checkinRepository.save(StudentCheckin.builder()
                            .tenantId(TenantContext.get()).studentId(studentId).classId(classId).checkinDate(today)
                            .scheduleSlotId(session.id()).sessionStart(start).sessionEnd(end)
                            .checkInTime(now).lateMinutes(late).createdAt(LocalDateTime.now()).build());
                });
        upsertDailyAttendance(studentId, classId, today);
        return checkin;
    }

    @Override
    public StudentCheckin recordExit(Long studentId, Long scannerUserId) {
        Long classId = studentDataProvider.findCurrentClassId(studentId)
                .orElseThrow(() -> new BusinessRuleException("Élève sans classe courante : impossible de pointer"));
        assertScannerAllowed(classId, scannerUserId);

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        ScheduleSlotPublicInfo session = sessionResolver.currentSession(academicsProvider.findScheduleByClass(classId), today, now);
        if (session == null) {
            throw new BusinessRuleException("Aucun cours programmé aujourd'hui pour cette classe.");
        }
        StudentCheckin checkin = checkinRepository.findByStudentIdAndDateAndSlot(studentId, today, session.id())
                .orElseThrow(() -> new BusinessRuleException(
                        "Aucun pointage d'entrée trouvé pour cette séance : scannez d'abord l'entrée."));
        checkin.setCheckOutTime(now);
        if (checkin.getSessionEnd() == null) checkin.setSessionEnd(LocalTime.parse(session.endTime()));
        return checkinRepository.save(checkin);
    }

    @Override
    public StudentCheckin recordManualEntry(Long studentId, LocalDate date, LocalTime checkInTime) {
        // Correction manuelle (saisie a posteriori, ex. appel papier) : contrairement au scan
        // (recordEntry, idempotent — le 1er scan fait foi), ici on écrase l'heure d'entrée existante
        // par la valeur saisie. Pas de vérification chef-de-classe : action réservée au staff (rôle
        // vérifié au niveau du contrôleur), pas un scan par un élève.
        Long classId = studentDataProvider.findCurrentClassId(studentId)
                .orElseThrow(() -> new BusinessRuleException("Élève sans classe courante : impossible de pointer"));
        ScheduleSlotPublicInfo session = sessionResolver.currentSession(academicsProvider.findScheduleByClass(classId), date, checkInTime);
        if (session == null) {
            throw new BusinessRuleException("Aucun cours programmé ce jour-là pour cette classe.");
        }
        LocalTime start = LocalTime.parse(session.startTime());
        LocalTime end = LocalTime.parse(session.endTime());
        int late = (int) Math.max(0, Duration.between(start, checkInTime).toMinutes());
        StudentCheckin checkin = checkinRepository.findByStudentIdAndDateAndSlot(studentId, date, session.id())
                .orElseGet(() -> StudentCheckin.builder()
                        .tenantId(TenantContext.get()).studentId(studentId).classId(classId).checkinDate(date)
                        .scheduleSlotId(session.id()).createdAt(LocalDateTime.now()).build());
        checkin.setSessionStart(start);
        checkin.setSessionEnd(end);
        checkin.setCheckInTime(checkInTime);
        checkin.setLateMinutes(late);
        StudentCheckin saved = checkinRepository.save(checkin);
        upsertDailyAttendance(studentId, classId, date);
        return saved;
    }

    @Override
    public StudentAttendance markAbsent(Long studentId, String reason) {
        Long classId = studentDataProvider.findCurrentClassId(studentId)
                .orElseThrow(() -> new BusinessRuleException("Élève sans classe courante : impossible de pointer"));
        LocalDate today = LocalDate.now();
        StudentAttendance saved = attendanceRepository.findByStudentIdAndClassIdAndDate(studentId, classId, today)
                .map(a -> { a.setStatus(AttendanceStatus.ABSENT); a.setReason(reason != null ? reason : "Absence signalée"); return attendanceRepository.save(a); })
                .orElseGet(() -> attendanceRepository.save(StudentAttendance.builder()
                        .tenantId(TenantContext.get()).studentId(studentId).classId(classId).date(today)
                        .status(AttendanceStatus.ABSENT).reason(reason != null ? reason : "Absence signalée").parentNotified(false).build()));
        publishIfAbsent(saved);
        return saved;
    }

    @Override @Transactional(readOnly = true)
    public List<StudentCheckin> findCheckins(Long studentId, LocalDate date) {
        return checkinRepository.findByStudentIdAndDate(studentId, date);
    }

    @Override
    public ChildPickup recordPickup(Long studentId, Long pickerParentId, String pickerName, String pickerRelation) {
        Long classId = studentDataProvider.findCurrentClassId(studentId)
                .orElseThrow(() -> new BusinessRuleException("Élève sans classe courante : impossible d'enregistrer le retrait"));
        String name = pickerName;
        if (pickerParentId != null) {
            name = studentDataProvider.findParentsByStudent(studentId).stream()
                    .filter(p -> pickerParentId.equals(p.id())).findFirst()
                    .map(p -> (p.firstName() + " " + p.lastName()).trim())
                    .orElseThrow(() -> new BusinessRuleException("Ce parent n'est pas lié à cet élève"));
        }
        if (name == null || name.isBlank()) {
            throw new BusinessRuleException("Le nom de la personne venue récupérer l'enfant est requis");
        }
        return childPickupRepository.save(ChildPickup.builder()
                .tenantId(TenantContext.get()).studentId(studentId).classId(classId)
                .pickupDate(LocalDate.now()).pickupTime(LocalTime.now())
                .pickerParentId(pickerParentId).pickerName(name).pickerRelation(pickerRelation)
                .createdAt(LocalDateTime.now()).build());
    }

    @Override @Transactional(readOnly = true)
    public List<ChildPickup> findPickups(Long studentId, LocalDate date) {
        return childPickupRepository.findByStudentIdAndDate(studentId, date);
    }

    @Override @Transactional(readOnly = true)
    public boolean requiresPickup(Long classId) {
        return academicsProvider.findClass(classId)
                .map(gn.lacaeducation.academics.ClassPublicInfo::schoolCycle)
                .map(c -> "MATERNELLE".equals(c) || "PRIMAIRE".equals(c))
                .orElse(false);
    }

    /** Si le compte qui scanne est lui-même un élève, il doit être le chef de classe désigné de CETTE
     * classe — le personnel (compte sans élève lié) n'a pas cette restriction (déjà filtré par rôle
     * au niveau du contrôleur). */
    private void assertScannerAllowed(Long classId, Long scannerUserId) {
        Optional<Long> scannerStudentId = scannerUserId != null
                ? studentDataProvider.findStudentIdByUserId(scannerUserId) : Optional.empty();
        if (scannerStudentId.isPresent()) {
            Long repStudentId = academicsProvider.findClass(classId)
                    .map(gn.lacaeducation.academics.ClassPublicInfo::classRepresentativeStudentId)
                    .orElse(null);
            if (repStudentId == null || !repStudentId.equals(scannerStudentId.get())) {
                throw new BusinessRuleException("Seul le chef de classe désigné peut scanner les badges de cette classe.");
            }
        }
    }

    /** Statut du jour toujours PRESENT depuis un scan (le retard reste représenté par lateMinutes > 0 —
     * un scan ne peut jamais produire ABSENT, voir {@link #markAbsent}). Notifie le parent en cas de
     * retard, comme avant (déclenché par le calcul automatique plutôt que par un clic manuel). */
    private void upsertDailyAttendance(Long studentId, Long classId, LocalDate today) {
        LocalTime firstCheckIn = checkinRepository.findByStudentIdAndDate(studentId, today).stream()
                .map(StudentCheckin::getCheckInTime).filter(Objects::nonNull)
                .min(Comparator.naturalOrder()).orElse(null);
        int dayLate = checkinRepository.findByStudentIdAndDate(studentId, today).stream()
                .mapToInt(c -> c.getLateMinutes() != null ? c.getLateMinutes() : 0).sum();
        StudentAttendance saved = attendanceRepository.findByStudentIdAndClassIdAndDate(studentId, classId, today)
                .map(a -> {
                    a.setStatus(AttendanceStatus.PRESENT);
                    a.setCheckInTime(firstCheckIn);
                    a.setLateMinutes(dayLate);
                    return attendanceRepository.save(a);
                })
                .orElseGet(() -> attendanceRepository.save(StudentAttendance.builder()
                        .tenantId(TenantContext.get()).studentId(studentId).classId(classId).date(today)
                        .status(AttendanceStatus.PRESENT).reason("Scan carte scolaire").parentNotified(false)
                        .checkInTime(firstCheckIn).lateMinutes(dayLate).build()));
        // Statut toujours PRESENT depuis un scan — le retard est signalé au parent via lateMinutes,
        // pas via le statut (qui ne devient LATE que pour une saisie manuelle, cf. record()/recordBulk()).
        if (dayLate > 0) {
            eventPublisher.publishEvent(new StudentAbsentEvent(
                    saved.getId(), saved.getTenantId(), saved.getStudentId(), saved.getClassId(), saved.getDate()));
        }
    }

    @Override
    public String studentName(Long studentId) {
        return studentDataProvider.findStudent(studentId)
                .map(s -> (s.firstName() + " " + s.lastName()).trim()).orElse(null);
    }

    @Override
    public StudentAttendance record(RecordAttendanceCommand cmd) {
        // One attendance per student, class and date.
        attendanceRepository.findByStudentIdAndClassIdAndDate(cmd.studentId(), cmd.classId(), cmd.date())
                .ifPresent(e -> { throw new ConflictException(
                        "Attendance already recorded for student " + cmd.studentId()
                                + " in class " + cmd.classId() + " on " + cmd.date()); });
        StudentAttendance a = attendanceRepository.save(StudentAttendance.builder()
                .tenantId(TenantContext.get())
                .studentId(cmd.studentId()).classId(cmd.classId()).date(cmd.date())
                .status(cmd.status()).reason(cmd.reason()).parentNotified(false).build());
        publishIfAbsent(a);
        return a;
    }

    private void publishIfAbsent(StudentAttendance a) {
        if (a.getStatus() == AttendanceStatus.ABSENT || a.getStatus() == AttendanceStatus.LATE) {
            eventPublisher.publishEvent(new StudentAbsentEvent(
                    a.getId(), a.getTenantId(), a.getStudentId(), a.getClassId(), a.getDate()));
        }
    }
    @Override
    public List<StudentAttendance> recordBulk(BulkAttendanceCommand cmd) {
        List<StudentAttendance> results = new ArrayList<>();
        for (AttendanceEntry entry : cmd.attendances()) {
            Optional<StudentAttendance> existing = attendanceRepository.findByStudentIdAndClassIdAndDate(
                    entry.studentId(), cmd.classId(), cmd.date());
            StudentAttendance saved;
            if (existing.isPresent()) {
                StudentAttendance a = existing.get();
                a.setStatus(entry.status());
                a.setReason(entry.reason());
                saved = attendanceRepository.save(a);
            } else {
                saved = attendanceRepository.save(StudentAttendance.builder()
                        .tenantId(TenantContext.get())
                        .studentId(entry.studentId()).classId(cmd.classId()).date(cmd.date())
                        .status(entry.status()).reason(entry.reason()).parentNotified(false).build());
            }
            publishIfAbsent(saved);
            results.add(saved);
        }
        return results;
    }
    @Override
    public StudentAttendance update(Long id, AttendanceStatus status, String reason) {
        StudentAttendance a = load(id);
        a.setStatus(status); a.setReason(reason);
        return attendanceRepository.save(a);
    }
    @Override @Transactional(readOnly = true)
    public StudentAttendance findById(Long id) { return load(id); }
    @Override @Transactional(readOnly = true)
    public List<StudentAttendance> findByClass(Long classId, LocalDate date) { return attendanceRepository.findByClassIdAndDate(classId, date); }
    @Override @Transactional(readOnly = true)
    public List<StudentAttendance> findByStudent(Long studentId, LocalDate from, LocalDate to) { return attendanceRepository.findByStudentIdAndDateBetween(studentId, from, to); }
    @Override @Transactional(readOnly = true)
    public List<StudentAttendance> findAbsences(Long classId, LocalDate from, LocalDate to) { return attendanceRepository.findByClassIdAndDateBetweenAndStatus(classId, from, to, AttendanceStatus.ABSENT); }
    @Override @Transactional(readOnly = true)
    public AttendanceStatsResponse getStats(Long studentId, LocalDate from, LocalDate to) {
        LocalDate effectiveFrom = from != null ? from : LocalDate.now().withDayOfYear(1);
        LocalDate effectiveTo = to != null ? to : LocalDate.now();
        long total = attendanceRepository.countByStudentIdAndDateBetween(studentId, effectiveFrom, effectiveTo);
        long absent = attendanceRepository.countByStudentIdAndDateBetweenAndStatus(studentId, effectiveFrom, effectiveTo, AttendanceStatus.ABSENT);
        long late = attendanceRepository.countByStudentIdAndDateBetweenAndStatus(studentId, effectiveFrom, effectiveTo, AttendanceStatus.LATE);
        long present = total - absent - late;
        BigDecimal rate = total == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(present).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        return new AttendanceStatsResponse(studentId, total, present, absent, late, rate);
    }
    @Override
    public StudentAttendance justify(Long id, Boolean justified, String reason, String justificationDoc) {
        StudentAttendance a = load(id);
        a.setJustified(justified);
        if (reason != null) a.setReason(reason);
        a.setJustificationDoc(justificationDoc);
        return attendanceRepository.save(a);
    }

    @Override @Transactional(readOnly = true)
    public List<UnjustifiedAlert> unjustifiedAlerts(Long classId, LocalDate from, LocalDate to, int threshold) {
        LocalDate f = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate t = to != null ? to : LocalDate.now();
        int min = threshold > 0 ? threshold : 2;
        java.util.Map<Long, Long> counts = new java.util.HashMap<>();
        for (StudentAttendance a : attendanceRepository.findByClassIdAndDateBetweenAndStatus(classId, f, t, AttendanceStatus.ABSENT)) {
            if (!Boolean.TRUE.equals(a.getJustified())) {
                counts.merge(a.getStudentId(), 1L, Long::sum);
            }
        }
        return counts.entrySet().stream()
                .filter(e -> e.getValue() >= min)
                .map(e -> new UnjustifiedAlert(e.getKey(), e.getValue()))
                .toList();
    }

    @Override public void delete(Long id) { attendanceRepository.deleteById(id); }
    @Override
    public void markParentNotified(Long id) {
        StudentAttendance a = load(id); a.setParentNotified(true); attendanceRepository.save(a);
    }
    private StudentAttendance load(Long id) { return attendanceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("StudentAttendance", id)); }

    @Override
    @Transactional(readOnly = true)
    public byte[] absenteeListPdf(Long classId, LocalDate date) {
        List<AbsenteeRow> rows = attendanceRepository.findByClassIdAndDate(classId, date).stream()
                .filter(a -> a.getStatus() == AttendanceStatus.ABSENT || a.getStatus() == AttendanceStatus.LATE)
                .sorted(java.util.Comparator.comparing(a -> nameOrId(a.getStudentId()), String.CASE_INSENSITIVE_ORDER))
                .map(a -> new AbsenteeRow(nameOrId(a.getStudentId()), statusLabel(a.getStatus()),
                        Boolean.TRUE.equals(a.getJustified()), a.getReason()))
                .toList();
        return reportPdfGenerator.absenteeList(new AbsenteeList(TenantContext.get(), className(classId), date, rows));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] latenessReportPdf(Long classId, LocalDate from, LocalDate to) {
        var lateByStudent = attendanceRepository
                .findByClassIdAndDateBetweenAndStatus(classId, from, to, AttendanceStatus.LATE).stream()
                .collect(java.util.stream.Collectors.groupingBy(StudentAttendance::getStudentId,
                        java.util.LinkedHashMap::new, java.util.stream.Collectors.toList()));
        List<LatenessRow> rows = lateByStudent.entrySet().stream()
                .map(e -> new LatenessRow(nameOrId(e.getKey()), e.getValue().size(),
                        e.getValue().stream().map(a -> a.getDate() == null ? "" : a.getDate().toString())
                                .filter(s -> !s.isBlank()).collect(java.util.stream.Collectors.joining(", "))))
                .sorted(java.util.Comparator.comparingInt(LatenessRow::count).reversed())
                .toList();
        return reportPdfGenerator.latenessReport(new LatenessReport(TenantContext.get(), className(classId), from, to, rows));
    }

    private String nameOrId(Long studentId) {
        String n = studentName(studentId);
        return n != null && !n.isBlank() ? n : "Élève #" + studentId;
    }

    private String className(Long classId) {
        return academicsProvider.findClass(classId)
                .map(c -> c.levelLabel() != null && !c.levelLabel().isBlank() ? c.label() + " (" + c.levelLabel() + ")" : c.label())
                .orElse("Classe #" + classId);
    }

    private static String statusLabel(AttendanceStatus s) {
        return s == AttendanceStatus.LATE ? "Retard" : s == AttendanceStatus.ABSENT ? "Absent" : "Présent";
    }
}
