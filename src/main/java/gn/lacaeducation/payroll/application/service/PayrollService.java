package gn.lacaeducation.payroll.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.academics.ClassPublicInfo;
import gn.lacaeducation.academics.ScheduleSlotPublicInfo;
import gn.lacaeducation.attendance.TeacherWorkProvider;
import gn.lacaeducation.calendar.CalendarDataProvider;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ConflictException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.lessons.TeacherTeachingProvider;
import gn.lacaeducation.payroll.application.port.out.PayslipLineRepository;
import gn.lacaeducation.payroll.application.port.out.PayslipRepository;
import gn.lacaeducation.payroll.application.port.out.SalaryRuleRepository;
import gn.lacaeducation.payroll.application.port.out.SalaryStructureRepository;
import gn.lacaeducation.payroll.application.port.out.TeacherCompensationRepository;
import gn.lacaeducation.payroll.application.event.PayslipPaidEvent;
import gn.lacaeducation.payroll.domain.*;
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
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Paie (inspiré OpenEduCat Payroll) : règles de paie, structures salariales, génération de fiches
 * (individuelle ou en lot) avec calcul base → brut → net, puis workflow DRAFT → VALIDATED → PAID.
 * Le salaire de base provient de la rémunération de la fiche enseignant (transmis par l'appelant) ;
 * aucune dépendance directe au module staff (le module iam, lui, est utilisé — voir
 * {@link #resolveAccountantName} — pour résoudre le nom du comptable ayant payé une fiche, une
 * préoccupation différente de « connaître le personnel enseignant/administratif »).
 */
@lombok.extern.slf4j.Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PayrollService {

    private final PayslipRepository payslipRepo;
    private final PayslipLineRepository lineRepo;
    private final SalaryStructureRepository structureRepo;
    private final SalaryRuleRepository ruleRepo;
    private final gn.lacaeducation.payroll.application.port.out.PayslipPdfGenerator pdfGenerator;
    private final TeacherCompensationRepository compRepo;
    private final gn.lacaeducation.payroll.application.port.out.TeacherVacationPayRepository vacationPayRepo;
    private final TeacherTeachingProvider teachingProvider;
    private final TeacherWorkProvider workProvider;
    private final gn.lacaeducation.payroll.application.port.in.SalaryFeeDeductionUseCase salaryFeeDeductionUseCase;
    private final gn.lacaeducation.payroll.application.port.in.SalaryAdvanceUseCase salaryAdvanceUseCase;
    private final AcademicsDataProvider academics;
    private final CalendarDataProvider calendar;
    private final ApplicationEventPublisher eventPublisher;
    private final gn.lacaeducation.iam.UserPublicInfoProvider userPublicInfoProvider;
    private final TeacherPublicInfoProvider teacherPublicInfoProvider;
    private final gn.lacaeducation.staff.EmployeePublicInfoProvider employeePublicInfoProvider;

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    // ── Rémunération (mode horaire / fixe) & calcul mensuel ───────────────────

    public record MonthlyPay(Long teacherId, int year, int month, String payMode,
                             int realizedMinutes, int lateMinutes, int earlyMinutes, int effectiveMinutes,
                             int normalEffectiveMinutes, int examEffectiveMinutes,
                             BigDecimal hourlyRateNormal, BigDecimal hourlyRateExam,
                             BigDecimal fixedSalary, BigDecimal teachingPay, BigDecimal grossPay) {}

    public TeacherCompensation setCompensation(Long tenantId, Long teacherId, PayMode mode,
                                                BigDecimal hourlyRateNormal, BigDecimal hourlyRateExam, BigDecimal fixedSalary) {
        TeacherCompensation c = compRepo.findByTeacherId(teacherId)
                .orElseGet(() -> TeacherCompensation.builder().tenantId(tenantId).teacherId(teacherId).build());
        c.setPayMode(mode != null ? mode : PayMode.HOURLY);
        c.setHourlyRateNormal(hourlyRateNormal != null ? hourlyRateNormal : BigDecimal.ZERO);
        c.setHourlyRateExam(hourlyRateExam != null ? hourlyRateExam : BigDecimal.ZERO);
        c.setFixedSalary(fixedSalary != null ? fixedSalary : BigDecimal.ZERO);
        return compRepo.save(c);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<TeacherCompensation> compensation(Long teacherId) { return compRepo.findByTeacherId(teacherId); }

    @Transactional(readOnly = true)
    public List<TeacherCompensation> compensations(Long tenantId) { return compRepo.findByTenantId(tenantId); }

    /** Marque un mois précis (année + mois) comme payé pendant les vacances pour cet enseignant —
     * avantage accordé au cas par cas par la direction (§6), pas un interrupteur valable toute
     * l'année. Idempotent (repose sur la contrainte d'unicité teacher/year/month). */
    public gn.lacaeducation.payroll.domain.TeacherVacationPayMonth addVacationPayMonth(Long tenantId, Long teacherId, int year, int month) {
        if (vacationPayRepo.existsByTeacherIdAndYearAndMonth(teacherId, year, month)) {
            return vacationPayRepo.findByTeacherId(teacherId).stream()
                    .filter(m -> m.getYear() == year && m.getMonth() == month).findFirst().orElseThrow();
        }
        return vacationPayRepo.save(gn.lacaeducation.payroll.domain.TeacherVacationPayMonth.builder()
                .tenantId(tenantId).teacherId(teacherId).year(year).month(month).build());
    }

    public void removeVacationPayMonth(Long teacherId, Long id) {
        boolean owned = vacationPayRepo.findByTeacherId(teacherId).stream().anyMatch(m -> m.getId().equals(id));
        if (!owned) throw new gn.lacaeducation.common.BusinessRuleException("Mois de congé payé introuvable pour cet enseignant.");
        vacationPayRepo.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<gn.lacaeducation.payroll.domain.TeacherVacationPayMonth> listVacationPayMonths(Long teacherId) {
        return vacationPayRepo.findByTeacherId(teacherId);
    }

    /** Vrai si la classe est de typologie "classe d'examen" (tarif horaire dédié, §6) ; false si
     * introuvable (classe supprimée depuis) — repli par défaut sur le tarif normal. */
    private boolean isExamClass(Long classId, Map<Long, ClassPublicInfo> classCache) {
        if (classId == null) return false;
        ClassPublicInfo info = classCache.computeIfAbsent(classId, id -> academics.findClass(id).orElse(null));
        return info != null && "EXAMEN".equals(info.classType());
    }

    /**
     * Paie mensuelle basée sur les heures RÉALISÉES (séances « réalisées » du cahier de textes) —
     * exactement la durée choisie dans le modal de confirmation de chaque séance (durée théorique de
     * l'emploi du temps, OU durée réelle pointée entrée/sortie) : ce choix, fait séance par séance par
     * la personne qui valide, est la seule source de vérité pour la paie, jamais recalculée ou corrigée
     * ensuite. Retards et départs anticipés (pointage) restent calculés et affichés sur la fiche de
     * paie à titre d'information, mais ne sont plus déduits du temps réalisé : les déduire aurait
     * pénalisé deux fois le même retard quand la durée RÉELLE pointée est choisie (elle l'exclut déjà,
     * puisqu'elle part de l'heure d'arrivée effective), et aurait contredit le choix explicite de payer
     * la durée THÉORIQUE complète quand celle-ci est retenue à la place.
     * Réparti par typologie de classe (normal/examen, §6) pour appliquer le bon tarif à chaque part —
     * un enseignant qui donne cours à la fois en classe normale et en classe d'examen est ainsi payé
     * correctement sur chaque partie. Horaire : heures réalisées × tarif (normal + examen) ; Fixe :
     * salaire fixe + heures de cours réalisées × tarif (cas direction qui enseigne aussi).
     */
    @Transactional(readOnly = true)
    public MonthlyPay computeMonthlyPay(Long teacherId, int year, int month) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        TeacherCompensation comp = compRepo.findByTeacherId(teacherId).orElse(null);
        Long tenantId = comp != null && comp.getTenantId() != null ? comp.getTenantId() : TenantContext.get();

        Map<Long, Integer> realizedByClass = new HashMap<>(dedupedRealizedByClass(teacherId, from, to));
        // Jour férié (chômé et payé par l'État, calendrier académique §calendar) : TOUJOURS payé sur les
        // heures touchées, automatiquement, sans action de la direction.
        for (var e : paidHolidayCoveredMinutesByClass(teacherId, tenantId, from, to).entrySet()) {
            realizedByClass.merge(e.getKey(), e.getValue(), Integer::sum);
        }
        // Événement école (kermesse, match, entraînement... §academics, décidé par la direction) qui a
        // empêché le cours sur une période : les heures des créneaux concernés ne restent payées que si
        // la direction a explicitement crédité cet enseignant pour CET événement (voir
        // SchoolEventService#setCreditedTeachers) — jamais automatique, contrairement à un jour férié.
        for (var e : eventCoveredMinutesByClass(teacherId, tenantId, from, to).entrySet()) {
            realizedByClass.merge(e.getKey(), e.getValue(), Integer::sum);
        }
        Map<Long, TeacherWorkProvider.ClassLateEarly> lateEarlyByClass = workProvider.monthlyLateEarlyByClass(teacherId, year, month);

        Map<Long, ClassPublicInfo> classCache = new HashMap<>();
        int realized = 0, late = 0, early = 0;
        int normalRealized = 0, examRealized = 0;
        for (var e : realizedByClass.entrySet()) {
            int minutes = e.getValue();
            realized += minutes;
            if (isExamClass(e.getKey(), classCache)) examRealized += minutes; else normalRealized += minutes;
        }
        // Retard/départ anticipé : uniquement informatifs sur la fiche de paie (voir javadoc ci-dessus),
        // plus jamais soustraits de la durée réalisée qui sert de base au calcul du salaire.
        for (var e : lateEarlyByClass.entrySet()) {
            late += e.getValue().lateMinutes();
            early += e.getValue().earlyMinutes();
        }
        int normalEffective = normalRealized;
        int examEffective = examRealized;
        int effective = normalEffective + examEffective;

        PayMode mode = comp != null && comp.getPayMode() != null ? comp.getPayMode() : PayMode.HOURLY;
        BigDecimal rateNormal = comp != null && comp.getHourlyRateNormal() != null ? comp.getHourlyRateNormal() : BigDecimal.ZERO;
        BigDecimal rateExam = comp != null && comp.getHourlyRateExam() != null ? comp.getHourlyRateExam() : BigDecimal.ZERO;
        BigDecimal fixed = comp != null && comp.getFixedSalary() != null ? comp.getFixedSalary() : BigDecimal.ZERO;

        BigDecimal normalHours = BigDecimal.valueOf(normalEffective).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        BigDecimal examHours = BigDecimal.valueOf(examEffective).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        BigDecimal teachingPay = normalHours.multiply(rateNormal).add(examHours.multiply(rateExam)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal gross = mode == PayMode.FIXED ? fixed.add(teachingPay) : teachingPay;
        return new MonthlyPay(teacherId, year, month, mode.name(), realized, late, early, effective,
                normalEffective, examEffective, rateNormal, rateExam, fixed, teachingPay, gross);
    }

    /**
     * Minutes des créneaux récurrents d'un enseignant qui tombent dans un événement école (kermesse,
     * match, entraînement... §academics, décidé par la direction) sur [from, to] : payées seulement si
     * la direction a explicitement crédité cet enseignant pour CET événement précis (voir
     * SchoolEventService#setCreditedTeachers) — sinon un enseignant tout juste ajouté à l'emploi du
     * temps se retrouverait crédité rétroactivement pour un événement passé qu'il n'a jamais couvert.
     * Regroupées par bloc d'emploi du temps (voir {@link #scheduleBlocks}) pour ne compter qu'une seule
     * fois les blocs concurrents (§5.6 enseignant exceptionnel).
     */
    private Map<Long, Integer> eventCoveredMinutesByClass(Long teacherId, Long tenantId, LocalDate from, LocalDate to) {
        if (tenantId == null) return Map.of();
        List<gn.lacaeducation.academics.SchoolEventPublicInfo> events = academics.findSchoolEvents(tenantId, from, to);
        if (events.isEmpty()) return Map.of();
        Map<Long, Integer> result = new HashMap<>();
        for (ScheduleBlock block : scheduleBlocks(teacherId)) {
            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                if (d.getDayOfWeek() != block.day()) continue;
                LocalDate date = d;
                boolean covered = events.stream().anyMatch(ev -> eventCoversSlot(ev, date, block.start(), block.end())
                        && academics.isTeacherCreditedForSchoolEvent(ev.id(), teacherId));
                if (covered) result.merge(block.representativeClassId(), block.durationMinutes(), Integer::sum);
            }
        }
        return result;
    }

    /**
     * Minutes des créneaux récurrents d'un enseignant qui tombent un jour férié (chômé et payé par
     * l'État — calendrier académique §calendar, {@code AcademicEventType.HOLIDAY}) sur [from, to] :
     * TOUJOURS payées, automatiquement, contrairement à un événement école décidé par la direction (voir
     * {@link #eventCoveredMinutesByClass}). Un jour férié n'est jamais saisi comme un {@code SchoolEvent}.
     */
    private Map<Long, Integer> paidHolidayCoveredMinutesByClass(Long teacherId, Long tenantId, LocalDate from, LocalDate to) {
        if (tenantId == null) return Map.of();
        List<LocalDate> holidays = calendar.paidHolidayDates(tenantId, from, to);
        if (holidays.isEmpty()) return Map.of();
        Set<LocalDate> holidaySet = new HashSet<>(holidays);
        Map<Long, Integer> result = new HashMap<>();
        for (ScheduleBlock block : scheduleBlocks(teacherId)) {
            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                if (d.getDayOfWeek() != block.day()) continue;
                if (holidaySet.contains(d)) result.merge(block.representativeClassId(), block.durationMinutes(), Integer::sum);
            }
        }
        return result;
    }

    /**
     * Un enseignant "exceptionnel" (§5.6, ex. EPS) peut couvrir plusieurs classes au même horaire —
     * l'emploi du temps ne bloque plus le conflit d'enseignant pour lui (voir
     * {@code EmploiDuTempsService}). Ses heures sont FIXÉES par le créneau (ex. samedi 8h-12h), pas
     * multipliées par le nombre de classes rassemblées dedans : c'est à lui de couvrir toutes ces
     * classes dans ce temps imparti. Un {@link ScheduleBlock} fusionne les créneaux d'un même jour qui
     * se chevauchent (donc "concurrent" seulement s'il regroupe plus d'une classe) ; un enseignant
     * normal n'a jamais de bloc concurrent puisque le conflit d'enseignant l'en empêche à la création.
     */
    private record ScheduleBlock(DayOfWeek day, LocalTime start, LocalTime end, Set<Long> classIds) {
        int durationMinutes() { return (int) Duration.between(start, end).toMinutes(); }
        boolean concurrent() { return classIds.size() > 1; }
        Long representativeClassId() { return classIds.stream().min(Comparator.naturalOrder()).orElse(null); }
    }

    /** Fusionne les créneaux récurrents (jour + horaire) d'un enseignant qui se chevauchent en blocs
     * uniques — voir {@link ScheduleBlock}. */
    private List<ScheduleBlock> scheduleBlocks(Long teacherId) {
        Map<DayOfWeek, List<ScheduleSlotPublicInfo>> byDay = new HashMap<>();
        for (ScheduleSlotPublicInfo slot : academics.findScheduleByTeacher(teacherId)) {
            if (slot.day() == null || slot.startTime() == null || slot.endTime() == null || slot.classId() == null) continue;
            DayOfWeek dow = DAY_OF_WEEK.get(slot.day().toUpperCase());
            if (dow == null) continue;
            byDay.computeIfAbsent(dow, d -> new ArrayList<>()).add(slot);
        }
        List<ScheduleBlock> blocks = new ArrayList<>();
        for (var e : byDay.entrySet()) {
            List<ScheduleSlotPublicInfo> sorted = e.getValue().stream()
                    .sorted(Comparator.comparing(s -> LocalTime.parse(s.startTime())))
                    .toList();
            LocalTime curStart = null, curEnd = null;
            Set<Long> curClasses = null;
            for (ScheduleSlotPublicInfo s : sorted) {
                LocalTime start = LocalTime.parse(s.startTime());
                LocalTime end = LocalTime.parse(s.endTime());
                // Ne fusionne que les créneaux qui se CHEVAUCHENT réellement (start < curEnd) — deux
                // créneaux simplement consécutifs (ex. 8h-9h puis 9h-10h, deux classes différentes à la
                // suite, cas normal très courant) ne doivent jamais être traités comme "concurrents" :
                // ce serait perdre des heures réellement enseignées à des moments différents.
                if (curStart == null || !start.isBefore(curEnd)) {
                    if (curStart != null) blocks.add(new ScheduleBlock(e.getKey(), curStart, curEnd, curClasses));
                    curStart = start; curEnd = end; curClasses = new HashSet<>();
                } else if (end.isAfter(curEnd)) {
                    curEnd = end;
                }
                curClasses.add(s.classId());
            }
            if (curStart != null) blocks.add(new ScheduleBlock(e.getKey(), curStart, curEnd, curClasses));
        }
        return blocks;
    }

    /** Comme {@link TeacherTeachingProvider#realizedMinutesByClass}, mais déduplique les blocs
     * concurrents (§5.6 enseignant exceptionnel) : si plusieurs classes d'un même bloc ont une séance
     * réalisée à la même date, la durée fixe du bloc n'est comptée qu'une seule fois pour cette date,
     * jamais multipliée par le nombre de classes couvertes en même temps. */
    private Map<Long, Integer> dedupedRealizedByClass(Long teacherId, LocalDate from, LocalDate to) {
        List<ScheduleBlock> blocks = scheduleBlocks(teacherId);
        if (blocks.stream().noneMatch(ScheduleBlock::concurrent)) {
            return teachingProvider.realizedMinutesByClass(teacherId, from, to);
        }
        Map<LocalDate, Map<Long, Integer>> byDateClass = teachingProvider.realizedMinutesByClassAndDate(teacherId, from, to);
        Map<Long, Integer> result = new HashMap<>();
        for (var dateEntry : byDateClass.entrySet()) {
            DayOfWeek dow = dateEntry.getKey().getDayOfWeek();
            Map<Long, Integer> perClass = new HashMap<>(dateEntry.getValue());
            for (ScheduleBlock block : blocks) {
                if (block.day() != dow || !block.concurrent()) continue;
                boolean anyRealized = block.classIds().stream().anyMatch(perClass::containsKey);
                block.classIds().forEach(perClass::remove);
                if (anyRealized) result.merge(block.representativeClassId(), block.durationMinutes(), Integer::sum);
            }
            perClass.forEach((classId, minutes) -> result.merge(classId, minutes, Integer::sum));
        }
        return result;
    }

    /** Vrai si l'événement couvre cette date et que sa plage horaire (toute la journée si non précisée)
     * chevauche le créneau [slotStart, slotEnd[. */
    private boolean eventCoversSlot(gn.lacaeducation.academics.SchoolEventPublicInfo ev, LocalDate date,
                                    LocalTime slotStart, LocalTime slotEnd) {
        if (date.isBefore(ev.startDate()) || date.isAfter(ev.endDate())) return false;
        LocalTime eventStart = ev.startTime() != null ? ev.startTime() : LocalTime.MIN;
        LocalTime eventEnd = ev.endTime() != null ? ev.endTime() : LocalTime.MAX;
        return slotStart.isBefore(eventEnd) && eventStart.isBefore(slotEnd);
    }

    private static final String[] FR_MONTHS = {"Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
            "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"};

    /** Génère automatiquement la fiche de paie du mois à partir des heures réalisées (brut calculé). */
    public Payslip generateFromHours(Long tenantId, Long teacherId, String teacherName, int year, int month,
                                     Long structureId, String teacherPhone, String bankAccount) {
        MonthlyPay mp = computeMonthlyPay(teacherId, year, month);
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        String periodLabel = FR_MONTHS[month - 1] + " " + year;
        // Le retard/départ anticipé cumulés sont purement informatifs (voir computeMonthlyPay) : la
        // durée payée est celle réalisée, jamais diminuée d'un retard déjà pris en compte (ou
        // volontairement ignoré) dans le choix théorique/réel fait séance par séance.
        String notes = String.format(
                "%dh%02d réalisées et payées (dont %dh%02d classes normales, %dh%02d classes d'examen)%s"
                        + " — retard cumulé %d min, départ anticipé cumulé %d min (informatif, non déduit)",
                mp.realizedMinutes() / 60, mp.realizedMinutes() % 60,
                mp.normalEffectiveMinutes() / 60, mp.normalEffectiveMinutes() % 60,
                mp.examEffectiveMinutes() / 60, mp.examEffectiveMinutes() % 60,
                "FIXED".equals(mp.payMode()) ? " + salaire fixe" : "", mp.lateMinutes(), mp.earlyMinutes());
        return generate(new GenerateCommand(tenantId, gn.lacaeducation.payroll.domain.EmployeeType.TEACHER,
                teacherId, teacherName, periodLabel, from, to,
                mp.grossPay(), structureId, notes, null, null, null, null, null, teacherPhone, bankAccount));
    }

    /**
     * Génère la fiche à partir d'heures réalisées saisies MANUELLEMENT par le comptable (§retour
     * comptable : "le directeur des études n'a pas créé de séances ce mois pour les enseignants du
     * secondaire donc la paie des cours est à 0 GNF, pourtant le comptable veut payer — avances sur
     * salaire et enfants de personnel à défalquer"). Le TARIF appliqué reste celui configuré pour
     * l'enseignant ({@link TeacherCompensation}), jamais saisi manuellement : seules les heures sont
     * une saisie de secours quand le cahier de textes est vide, le calcul du montant reste automatique
     * et cohérent avec {@link #computeMonthlyPay}.
     */
    public Payslip generateFromManualHours(Long tenantId, Long teacherId, String teacherName, int year, int month,
                                           Long structureId, String teacherPhone, String bankAccount,
                                           int manualNormalMinutes, int manualExamMinutes) {
        TeacherCompensation comp = compRepo.findByTeacherId(teacherId).orElse(null);
        PayMode mode = comp != null && comp.getPayMode() != null ? comp.getPayMode() : PayMode.HOURLY;
        BigDecimal rateNormal = comp != null && comp.getHourlyRateNormal() != null ? comp.getHourlyRateNormal() : BigDecimal.ZERO;
        BigDecimal rateExam = comp != null && comp.getHourlyRateExam() != null ? comp.getHourlyRateExam() : BigDecimal.ZERO;
        BigDecimal fixed = comp != null && comp.getFixedSalary() != null ? comp.getFixedSalary() : BigDecimal.ZERO;
        BigDecimal normalHours = BigDecimal.valueOf(Math.max(0, manualNormalMinutes)).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        BigDecimal examHours = BigDecimal.valueOf(Math.max(0, manualExamMinutes)).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        BigDecimal teachingPay = normalHours.multiply(rateNormal).add(examHours.multiply(rateExam)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal gross = mode == PayMode.FIXED ? fixed.add(teachingPay) : teachingPay;
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        String periodLabel = FR_MONTHS[month - 1] + " " + year;
        String notes = String.format(
                "%dh%02d réalisées SAISIES MANUELLEMENT par le comptable (dont %dh%02d classes normales, "
                        + "%dh%02d classes d'examen)%s — séances non créées/validées ce mois dans le cahier de textes.",
                (Math.max(0, manualNormalMinutes) + Math.max(0, manualExamMinutes)) / 60,
                (Math.max(0, manualNormalMinutes) + Math.max(0, manualExamMinutes)) % 60,
                Math.max(0, manualNormalMinutes) / 60, Math.max(0, manualNormalMinutes) % 60,
                Math.max(0, manualExamMinutes) / 60, Math.max(0, manualExamMinutes) % 60,
                mode == PayMode.FIXED ? " + salaire fixe" : "");
        return generate(new GenerateCommand(tenantId, gn.lacaeducation.payroll.domain.EmployeeType.TEACHER,
                teacherId, teacherName, periodLabel, from, to,
                gross, structureId, notes, null, null, null, null, null, teacherPhone, bankAccount));
    }

    // ── Calcul automatique par typologie de classe, depuis l'emploi du temps validé (§6) ──────

    private static final Map<String, DayOfWeek> DAY_OF_WEEK = Map.of(
            "LUNDI", DayOfWeek.MONDAY, "MARDI", DayOfWeek.TUESDAY, "MERCREDI", DayOfWeek.WEDNESDAY,
            "JEUDI", DayOfWeek.THURSDAY, "VENDREDI", DayOfWeek.FRIDAY, "SAMEDI", DayOfWeek.SATURDAY,
            "DIMANCHE", DayOfWeek.SUNDAY);

    public record TheoreticalMonthlyPay(Long teacherId, int year, int month, String payMode,
                                        int normalMinutes, int examMinutes,
                                        BigDecimal hourlyRateNormal, BigDecimal hourlyRateExam,
                                        BigDecimal fixedSalary, BigDecimal teachingPay, BigDecimal grossPay) {}

    /** Masse salariale des enseignants d'un tenant (Vision Fondateur §10.2) : cumul des taux horaires
     * configurés + paie mensuelle théorique (heures réelles × tarif normal/examen, ou salaire fixe).
     * {@code maternelleMonthlyPayroll}/{@code primaryMonthlyPayroll}/{@code secondaryMonthlyPayroll} :
     * même total que {@code monthlyPayroll}, réparti par cycle (§retour fondateur, état financier du
     * mois — Maternelle distinguée du Primaire sur demande explicite) — leur somme égale
     * {@code monthlyPayroll}. */
    public record TeacherPayrollMass(BigDecimal totalHourlyRateNormal, BigDecimal totalHourlyRateExam,
                                     BigDecimal monthlyPayroll, int teacherCount,
                                     BigDecimal maternelleMonthlyPayroll, BigDecimal primaryMonthlyPayroll,
                                     BigDecimal secondaryMonthlyPayroll) {}

    @Transactional(readOnly = true)
    public TeacherPayrollMass teacherPayrollMass(Long tenantId, int year, int month) {
        return teacherPayrollMass(tenantId, year, month, null);
    }

    /** @param validTeacherIds si non nul, ignore les fiches de compensation dont l'enseignant n'y
     * figure plus — une fiche {@code TeacherCompensation} reste orpheline en base après la suppression
     * de l'enseignant (jamais nettoyée automatiquement), ce qui gonflait la masse salariale théorique
     * d'un enseignant supprimé indéfiniment (§retour utilisateur, Vision Fondateur en production, cas
     * observé avec un enseignant du primaire à salaire fixe). {@code null} = pas de filtre
     * (comportement historique, ex. tests qui n'ont pas de référentiel enseignant à fournir). */
    @Transactional(readOnly = true)
    public TeacherPayrollMass teacherPayrollMass(Long tenantId, int year, int month, java.util.Set<Long> validTeacherIds) {
        List<TeacherCompensation> comps = compRepo.findByTenantId(tenantId).stream()
                .filter(c -> validTeacherIds == null || validTeacherIds.contains(c.getTeacherId()))
                .toList();
        BigDecimal totalNormal = BigDecimal.ZERO;
        BigDecimal totalExam = BigDecimal.ZERO;
        BigDecimal monthly = BigDecimal.ZERO;
        BigDecimal maternelleMonthly = BigDecimal.ZERO;
        BigDecimal primaryMonthly = BigDecimal.ZERO;
        BigDecimal secondaryMonthly = BigDecimal.ZERO;
        for (TeacherCompensation c : comps) {
            totalNormal = totalNormal.add(c.getHourlyRateNormal() != null ? c.getHourlyRateNormal() : BigDecimal.ZERO);
            totalExam = totalExam.add(c.getHourlyRateExam() != null ? c.getHourlyRateExam() : BigDecimal.ZERO);
            BigDecimal teacherMonthly;
            if (c.getPayMode() == PayMode.FIXED) {
                teacherMonthly = c.getFixedSalary() != null ? c.getFixedSalary() : BigDecimal.ZERO;
            } else {
                teacherMonthly = computeTheoreticalMonthlyPay(c.getTeacherId(), year, month).grossPay();
            }
            monthly = monthly.add(teacherMonthly);
            switch (resolveCycleBucket(c.getTeacherId())) {
                case MATERNELLE -> maternelleMonthly = maternelleMonthly.add(teacherMonthly);
                case PRIMAIRE -> primaryMonthly = primaryMonthly.add(teacherMonthly);
                case SECONDAIRE -> secondaryMonthly = secondaryMonthly.add(teacherMonthly);
            }
        }
        // Enseignants existants SANS encore de TeacherCompensation configurée (jamais eu de fiche de
        // paie générée pour eux — voir ensureTeacherCompensation côté frontend, appelé seulement à la
        // génération) : leur rémunération mensuelle/forfaitaire saisie à la CRÉATION de la fiche
        // enseignant (Teacher#remunerationType/Rate, exposée via TeacherPublicInfoProvider) compte déjà
        // dans la masse salariale affichée — jamais absente juste parce qu'aucune fiche de paie n'a
        // encore été générée, ni parce qu'aucune classe/matière ne lui est assignée (§retour
        // utilisateur : "même si il n'est pas affecté à une matière et une classe, le cumul de tout le
        // salaire doit venir"). HORAIRE non repris ici (aucun taux normal/examen séparé connu à ce
        // stade, voir TeacherCompensation) — seul un enseignant déjà sur un mode fixe est concerné.
        if (validTeacherIds != null) {
            java.util.Set<Long> teacherIdsWithComp = comps.stream().map(TeacherCompensation::getTeacherId)
                    .collect(java.util.stream.Collectors.toSet());
            for (TeacherPublicInfoProvider.TeacherInfo info : teacherPublicInfoProvider.listTeachers(tenantId)) {
                if (!validTeacherIds.contains(info.id()) || teacherIdsWithComp.contains(info.id())) continue;
                if (info.remunerationRate() == null || "HORAIRE".equals(info.remunerationType())) continue;
                BigDecimal teacherMonthly = info.remunerationRate();
                monthly = monthly.add(teacherMonthly);
                switch (resolveCycleBucket(info.id())) {
                    case MATERNELLE -> maternelleMonthly = maternelleMonthly.add(teacherMonthly);
                    case PRIMAIRE -> primaryMonthly = primaryMonthly.add(teacherMonthly);
                    case SECONDAIRE -> secondaryMonthly = secondaryMonthly.add(teacherMonthly);
                }
            }
        }
        // Avec un filtre fourni, le compte reflète le référentiel enseignant réel (pas seulement ceux
        // ayant une compensation configurée) : un enseignant tout juste créé, sans salaire renseigné,
        // doit compter — §retour utilisateur, "peu importe primaire/secondaire, salaire configuré ou
        // non, il doit être affiché sur la vision fondateur".
        int teacherCount = validTeacherIds != null ? validTeacherIds.size() : comps.size();
        return new TeacherPayrollMass(totalNormal, totalExam, monthly, teacherCount,
                maternelleMonthly, primaryMonthly, secondaryMonthly);
    }

    /** Cycle (Maternelle/Primaire/Secondaire) de chaque enseignant demandé — retour utilisateur :
     * "pouvoir payer les gens par niveau... générer les fiches des enseignants du primaire à part, et
     * ceux du secondaire" + colonne "Cycle" sur le tableau Rémunération & heures. Même résolution que
     * {@link #teacherPayrollMass} (majorité des classes assignées), exposée ici pour les enseignants
     * sans encore de {@code TeacherCompensation} configurée (qui n'apparaîtraient pas dans la masse
     * salariale, mais doivent déjà être filtrables par cycle avant de configurer leur rémunération). */
    public java.util.Map<Long, String> resolveTeacherCycles(java.util.List<Long> teacherIds) {
        java.util.Map<Long, String> result = new java.util.LinkedHashMap<>();
        for (Long id : teacherIds) {
            result.put(id, resolveCycleBucket(id).name());
        }
        return result;
    }

    /** Regroupement par cycle (Maternelle/Primaire/Secondaire — retour fondateur : Maternelle doit
     * apparaître à part sur l'état financier du mois, pas fondue dans "Primaire"). Logique dupliquée
     * ici (pas d'import de {@code grading.domain.CycleGroup}) : la modularité du projet
     * ({@code ModularityTests}) interdit à {@code payroll} de dépendre du domaine interne d'un autre
     * module, seules ses API publiques ({@code AcademicsDataProvider}) sont accessibles. */
    private enum TeacherCycleBucket { MATERNELLE, PRIMAIRE, SECONDAIRE }

    private TeacherCycleBucket cycleBucketOf(String schoolCycle) {
        if (schoolCycle == null) return TeacherCycleBucket.SECONDAIRE;
        return switch (schoolCycle) {
            case "GARDERIE", "MATERNELLE" -> TeacherCycleBucket.MATERNELLE;
            case "PRIMAIRE" -> TeacherCycleBucket.PRIMAIRE;
            default -> TeacherCycleBucket.SECONDAIRE;
        };
    }

    /** Cycle résolu depuis les "Niveaux enseignés" de l'enseignant (checklist déclarative sur sa
     * fiche, module staff) — JAMAIS depuis ses classes/matières réellement assignées (§retour
     * utilisateur : "base-toi sur le niveau enseigné, pas sur les matières"). Un moniteur maternelle/
     * primaire sans aucune matière affectée (cas normal, voir LessonService#resolveHeadTeacherId)
     * n'était auparavant jamais détecté comme tel (classé Secondaire par défaut, faussant "État
     * financier du mois") : la checklist "Niveaux enseignés" est une déclaration DIRECTE et
     * volontaire, qui ne dépend d'aucune assignation de classe. Présence simple (pas de majorité) :
     * dès qu'un des niveaux déclarés est Maternelle (ou sinon Primaire), ce cycle l'emporte — Secondaire
     * seulement si aucun niveau Maternelle/Primaire n'est déclaré (y compris aucun niveau du tout). */
    private TeacherCycleBucket resolveCycleBucket(Long teacherId) {
        boolean maternelle = false;
        boolean primaire = false;
        for (Long levelId : teacherPublicInfoProvider.findAssignedLevelIds(teacherId)) {
            TeacherCycleBucket bucket = cycleBucketOf(academics.findLevelCycle(levelId).orElse(null));
            if (bucket == TeacherCycleBucket.MATERNELLE) maternelle = true;
            else if (bucket == TeacherCycleBucket.PRIMAIRE) primaire = true;
        }
        if (maternelle) return TeacherCycleBucket.MATERNELLE;
        if (primaire) return TeacherCycleBucket.PRIMAIRE;
        return TeacherCycleBucket.SECONDAIRE;
    }

    private boolean isPrimaryCycle(String schoolCycle) {
        if (schoolCycle == null) return false;
        return switch (schoolCycle) {
            case "GARDERIE", "MATERNELLE", "PRIMAIRE" -> true;
            default -> false;
        };
    }

    /** Masse salariale RÉELLE des enseignants d'un tenant (Vision Fondateur) : contrairement à
     * {@link #teacherPayrollMass}, qui projette depuis l'emploi du temps (heures théoriques, sans
     * tenir compte des retards/départs anticipés réellement pointés), celle-ci part de
     * {@link #computeMonthlyPay} — heures réellement réalisées (cahier de textes marqué "réalisée" par
     * le directeur des études), retards et départs anticipés cumulés déjà déduits. La différence entre
     * les deux masses révèle concrètement ce que coûtent les retards/départs anticipés non gérés — et
     * sert à repérer une anomalie (un enseignant payé plein tarif malgré une présence très partielle). */
    public record RealTeacherPayrollMass(BigDecimal monthlyPayroll, int totalLateMinutes, int totalEarlyMinutes,
                                         int totalRealizedMinutes, int teacherCount) {}

    @Transactional(readOnly = true)
    public RealTeacherPayrollMass realTeacherPayrollMass(Long tenantId, int year, int month) {
        return realTeacherPayrollMass(tenantId, year, month, null);
    }

    /** @param validTeacherIds voir {@link #teacherPayrollMass(Long, int, int, java.util.Set)} — même
     * filtrage anti-orphelin, {@code null} = pas de filtre. */
    @Transactional(readOnly = true)
    public RealTeacherPayrollMass realTeacherPayrollMass(Long tenantId, int year, int month, java.util.Set<Long> validTeacherIds) {
        List<TeacherCompensation> comps = compRepo.findByTenantId(tenantId).stream()
                .filter(c -> validTeacherIds == null || validTeacherIds.contains(c.getTeacherId()))
                .toList();
        BigDecimal monthly = BigDecimal.ZERO;
        int totalLate = 0, totalEarly = 0, totalRealized = 0;
        for (TeacherCompensation c : comps) {
            // Le retard/départ anticipé compte dans le cumul école pour TOUS les enseignants, même à
            // salaire fixe (dont le montant lui n'en dépend pas) — sans ça, le total sous-estimerait
            // la réalité pour toute école ayant des enseignants payés au fixe.
            MonthlyPay mp = computeMonthlyPay(c.getTeacherId(), year, month);
            totalLate += mp.lateMinutes();
            totalEarly += mp.earlyMinutes();
            totalRealized += mp.realizedMinutes();
            monthly = monthly.add(c.getPayMode() == PayMode.FIXED
                    ? (c.getFixedSalary() != null ? c.getFixedSalary() : BigDecimal.ZERO)
                    : mp.grossPay());
        }
        int teacherCount = validTeacherIds != null ? validTeacherIds.size() : comps.size();
        return new RealTeacherPayrollMass(monthly, totalLate, totalEarly, totalRealized, teacherCount);
    }

    /** Salaire (mensuel courant + estimation cumulée sur monthsCount mois) de chaque enseignant du
     * tenant (Vision Fondateur) — pas seulement le total agrégé de {@link #teacherPayrollMass}. */
    public record TeacherSalaryBreakdown(Long teacherId, BigDecimal monthlyPay, BigDecimal annualProjection) {}

    @Transactional(readOnly = true)
    public List<TeacherSalaryBreakdown> teacherSalaryBreakdown(Long tenantId, int year, int month, int monthsCount) {
        List<TeacherSalaryBreakdown> result = new java.util.ArrayList<>();
        for (TeacherCompensation c : compRepo.findByTenantId(tenantId)) {
            BigDecimal monthlyPay = monthlyGrossPayFor(c, year, month);
            BigDecimal annual = BigDecimal.ZERO;
            YearMonth cursor = YearMonth.of(year, month);
            for (int i = 0; i < monthsCount; i++) {
                annual = annual.add(monthlyGrossPayFor(c, cursor.getYear(), cursor.getMonthValue()));
                cursor = cursor.plusMonths(1);
            }
            result.add(new TeacherSalaryBreakdown(c.getTeacherId(), monthlyPay, annual));
        }
        return result;
    }

    /** Salaire brut théorique d'un enseignant pour un mois donné : fixe s'il est en mode FIXE, sinon
     * heures théoriques de l'emploi du temps (jours fériés exclus, sauf avantage vacances) × tarif. */
    private BigDecimal monthlyGrossPayFor(TeacherCompensation c, int year, int month) {
        if (c.getPayMode() == PayMode.FIXED) {
            return c.getFixedSalary() != null ? c.getFixedSalary() : BigDecimal.ZERO;
        }
        return computeTheoreticalMonthlyPay(c.getTeacherId(), year, month).grossPay();
    }

    /**
     * Calcule automatiquement, sans saisie manuelle, le nombre d'heures théoriques que l'enseignant
     * devait effectuer ce mois-ci (projection de son emploi du temps hebdomadaire sur les
     * occurrences de chaque jour ouvré du mois), séparément pour les classes normales et les classes
     * d'examen, puis applique le tarif horaire correspondant à chacune (§6).
     */
    @Transactional(readOnly = true)
    public TheoreticalMonthlyPay computeTheoreticalMonthlyPay(Long teacherId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        TeacherCompensation comp = compRepo.findByTeacherId(teacherId).orElse(null);
        Long tenantId = comp != null && comp.getTenantId() != null ? comp.getTenantId() : TenantContext.get();

        // Vacances scolaires (§calendar) exclues du comptage par défaut : un enseignant programmé le
        // lundi ne doit pas être compté sur un lundi tombant en vacances (sinon les heures dues sont
        // surestimées). Sauf si CE mois précis (année + mois) est marqué payé pendant les vacances par
        // la direction (avantage accordé au cas par cas, pas un interrupteur valable toute l'année) :
        // dans ce cas, on ne retire rien pour ce mois-ci.
        // Les jours fériés CHÔMÉS PAYÉS (ex. fête nationale), eux, ne sont JAMAIS exclus : légalement
        // payés, ils comptent toujours comme travaillés, indépendamment du statut "vacances" ci-dessus.
        boolean paidThisMonth = vacationPayRepo.existsByTeacherIdAndYearAndMonth(teacherId, year, month);
        java.util.Set<LocalDate> nonWorkingDates = tenantId != null && !paidThisMonth
                ? new java.util.HashSet<>(calendar.vacationDates(tenantId, ym.atDay(1), ym.atEndOfMonth()))
                : java.util.Set.of();
        Map<DayOfWeek, Integer> occurrencesByDay = new HashMap<>();
        for (int day = 1; day <= ym.lengthOfMonth(); day++) {
            LocalDate d = ym.atDay(day);
            if (nonWorkingDates.contains(d)) continue;
            occurrencesByDay.merge(d.getDayOfWeek(), 1, Integer::sum);
        }

        Map<Long, ClassPublicInfo> classCache = new HashMap<>();
        int normalMinutes = 0;
        int examMinutes = 0;
        for (ScheduleSlotPublicInfo slot : academics.findScheduleByTeacher(teacherId)) {
            if (slot.startTime() == null || slot.endTime() == null || slot.day() == null) continue;
            DayOfWeek dayOfWeek = DAY_OF_WEEK.get(slot.day().toUpperCase());
            if (dayOfWeek == null) continue;
            int occurrences = occurrencesByDay.getOrDefault(dayOfWeek, 0);
            if (occurrences == 0) continue;
            int minutes = (int) Duration.between(LocalTime.parse(slot.startTime()), LocalTime.parse(slot.endTime())).toMinutes()
                    * occurrences;
            if (isExamClass(slot.classId(), classCache)) examMinutes += minutes; else normalMinutes += minutes;
        }

        PayMode mode = comp != null && comp.getPayMode() != null ? comp.getPayMode() : PayMode.HOURLY;
        BigDecimal rateNormal = comp != null && comp.getHourlyRateNormal() != null ? comp.getHourlyRateNormal() : BigDecimal.ZERO;
        BigDecimal rateExam = comp != null && comp.getHourlyRateExam() != null ? comp.getHourlyRateExam() : BigDecimal.ZERO;
        BigDecimal fixed = comp != null && comp.getFixedSalary() != null ? comp.getFixedSalary() : BigDecimal.ZERO;

        BigDecimal normalHours = BigDecimal.valueOf(normalMinutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        BigDecimal examHours = BigDecimal.valueOf(examMinutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        BigDecimal teachingPay = normalHours.multiply(rateNormal).add(examHours.multiply(rateExam)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal gross = mode == PayMode.FIXED ? fixed.add(teachingPay) : teachingPay;
        return new TheoreticalMonthlyPay(teacherId, year, month, mode.name(), normalMinutes, examMinutes,
                rateNormal, rateExam, fixed, teachingPay, gross);
    }

    /** Génère automatiquement la fiche de paie du mois à partir de l'emploi du temps validé (§6),
     * sans aucune saisie manuelle : heures théoriques par typologie de classe × tarif correspondant. */
    public Payslip generateFromTimetable(Long tenantId, Long teacherId, String teacherName, int year, int month,
                                         Long structureId, String teacherPhone, String bankAccount) {
        TheoreticalMonthlyPay tp = computeTheoreticalMonthlyPay(teacherId, year, month);
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        String periodLabel = FR_MONTHS[month - 1] + " " + year;
        String notes = String.format("%dh%02d (classes normales) + %dh%02d (classes d'examen), depuis l'emploi du temps%s",
                tp.normalMinutes() / 60, tp.normalMinutes() % 60, tp.examMinutes() / 60, tp.examMinutes() % 60,
                "FIXED".equals(tp.payMode()) ? " + salaire fixe" : "");
        return generate(new GenerateCommand(tenantId, gn.lacaeducation.payroll.domain.EmployeeType.TEACHER,
                teacherId, teacherName, periodLabel, from, to,
                tp.grossPay(), structureId, notes, null, null, null, null, null, teacherPhone, bankAccount));
    }

    /**
     * Fiche de paie combinée pour une personne à la fois administrative/direction et enseignante
     * (§payroll — ex. un directeur qui donne aussi quelques heures de cours) : UNE seule fiche, avec
     * le salaire de base fixe (fourni par l'appelant, depuis {@code Employee.baseSalary}) comme sur
     * une fiche administrative classique, PLUS une ligne « Heures d'enseignement » calculée
     * automatiquement depuis les heures réellement réalisées par l'enseignant lié
     * ({@code linkedTeacherId}, même calcul que {@link #generateFromHours}). Le salarié de la fiche
     * reste l'Employee (comme pour une fiche administrative simple) ; l'enseignement n'est qu'une
     * ligne supplémentaire, pas une fiche séparée — pas de dépendance au module {@code staff} : c'est
     * à l'appelant de résoudre {@code Employee} (nom, salaire de base, type, enseignant lié).
     */
    public Payslip generateCombinedPayslip(Long tenantId, Long employeeId, String employeeName,
                                           gn.lacaeducation.payroll.domain.EmployeeType employeeType,
                                           BigDecimal baseSalary, Long linkedTeacherId,
                                           int year, int month, Long structureId) {
        return generateCombinedPayslip(tenantId, employeeId, employeeName, employeeType, baseSalary,
                linkedTeacherId, year, month, structureId, null, null);
    }

    public Payslip generateCombinedPayslip(Long tenantId, Long employeeId, String employeeName,
                                           gn.lacaeducation.payroll.domain.EmployeeType employeeType,
                                           BigDecimal baseSalary, Long linkedTeacherId,
                                           int year, int month, Long structureId,
                                           String employeePhone, String bankAccount) {
        if (linkedTeacherId == null) {
            throw new BusinessRuleException("Aucun enseignant lié à ce personnel — renseignez le lien dans sa fiche avant de générer la fiche combinée.");
        }
        String periodLabel = FR_MONTHS[month - 1] + " " + year;
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        Payslip p = generate(new GenerateCommand(tenantId, employeeType, employeeId, employeeName, periodLabel,
                from, to, baseSalary, structureId,
                "Fiche combinée : salaire de base (direction/administratif) + heures d'enseignement",
                null, null, null, null, null, employeePhone, bankAccount));

        MonthlyPay mp = computeMonthlyPay(linkedTeacherId, year, month);
        if (mp.grossPay() != null && mp.grossPay().signum() > 0) {
            String label = String.format("Heures d'enseignement (%dh%02d effectives)",
                    mp.effectiveMinutes() / 60, mp.effectiveMinutes() % 60);
            p = addLine(p.getId(), label, PayslipLineType.EARNING, mp.grossPay());
        }
        return p;
    }

    // ── Structures salariales ────────────────────────────────────────────────

    public SalaryStructure createStructure(Long tenantId, String name, String code) {
        if (name == null || name.isBlank()) throw new BusinessRuleException("Le nom de la structure est requis");
        return structureRepo.save(SalaryStructure.builder().tenantId(tenantId).name(name).code(code).active(true).build());
    }

    public SalaryStructure updateStructure(Long id, String name, String code, boolean active) {
        SalaryStructure s = loadStructure(id);
        s.setName(name); s.setCode(code); s.setActive(active);
        return structureRepo.save(s);
    }

    public void deleteStructure(Long id) { structureRepo.deleteById(id); }

    @Transactional(readOnly = true)
    public List<SalaryStructure> structures(Long tenantId) { return structureRepo.findByTenantId(tenantId); }

    @Transactional(readOnly = true)
    public List<SalaryRule> rules(Long structureId) { return ruleRepo.findByStructureIdOrderBySequenceAsc(structureId); }

    public SalaryRule addRule(Long tenantId, Long structureId, SalaryRule r) {
        loadStructure(structureId);
        r.setTenantId(tenantId);
        r.setStructureId(structureId);
        if (r.getValue() == null) r.setValue(BigDecimal.ZERO);
        if (r.getSequence() == null) r.setSequence(10);
        if (r.getLineType() == null) r.setLineType(PayslipLineType.EARNING);
        if (r.getComputation() == null) r.setComputation(RuleComputation.FIXED);
        return ruleRepo.save(r);
    }

    public void deleteRule(Long ruleId) { ruleRepo.deleteById(ruleId); }

    // ── Génération des fiches ────────────────────────────────────────────────

    public record GenerateCommand(Long tenantId, gn.lacaeducation.payroll.domain.EmployeeType employeeType,
                                  Long teacherId, String teacherName, String periodLabel,
                                  LocalDate periodStart, LocalDate periodEnd, BigDecimal baseSalary, Long structureId,
                                  String notes, Integer absentDays, Integer workingDays,
                                  BigDecimal overtimeHours, BigDecimal overtimeRate, BigDecimal overtimeMultiplier,
                                  /** Téléphone et compte bancaire de l'enseignant/employé, transmis par l'appelant
                                   * (qui a déjà la fiche) — figés sur la fiche de paie pour le fichier bancaire. */
                                  String teacherPhone, String bankAccount) {}

    public Payslip generate(GenerateCommand c) {
        if (c.teacherId() == null) throw new BusinessRuleException("Le salarié est requis");
        if (c.periodLabel() == null || c.periodLabel().isBlank()) throw new BusinessRuleException("La période est requise");
        gn.lacaeducation.payroll.domain.EmployeeType type = c.employeeType() != null
                ? c.employeeType() : gn.lacaeducation.payroll.domain.EmployeeType.TEACHER;
        // Empêche un doublon silencieux (double-clic sur « Générer en lot »/« Générer une fiche », ou
        // relance par erreur du même mois) : une fiche existe déjà pour ce salarié sur cette période.
        if (payslipRepo.existsByTenantIdAndTeacherIdAndPeriodLabelAndEmployeeType(c.tenantId(), c.teacherId(), c.periodLabel(), type)) {
            throw new ConflictException(
                    "Une fiche de paie existe déjà pour ce salarié sur la période « " + c.periodLabel()
                            + " » — modifiez ou supprimez la fiche existante plutôt que d'en générer une nouvelle.");
        }
        BigDecimal base = c.baseSalary() != null ? c.baseSalary() : BigDecimal.ZERO;
        Payslip p = payslipRepo.save(Payslip.builder()
                .tenantId(c.tenantId()).employeeType(type)
                .teacherId(c.teacherId()).teacherName(c.teacherName())
                .teacherPhone(c.teacherPhone()).bankAccount(c.bankAccount())
                .periodLabel(c.periodLabel())
                .periodStart(c.periodStart()).periodEnd(c.periodEnd())
                .baseSalary(base).grossSalary(base).netSalary(base)
                .status(PayslipStatus.DRAFT).notes(c.notes()).createdAt(LocalDateTime.now()).build());
        if (c.structureId() != null) applyStructure(p, c.structureId());
        applyAttendanceAndOvertime(p, c);
        applyStaffBenefitRemainderDeduction(p, c);
        applySalaryAdvanceDeduction(p, c);
        return recompute(p.getId());
    }

    /**
     * Reste à charge non couvert par un avantage personnel PARTIEL (§billing) sur les factures des
     * enfants du salarié : ajouté comme ligne de retenue, exactement le solde réel restant sur les
     * factures concernées (jamais plus — une facture déjà réglée directement par le parent n'est plus
     * comptée, voir {@code InvoiceService#staffBenefitInvoicesForPeriod} qui ne retient que les
     * factures encore UNPAID/PARTIAL), répercuté comme paiement à la mise en paiement de la fiche (voir
     * {@code markPaid}). Remplace l'ancienne règle à montant fixe par enfant ({@code
     * SalaryFeeDeductionUseCase#computeFeeDeduction}, désormais retirée d'ici — retour comptable :
     * les deux s'additionnaient pour le même enfant depuis que ce solde réel couvre déjà tous les
     * frais, double retenue. La règle à montant fixe reste configurable/consultable (écran Rôles &
     * permissions → Paie), juste plus appliquée automatiquement à la génération.
     */
    private static final String STAFF_BENEFIT_LINE_PREFIX = "Reste avantage personnel";

    private void applyStaffBenefitRemainderDeduction(Payslip p, GenerateCommand c) {
        var deduction = salaryFeeDeductionUseCase.computeStaffBenefitRemainder(c.tenantId(), p.getEmployeeType(), c.teacherId(),
                c.periodStart(), c.periodLabel());
        if (deduction.amount() != null && deduction.amount().signum() > 0) {
            saveLine(p, STAFF_BENEFIT_LINE_PREFIX + " (" + deduction.childrenCount() + " enfant(s))",
                    PayslipLineType.DEDUCTION, deduction.amount());
        }
    }

    /**
     * Recalcule la ligne "Reste avantage personnel" d'une fiche encore modifiable (§retour comptable :
     * "j'ai retiré deux factures, j'ai rechargé mais le montant global de la retenue n'a pas changé") —
     * la ligne est posée une seule fois à la génération ({@link #applyStaffBenefitRemainderDeduction}),
     * elle ne se met jamais à jour toute seule quand une facture est retirée/réincluse après coup
     * (§billing, modal "Détail de la retenue") : il faut explicitement la régénérer. Sans effet (lève)
     * sur une fiche déjà PAYÉE — déjà figée, répercutée sur les factures, ne doit plus bouger.
     */
    public Payslip refreshStaffBenefitDeductionLine(Long payslipId) {
        Payslip p = mutable(payslipId);
        lineRepo.findByPayslipId(payslipId).stream()
                .filter(l -> l.getLabel() != null && l.getLabel().startsWith(STAFF_BENEFIT_LINE_PREFIX))
                .forEach(l -> lineRepo.deleteById(l.getId()));
        var deduction = salaryFeeDeductionUseCase.computeStaffBenefitRemainder(p.getTenantId(), p.getEmployeeType(),
                p.getTeacherId(), p.getPeriodStart(), p.getPeriodLabel());
        if (deduction.amount() != null && deduction.amount().signum() > 0) {
            saveLine(p, STAFF_BENEFIT_LINE_PREFIX + " (" + deduction.childrenCount() + " enfant(s))",
                    PayslipLineType.DEDUCTION, deduction.amount());
        }
        return recompute(payslipId);
    }

    /**
     * Remboursement automatique d'une avance sur salaire en cours (§payroll) : déduit du solde restant,
     * borné par le net déjà calculé sur la fiche à ce stade (lignes de structure/absences/scolarité déjà
     * appliquées) — jamais un net négatif. Répété à chaque génération de fiche tant que le solde n'est
     * pas épuisé (voir {@code SalaryAdvanceService#applyDeduction}).
     *
     * <p>Double profil (§retour utilisateur urgence production, "il ne prend pas l'avance sur salaire") :
     * une personne à la fois enseignante ET personnel direction/administratif a DEUX identités
     * distinctes ({@code teacherId}+TEACHER, {@code employeeId}+catégorie) — l'écran d'octroi d'avance
     * liste les deux séparément. Si l'avance a été accordée sous une identité mais que la fiche de paie
     * est générée sous l'AUTRE (ex. avance accordée "Personnel", fiche générée depuis la page enseignant
     * "Paie du mois"), la recherche ne trouvait jamais l'avance et ne la déduisait jamais. On tente donc
     * systématiquement l'identité liée (via {@code Employee#linkedTeacherId}) en repli si la première
     * tentative n'a rien trouvé.</p>
     */
    private void applySalaryAdvanceDeduction(Payslip p, GenerateCommand c) {
        BigDecimal base = p.getBaseSalary() != null ? p.getBaseSalary() : BigDecimal.ZERO;
        BigDecimal earnings = BigDecimal.ZERO;
        BigDecimal deductions = BigDecimal.ZERO;
        for (PayslipLine l : lineRepo.findByPayslipId(p.getId())) {
            if (l.getLineType() == PayslipLineType.EARNING) earnings = earnings.add(l.getAmount());
            else deductions = deductions.add(l.getAmount());
        }
        BigDecimal netSoFar = base.add(earnings).subtract(deductions);
        gn.lacaeducation.payroll.domain.EmployeeType type = c.employeeType() != null
                ? c.employeeType() : gn.lacaeducation.payroll.domain.EmployeeType.TEACHER;
        var result = salaryAdvanceUseCase.applyDeduction(c.tenantId(), c.teacherId(), type, netSoFar);
        if ((result.amount() == null || result.amount().signum() <= 0)) {
            result = applySalaryAdvanceDeductionViaLinkedIdentity(c.tenantId(), c.teacherId(), type, netSoFar);
        }
        if (result.amount() != null && result.amount().signum() > 0) {
            saveLine(p, result.label(), PayslipLineType.DEDUCTION, result.amount());
        }
    }

    /** Tente la déduction sous l'identité LIÉE à {@code (teacherId, type)} — la fiche Employee liée si
     * {@code type == TEACHER}, ou la fiche Teacher liée à l'Employee sinon. Résultat à zéro (jamais
     * d'exception) si cette personne n'a pas de double profil ou que l'identité liée n'a pas d'avance
     * active non plus. */
    private gn.lacaeducation.payroll.application.port.in.SalaryAdvanceUseCase.AdvanceDeductionResult
            applySalaryAdvanceDeductionViaLinkedIdentity(Long tenantId, Long teacherId,
                    gn.lacaeducation.payroll.domain.EmployeeType type, BigDecimal netSoFar) {
        var none = new gn.lacaeducation.payroll.application.port.in.SalaryAdvanceUseCase.AdvanceDeductionResult(BigDecimal.ZERO, null);
        if (type == gn.lacaeducation.payroll.domain.EmployeeType.TEACHER) {
            return employeePublicInfoProvider.findByLinkedTeacherId(tenantId, teacherId)
                    .map(emp -> {
                        try {
                            gn.lacaeducation.payroll.domain.EmployeeType empType =
                                    gn.lacaeducation.payroll.domain.EmployeeType.valueOf(emp.category());
                            return salaryAdvanceUseCase.applyDeduction(tenantId, emp.id(), empType, netSoFar);
                        } catch (IllegalArgumentException e) {
                            return none;
                        }
                    })
                    .orElse(none);
        }
        return employeePublicInfoProvider.findEmployee(teacherId)
                .map(gn.lacaeducation.staff.EmployeePublicInfoProvider.EmployeeInfo::linkedTeacherId)
                .filter(java.util.Objects::nonNull)
                .map(linkedTeacherId -> salaryAdvanceUseCase.applyDeduction(
                        tenantId, linkedTeacherId, gn.lacaeducation.payroll.domain.EmployeeType.TEACHER, netSoFar))
                .orElse(none);
    }

    /**
     * Retenue sur absences (LOP : montant journalier × jours d'absence) et prime d'heures supplémentaires
     * (heures × taux × coefficient, ex. 1.5 ordinaire / 2 week-end / 2.5 férié). Les jours d'absence sont
     * auto-dérivés du pointage enseignant (module attendance) sur le mois de {@code periodStart} ; une
     * valeur explicite dans la commande reste prioritaire (correction manuelle).
     */
    private void applyAttendanceAndOvertime(Payslip p, GenerateCommand c) {
        BigDecimal base = p.getBaseSalary() != null ? p.getBaseSalary() : BigDecimal.ZERO;
        Integer absentDays = c.absentDays();
        Integer workingDays = c.workingDays();
        // Auto-dérivation depuis le pointage : réservée aux enseignants (le personnel non-enseignant
        // n'est pas pointé par séance ; ses absences éventuelles sont saisies manuellement).
        boolean isTeacher = c.employeeType() == null
                || c.employeeType() == gn.lacaeducation.payroll.domain.EmployeeType.TEACHER;
        if (isTeacher && absentDays == null && c.teacherId() != null && c.periodStart() != null) {
            TeacherWorkProvider.MonthlyWork mw = workProvider.monthlyWork(
                    c.teacherId(), c.periodStart().getYear(), c.periodStart().getMonthValue());
            absentDays = (int) mw.absentDays();
            if (workingDays == null) {
                long scheduledDays = mw.presentDays() + mw.absentDays();
                workingDays = scheduledDays > 0 ? (int) scheduledDays : null;
            }
        }
        if (absentDays != null && absentDays > 0 && workingDays != null && workingDays > 0) {
            BigDecimal dailyRate = base.divide(BigDecimal.valueOf(workingDays), 2, RoundingMode.HALF_UP);
            BigDecimal amount = dailyRate.multiply(BigDecimal.valueOf(absentDays));
            saveLine(p, "Absences (" + absentDays + " j)", PayslipLineType.DEDUCTION, amount);
        }
        if (c.overtimeHours() != null && c.overtimeHours().signum() > 0
                && c.overtimeRate() != null && c.overtimeRate().signum() > 0) {
            BigDecimal mult = (c.overtimeMultiplier() != null && c.overtimeMultiplier().signum() > 0)
                    ? c.overtimeMultiplier() : BigDecimal.ONE;
            BigDecimal amount = c.overtimeHours().multiply(c.overtimeRate()).multiply(mult)
                    .setScale(2, RoundingMode.HALF_UP);
            saveLine(p, "Heures supp. (" + c.overtimeHours().stripTrailingZeros().toPlainString()
                    + "h ×" + mult.stripTrailingZeros().toPlainString() + ")", PayslipLineType.EARNING, amount);
        }
    }

    /** Génération en lot : une fiche par (enseignant, base) fournis, avec la même structure/période. */
    public List<Payslip> generateBatch(Long tenantId, String periodLabel, LocalDate start, LocalDate end,
                                       Long structureId, List<TeacherWage> wages) {
        return wages.stream()
                .map(w -> generate(new GenerateCommand(tenantId, gn.lacaeducation.payroll.domain.EmployeeType.TEACHER,
                        w.teacherId(), w.teacherName(), periodLabel, start, end,
                        w.baseSalary(), structureId, null, null, null, null, null, null,
                        w.teacherPhone(), w.bankAccount())))
                .toList();
    }

    /** {@code teacherName}/{@code teacherPhone}/{@code bankAccount} sont facultatifs (rétro-compatible
     * avec un appelant qui ne les fournit pas encore) — transmis par le frontend, qui a déjà la fiche
     * enseignant, pour que le fichier bancaire fonctionne aussi sur les fiches générées en lot. */
    public record TeacherWage(Long teacherId, BigDecimal baseSalary, String teacherName,
                              String teacherPhone, String bankAccount) {}

    private void applyStructure(Payslip p, Long structureId) {
        List<SalaryRule> rules = ruleRepo.findByStructureIdOrderBySequenceAsc(structureId);
        BigDecimal base = p.getBaseSalary();
        BigDecimal gross = base;
        // 1) gains → brut
        for (SalaryRule r : rules) {
            if (r.getLineType() != PayslipLineType.EARNING) continue;
            BigDecimal amount = computeAmount(r, base, base);
            saveLine(p, r.getLabel(), PayslipLineType.EARNING, amount);
            gross = gross.add(amount);
        }
        // 2) retenues (sur base ou brut)
        for (SalaryRule r : rules) {
            if (r.getLineType() != PayslipLineType.DEDUCTION) continue;
            BigDecimal amount = computeAmount(r, base, gross);
            saveLine(p, r.getLabel(), PayslipLineType.DEDUCTION, amount);
        }
    }

    private BigDecimal computeAmount(SalaryRule r, BigDecimal base, BigDecimal gross) {
        BigDecimal v = r.getValue() != null ? r.getValue() : BigDecimal.ZERO;
        return switch (r.getComputation()) {
            case FIXED -> v;
            case PERCENT_OF_BASE -> base.multiply(v).divide(HUNDRED, 2, RoundingMode.HALF_UP);
            case PERCENT_OF_GROSS -> gross.multiply(v).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        };
    }

    private void saveLine(Payslip p, String label, PayslipLineType type, BigDecimal amount) {
        lineRepo.save(PayslipLine.builder()
                .tenantId(p.getTenantId()).payslipId(p.getId()).label(label).lineType(type).amount(amount).build());
    }

    // ── Lignes manuelles + recalcul ──────────────────────────────────────────

    public Payslip addLine(Long payslipId, String label, PayslipLineType type, BigDecimal amount) {
        Payslip p = mutable(payslipId);
        saveLine(p, label, type, amount != null ? amount : BigDecimal.ZERO);
        return recompute(payslipId);
    }

    public Payslip removeLine(Long payslipId, Long lineId) {
        mutable(payslipId);
        lineRepo.deleteById(lineId);
        return recompute(payslipId);
    }

    public Payslip recompute(Long payslipId) {
        Payslip p = loadPayslip(payslipId);
        BigDecimal base = p.getBaseSalary() != null ? p.getBaseSalary() : BigDecimal.ZERO;
        BigDecimal earnings = BigDecimal.ZERO;
        BigDecimal deductions = BigDecimal.ZERO;
        for (PayslipLine l : lineRepo.findByPayslipId(payslipId)) {
            if (l.getLineType() == PayslipLineType.EARNING) earnings = earnings.add(l.getAmount());
            else deductions = deductions.add(l.getAmount());
        }
        BigDecimal gross = base.add(earnings);
        p.setGrossSalary(gross);
        p.setNetSalary(gross.subtract(deductions));
        return payslipRepo.save(p);
    }

    // ── Workflow ─────────────────────────────────────────────────────────────

    public Payslip approve(Long id) {
        Payslip p = loadPayslip(id);
        if (p.getStatus() == PayslipStatus.PAID) throw new BusinessRuleException("Fiche déjà payée");
        p.setStatus(PayslipStatus.VALIDATED);
        return payslipRepo.save(p);
    }

    public Payslip markPaid(Long id, String paymentMethod, Long paidByUserId) {
        Payslip p = loadPayslip(id);
        if (p.getStatus() == PayslipStatus.DRAFT) throw new BusinessRuleException("Validez la fiche avant de la payer");
        // Empêche un double-clic/rejeu de repayer une fiche déjà payée — sans ce garde-fou, la retenue
        // scolarité (voir ci-dessous) serait répercutée deux fois sur les factures des enfants.
        if (p.getStatus() == PayslipStatus.PAID) throw new BusinessRuleException("Fiche déjà payée");
        p.setStatus(PayslipStatus.PAID);
        // Jamais de présomption "virement bancaire" quand rien n'est fourni (§retour utilisateur : une
        // fiche payée en espèces affichait "Virement bancaire" sur le PDF) — le mode de paiement réel
        // est désormais toujours choisi explicitement côté appelant (modal "Marquer comme payée").
        p.setPaymentMethod(paymentMethod);
        p.setPaidAt(LocalDateTime.now());
        // Comptable ayant traité le paiement — imprimé sous la signature "Signature et Cachet de
        // l'Établissement" du PDF (demande utilisateur), résolu en nom à l'impression (voir pdfBytes),
        // jamais stocké en dur ici.
        p.setPaidByUserId(paidByUserId);
        Payslip saved = payslipRepo.save(p);
        // Répercute la retenue (solde réel des factures "avantage personnel", voir
        // applyStaffBenefitRemainderDeduction) sur les factures des enfants du salarié, maintenant que
        // la fiche est réellement payée (§payroll ↔ billing) — sans ça, l'école a bien été payée (via
        // la paie) mais la facturation continue d'afficher ces enfants comme débiteurs.
        salaryFeeDeductionUseCase.applyStaffBenefitRemainderAsInvoicePayment(saved.getTenantId(), saved.getEmployeeType(), saved.getTeacherId(),
                saved.getPeriodStart(), saved.getPeriodLabel());
        // Fait remonter la paie dans la trésorerie (sortie de caisse "Salaires") — jamais dans le
        // budget annuel voté, qui couvre le fonctionnement courant (carburant, kermesse, événements),
        // financièrement distinct de la masse salariale (toujours puisée dans les recettes de l'école).
        eventPublisher.publishEvent(new PayslipPaidEvent(saved.getTenantId(), saved.getId(), saved.getNetSalary(),
                saved.getTeacherName(), saved.getTeacherPhone(), saved.getPeriodLabel()));
        return saved;
    }

    public void delete(Long id) {
        Payslip p = loadPayslip(id);
        if (p.getStatus() != PayslipStatus.DRAFT) throw new BusinessRuleException("Seule une fiche en brouillon peut être supprimée");
        payslipRepo.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Payslip> find(Long tenantId, Long teacherId, PayslipStatus status) {
        if (teacherId != null) return payslipRepo.findByTenantIdAndTeacherId(tenantId, teacherId);
        if (status != null) return payslipRepo.findByTenantIdAndStatus(tenantId, status);
        return payslipRepo.findByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public Payslip findById(Long id) { return loadPayslip(id); }

    @Transactional(readOnly = true)
    public List<PayslipLine> lines(Long payslipId) { return lineRepo.findByPayslipId(payslipId); }

    /** Octets du PDF (A4 stylé, couleur d'école personnalisable) de la fiche de paie, avec les heures
     * dues/réalisées du mois pour un enseignant. {@code role}/{@code photoUrl} : résolus par l'appelant
     * (voir {@link gn.lacaeducation.payroll.application.port.out.PayslipPdfGenerator.Presentation}) —
     * cette méthode reste décorrélée du module staff, mais résout elle-même le nom du comptable ayant
     * payé la fiche (module iam, jamais staff). */
    @Transactional(readOnly = true)
    public byte[] pdfBytes(Long id) { return pdfBytes(id, null, null); }

    @Transactional(readOnly = true)
    public byte[] pdfBytes(Long id, String beneficiaryRole, String beneficiaryPhotoUrl) {
        return pdfBytes(id, beneficiaryRole, beneficiaryPhotoUrl, null);
    }

    /** @param generatedByUserId comptable en train de télécharger la fiche MAINTENANT (JWT de
     * l'appelant, {@code PayslipController#pdfDownload}) — nom de secours pour la signature quand la
     * fiche n'est pas encore payée ({@code paidByUserId} reste prioritaire : il désigne précisément qui
     * a traité le paiement, plus fiable qu'un simple téléchargement). {@code null} pour le self-service
     * enseignant ({@code minePdfDownload}), qui ne doit jamais apparaître comme comptable. */
    @Transactional(readOnly = true)
    public byte[] pdfBytes(Long id, String beneficiaryRole, String beneficiaryPhotoUrl, Long generatedByUserId) {
        Payslip p = loadPayslip(id);
        gn.lacaeducation.payroll.application.port.out.PayslipPdfGenerator.TeacherHours hours = null;
        boolean isTeacher = p.getEmployeeType() == null || p.getEmployeeType() == gn.lacaeducation.payroll.domain.EmployeeType.TEACHER;
        if (isTeacher && p.getTeacherId() != null && p.getPeriodStart() != null) {
            int year = p.getPeriodStart().getYear();
            int month = p.getPeriodStart().getMonthValue();
            TheoreticalMonthlyPay theoretical = computeTheoreticalMonthlyPay(p.getTeacherId(), year, month);
            int due = theoretical.normalMinutes() + theoretical.examMinutes();
            MonthlyPay mp = computeMonthlyPay(p.getTeacherId(), year, month);
            hours = new gn.lacaeducation.payroll.application.port.out.PayslipPdfGenerator.TeacherHours(
                    due, mp.realizedMinutes(), Math.max(0, due - mp.realizedMinutes()), mp.lateMinutes(), mp.earlyMinutes(),
                    mp.effectiveMinutes());
        }
        Long accountantUserId = p.getPaidByUserId() != null ? p.getPaidByUserId() : generatedByUserId;
        var presentation = new gn.lacaeducation.payroll.application.port.out.PayslipPdfGenerator.Presentation(
                beneficiaryRole, beneficiaryPhotoUrl, resolveAccountantName(accountantUserId));
        return pdfGenerator.generate(p, lineRepo.findByPayslipId(id), hours, presentation);
    }

    /** Nom du comptable ayant marqué la fiche payée, pour la signature du PDF — ne bloque jamais la
     * génération si non résolu (même logique défensive que {@code ReceiptService#resolveCollectedByName}). */
    private String resolveAccountantName(Long userId) {
        if (userId == null) return null;
        try {
            return userPublicInfoProvider.findUser(userId)
                    .map(u -> (u.firstName() + " " + u.lastName()).trim())
                    .filter(n -> !n.isBlank())
                    .orElse(null);
        } catch (RuntimeException e) {
            log.warn("Nom du comptable non résolu pour la fiche de paie (utilisateur {}) : {}", userId, e.getMessage());
            return null;
        }
    }

    private Payslip mutable(Long id) {
        Payslip p = loadPayslip(id);
        if (p.getStatus() == PayslipStatus.PAID) throw new BusinessRuleException("Fiche payée : non modifiable");
        return p;
    }

    private Payslip loadPayslip(Long id) {
        return payslipRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Payslip", id));
    }

    private SalaryStructure loadStructure(Long id) {
        return structureRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("SalaryStructure", id));
    }
}
