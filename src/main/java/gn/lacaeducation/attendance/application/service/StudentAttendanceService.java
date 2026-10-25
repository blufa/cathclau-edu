package gn.lacaeducation.attendance.application.service;
import gn.lacaeducation.attendance.StudentAttendanceNotifier;
import gn.lacaeducation.attendance.application.event.StudentAbsentEvent;
import gn.lacaeducation.attendance.application.event.StudentExitEvent;
import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.attendance.application.port.in.StudentAttendanceUseCase;
import gn.lacaeducation.attendance.application.port.out.AttendanceReportPdfGenerator;
import gn.lacaeducation.attendance.application.port.out.AttendanceReportPdfGenerator.AbsenteeList;
import gn.lacaeducation.attendance.application.port.out.AttendanceReportPdfGenerator.AbsenteeRow;
import gn.lacaeducation.attendance.application.port.out.AttendanceReportPdfGenerator.LatenessReport;
import gn.lacaeducation.attendance.application.port.out.AttendanceReportPdfGenerator.LatenessRow;
import gn.lacaeducation.attendance.application.port.out.AttendanceSettingsRepository;
import gn.lacaeducation.attendance.application.port.out.ChildPickupRepository;
import gn.lacaeducation.attendance.application.port.out.RecessWindowRepository;
import gn.lacaeducation.attendance.application.port.out.StudentAttendanceRepository;
import gn.lacaeducation.attendance.application.port.out.StudentCheckinRepository;
import gn.lacaeducation.attendance.domain.AttendanceSettings;
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
    private final AttendanceSettingsRepository settingsRepository;
    private final RecessWindowRepository recessWindowRepository;

    /**
     * Heure à partir de laquelle un retard commence à être compté pour une entrée sur {@code session} :
     * l'heure de début prévue, repoussée par la marge de tolérance PROPRE AUX ÉLÈVES (§réglages —
     * distincte de celle des enseignants/personnel : le temps de trajet d'un enseignant entre deux
     * salles n'a pas de raison de suivre la même tolérance qu'un élève) et/ou une fenêtre de
     * récréation. Même mécanisme que pour l'enseignant (voir
     * {@code TeacherAttendanceService#graceDeadline}), marge différente.
     */
    private LocalTime graceDeadline(LocalTime sessionStart) {
        return sessionResolver.graceDeadline(sessionStart, transitionMargin(), recessWindowRepository.findByTenantId(TenantContext.get()));
    }

    private int transitionMargin() {
        return settingsRepository.findByTenantId(TenantContext.get())
                .map(AttendanceSettings::studentTransitionMarginMinutesOrDefault)
                .orElseGet(() -> AttendanceSettings.defaults().studentTransitionMarginMinutesOrDefault());
    }

    /** Fenêtre de course réseau (§16) : une ligne créée il y a moins de {@code RACE_WINDOW_SECONDS}
     * vient très probablement de la retentative du MÊME tap physique (voir javadoc de recordEntry),
     * pas d'un second scan humain — un vrai re-scan arrive toujours bien après (au minimum le temps
     * de refaire le trajet jusqu'au lecteur). */
    private static final long RACE_WINDOW_SECONDS = 5;

    private boolean createdWithinRaceWindow(StudentCheckin checkin) {
        return checkin.getCreatedAt() != null
                && Duration.between(checkin.getCreatedAt(), LocalDateTime.now()).getSeconds() < RACE_WINDOW_SECONDS;
    }

    @Override
    public StudentCheckin recordEntry(Long studentId, Long scannerUserId) {
        Long classId = studentDataProvider.findCurrentClassId(studentId)
                .orElseThrow(() -> new BusinessRuleException("Élève sans classe courante : impossible de pointer"));
        assertScannerAllowed(classId, scannerUserId);

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        SessionResolver.Block block = sessionResolver.currentBlock(academicsProvider.findScheduleByClass(classId), today, now, transitionMargin());
        if (block == null) {
            throw new BusinessRuleException("Aucun cours programmé aujourd'hui pour cette classe.");
        }
        // Garde-fou par ÉTAT, jamais par compteur journalier (§retour terrain — un enfant malade qui
        // sort puis revient de l'hôpital doit pouvoir repointer : cycles entrée/sortie illimités par
        // jour) : seul le pointage le plus récent du jour décide. S'il est encore OUVERT (pas de
        // sortie), toute nouvelle entrée est refusée — scannez la sortie avant de réentrer. S'il est
        // CLOS (entrée + sortie déjà faites), on tombe plus bas pour créer un nouveau pointage, jamais
        // un rejet. Exception technique : une requête qui retrouve la ligne qu'ELLE-MÊME vient de
        // créer une fraction de seconde plus tôt (retentative réseau du même appel HTTP, voir
        // AttendanceScanController#idempotent, ou badge qui reste visible plusieurs frames à la
        // caméra) n'est pas un second scan humain — cette ligne est renvoyée telle quelle, marquée
        // duplicate (invisible pour l'utilisateur, voir AttendanceScanController#response).
        Optional<StudentCheckin> existing = checkinRepository.findByStudentIdAndDate(studentId, today).stream()
                .max(Comparator.comparing(StudentCheckin::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder())));
        if (existing.isPresent()) {
            StudentCheckin last = existing.get();
            if (createdWithinRaceWindow(last)) {
                // Retentative réseau du même tap physique (ou badge qui reste visible à la caméra
                // plusieurs frames de suite) — jamais un nouveau scan humain : marqué duplicate pour
                // que le frontend n'affiche/vibre qu'une seule fois par scan physique (§retour terrain).
                last.setDuplicate(true);
                return last;
            }
            if (last.getCheckOutTime() == null) {
                throw new BusinessRuleException(
                        "Ce badge a déjà été scanné à l'entrée aujourd'hui. Scannez-le à la sortie avant un nouvel accès.");
            }
            // Dernier pointage du jour déjà clos (entrée + sortie) : cycles illimités par jour
            // (§retour terrain — un enfant malade qui sort puis revient doit pouvoir repointer), on
            // tombe en dessous pour créer un NOUVEAU pointage, jamais un rejet.
        }
        LocalTime start = block.start();
        LocalTime end = block.end();
        LocalTime deadline = graceDeadline(start);
        int late = (int) Math.max(0, Duration.between(deadline, now).toMinutes());
        StudentCheckin checkin = checkinRepository.save(StudentCheckin.builder()
                .tenantId(TenantContext.get()).studentId(studentId).classId(classId).checkinDate(today)
                .scheduleSlotId(block.firstSlotId()).sessionStart(start).sessionEnd(end)
                .checkInTime(now).lateMinutes(late).createdAt(LocalDateTime.now()).build());
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
        SessionResolver.Block block = sessionResolver.currentBlock(academicsProvider.findScheduleByClass(classId), today, now, transitionMargin());
        if (block == null) {
            throw new BusinessRuleException("Aucun cours programmé aujourd'hui pour cette classe.");
        }
        // Verrou pessimiste (§15 : "le SMS doit être envoyé une seule fois... malgré les reprises") :
        // contrairement à l'entrée (protégée par la contrainte d'unicité à l'insertion), rien n'empêche
        // nativement deux scans de sortie quasi simultanés de mettre à jour la MÊME ligne en parallèle
        // et de publier chacun leur StudentExitEvent (deux SMS avec des heures légèrement différentes).
        // Porte sur le pointage le plus RÉCENT du jour entier (pas un créneau précis) — cycles
        // entrée/sortie illimités par jour (§retour terrain, voir recordEntry) : c'est toujours le
        // dernier pointage créé qui est le cycle en cours à fermer.
        StudentCheckin checkin = checkinRepository.lockOpenByStudentIdAndDate(studentId, today)
                .orElseThrow(() -> new BusinessRuleException(
                        "Aucun pointage d'entrée trouvé aujourd'hui : scannez d'abord l'entrée."));
        if (checkin.getCheckOutTime() != null) {
            // Déjà sorti aujourd'hui (second scan de sortie, retentative réseau…) : on renvoie le
            // pointage existant SANS écraser l'heure de sortie ni republier l'événement — sinon chaque
            // nouveau scan de sortie décalerait silencieusement l'heure enregistrée et renverrait un
            // second SMS avec une durée différente. Marqué duplicate (§recordEntry) pour que le
            // frontend n'affiche/vibre qu'une seule fois par scan physique.
            checkin.setDuplicate(true);
            return checkin;
        }
        checkin.setCheckOutTime(now);
        if (checkin.getSessionEnd() == null) checkin.setSessionEnd(block.end());
        // Départ anticipé : sortie pointée avant la fin prévue de la séance (symétrique du retard à
        // l'entrée, miroir de TeacherAttendanceService#recordExit).
        int early = (int) Math.max(0, Duration.between(now, checkin.getSessionEnd()).toMinutes());
        checkin.setEarlyMinutes(early);
        StudentCheckin saved = checkinRepository.save(checkin);

        // §retour fondateurs : notifie le parent à la sortie avec la durée passée à l'école — depuis
        // le PREMIER pointage du jour (pas forcément celui de ce créneau), pour refléter la journée
        // entière (6h/8h selon l'emploi du temps de l'élève), même si plusieurs créneaux ont été
        // pointés séparément (ex. sortie/retour de pause).
        StudentCheckin firstCheckin = checkinRepository.findByStudentIdAndDate(studentId, today).stream()
                .filter(c -> c.getCheckInTime() != null)
                .min(Comparator.comparing(StudentCheckin::getCheckInTime)).orElse(saved);
        int minutesAtSchool = (int) Math.max(0, Duration.between(firstCheckin.getCheckInTime(), now).toMinutes());
        eventPublisher.publishEvent(new StudentExitEvent(TenantContext.get(), studentId, classId, today,
                firstCheckin.getCheckInTime(), now, minutesAtSchool));
        return saved;
    }

    @Override
    public StudentCheckin recordManualExit(Long studentId, LocalDate date, LocalTime checkOutTime) {
        // Symétrique de recordManualEntry : crée le pointage si absent (badge oublié toute la séance),
        // sinon complète le pointage d'entrée déjà existant avec l'heure de sortie saisie.
        Long classId = studentDataProvider.findCurrentClassId(studentId)
                .orElseThrow(() -> new BusinessRuleException("Élève sans classe courante : impossible de pointer"));
        SessionResolver.Block block = sessionResolver.currentBlock(academicsProvider.findScheduleByClass(classId), date, checkOutTime, transitionMargin());
        if (block == null) {
            throw new BusinessRuleException("Aucun cours programmé ce jour-là pour cette classe.");
        }
        LocalTime start = block.start();
        LocalTime end = block.end();
        StudentCheckin checkin = checkinRepository.findByStudentIdAndDateAndSlot(studentId, date, block.firstSlotId())
                .orElseGet(() -> StudentCheckin.builder()
                        .tenantId(TenantContext.get()).studentId(studentId).classId(classId).checkinDate(date)
                        .scheduleSlotId(block.firstSlotId()).sessionStart(start).lateMinutes(0).createdAt(LocalDateTime.now()).build());
        checkin.setSessionEnd(end);
        checkin.setCheckOutTime(checkOutTime);
        int early = (int) Math.max(0, Duration.between(checkOutTime, end).toMinutes());
        checkin.setEarlyMinutes(early);
        StudentCheckin saved = checkinRepository.save(checkin);

        // Même notification que recordExit (§retour fondateurs) — cohérent avec recordManualEntry,
        // qui notifie déjà lui aussi via upsertDailyAttendance quelle que soit la date saisie.
        // `checkInTime` peut rester null ici (badge oublié toute la séance, aucune entrée connue) —
        // contrairement à recordExit, où l'entrée est obligatoire (voir son orElseThrow juste avant).
        LocalTime firstCheckInTime = checkinRepository.findByStudentIdAndDate(studentId, date).stream()
                .map(StudentCheckin::getCheckInTime).filter(Objects::nonNull)
                .min(LocalTime::compareTo).orElse(null);
        int minutesAtSchool = firstCheckInTime != null
                ? (int) Math.max(0, Duration.between(firstCheckInTime, checkOutTime).toMinutes()) : 0;
        eventPublisher.publishEvent(new StudentExitEvent(TenantContext.get(), studentId, classId, date,
                firstCheckInTime, checkOutTime, minutesAtSchool));
        return saved;
    }

    @Override
    public StudentCheckin recordManualEntry(Long studentId, LocalDate date, LocalTime checkInTime) {
        // Correction manuelle (saisie a posteriori, ex. appel papier) : contrairement au scan
        // (recordEntry, idempotent — le 1er scan fait foi), ici on écrase l'heure d'entrée existante
        // par la valeur saisie. Pas de vérification chef-de-classe : action réservée au staff (rôle
        // vérifié au niveau du contrôleur), pas un scan par un élève.
        Long classId = studentDataProvider.findCurrentClassId(studentId)
                .orElseThrow(() -> new BusinessRuleException("Élève sans classe courante : impossible de pointer"));
        SessionResolver.Block block = sessionResolver.currentBlock(academicsProvider.findScheduleByClass(classId), date, checkInTime, transitionMargin());
        if (block == null) {
            throw new BusinessRuleException("Aucun cours programmé ce jour-là pour cette classe.");
        }
        LocalTime start = block.start();
        LocalTime end = block.end();
        LocalTime deadline = graceDeadline(start);
        int late = (int) Math.max(0, Duration.between(deadline, checkInTime).toMinutes());
        StudentCheckin checkin = checkinRepository.findByStudentIdAndDateAndSlot(studentId, date, block.firstSlotId())
                .orElseGet(() -> StudentCheckin.builder()
                        .tenantId(TenantContext.get()).studentId(studentId).classId(classId).checkinDate(date)
                        .scheduleSlotId(block.firstSlotId()).createdAt(LocalDateTime.now()).build());
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

    @Override @Transactional(readOnly = true)
    public List<StudentCheckin> findCheckinsByClass(Long classId, LocalDate date) {
        return checkinRepository.findByClassIdAndDate(classId, date);
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
                .map(c -> "GARDERIE".equals(c) || "MATERNELLE".equals(c) || "PRIMAIRE".equals(c))
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
     * un scan ne peut jamais produire ABSENT, voir {@link #markAbsent}). Notifie le parent par SMS à
     * chaque premier scan du jour (arrivée confirmée), qu'il soit à l'heure ou en retard — un second
     * scan le même jour (ex. retour après la pause) ne renvoie pas de notification. */
    private void upsertDailyAttendance(Long studentId, Long classId, LocalDate today) {
        // Retard = celui du PREMIER pointage du jour uniquement (l'arrivée du matin) — jamais la somme
        // des lateMinutes de chaque créneau/période scanné dans la journée : un élève pointant à
        // plusieurs séances (matin, retour de pause…) voyait sinon son « retard » gonfler à chaque
        // nouveau scan (ex. 351 min cumulées sur la journée pour un retard réel de 20 min le matin).
        StudentCheckin firstCheckin = checkinRepository.findByStudentIdAndDate(studentId, today).stream()
                .filter(c -> c.getCheckInTime() != null)
                .min(Comparator.comparing(StudentCheckin::getCheckInTime)).orElse(null);
        LocalTime firstCheckIn = firstCheckin != null ? firstCheckin.getCheckInTime() : null;
        int dayLate = firstCheckin != null && firstCheckin.getLateMinutes() != null ? firstCheckin.getLateMinutes() : 0;
        Optional<StudentAttendance> existing = attendanceRepository.findByStudentIdAndClassIdAndDate(studentId, classId, today);
        boolean firstScanOfDay = existing.isEmpty();
        StudentAttendance saved = existing
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
        // Notification uniquement au premier scan du jour — un scan suivant (retour de pause, autre
        // période) ne doit jamais redéclencher un SMS au parent, déjà prévenu de l'arrivée.
        if (firstScanOfDay) {
            String status = dayLate > 0 ? AttendanceStatus.LATE.name() : AttendanceStatus.PRESENT.name();
            eventPublisher.publishEvent(new StudentAbsentEvent(saved.getId(), saved.getTenantId(),
                    saved.getStudentId(), saved.getClassId(), saved.getDate(), status, dayLate, firstCheckIn));
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
                    a.getId(), a.getTenantId(), a.getStudentId(), a.getClassId(), a.getDate(),
                    a.getStatus().name(), a.getLateMinutes(), a.getCheckInTime()));
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

    @Override
    public void notifyParentNow(Long id) {
        // Republie StudentAbsentEvent (mêmes écouteurs que l'envoi automatique : SMS/email via
        // communication, email via notification) — PAS d'appel à markParentNotified() ici, pour éviter
        // toute boucle : DomainEventListener (notification) appelle déjà markParentNotified() en retour
        // quand son propre envoi réussit ; réutiliser cette méthode republierait l'événement à l'infini.
        StudentAttendance a = load(id);
        eventPublisher.publishEvent(new StudentAbsentEvent(a.getId(), a.getTenantId(), a.getStudentId(), a.getClassId(),
                a.getDate(), a.getStatus().name(), a.getLateMinutes(), a.getCheckInTime()));
        a.setParentNotified(true);
        attendanceRepository.save(a);
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
