package gn.lacaeducation.payroll.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.academics.ClassPublicInfo;
import gn.lacaeducation.academics.ScheduleSlotPublicInfo;
import gn.lacaeducation.attendance.TeacherWorkProvider;
import gn.lacaeducation.calendar.CalendarDataProvider;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.lessons.TeacherTeachingProvider;
import gn.lacaeducation.payroll.application.port.out.PayslipLineRepository;
import gn.lacaeducation.payroll.application.port.out.PayslipRepository;
import gn.lacaeducation.payroll.application.port.out.SalaryRuleRepository;
import gn.lacaeducation.payroll.application.port.out.SalaryStructureRepository;
import gn.lacaeducation.payroll.application.port.out.TeacherCompensationRepository;
import gn.lacaeducation.payroll.domain.*;
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
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Paie (inspiré OpenEduCat Payroll) : règles de paie, structures salariales, génération de fiches
 * (individuelle ou en lot) avec calcul base → brut → net, puis workflow DRAFT → VALIDATED → PAID.
 * Le salaire de base provient de la rémunération de la fiche enseignant (transmis par l'appelant) ;
 * aucune dépendance directe au module staff.
 */
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
    private final AcademicsDataProvider academics;
    private final CalendarDataProvider calendar;

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
     * Paie mensuelle basée sur les heures RÉALISÉES (séances « réalisées » du cahier de textes),
     * diminuées des retards et départs anticipés cumulés (pointage par séance), le tout réparti par
     * typologie de classe (normal/examen, §6) pour appliquer le bon tarif à chaque part — un
     * enseignant qui donne cours à la fois en classe normale et en classe d'examen est ainsi payé
     * correctement sur chaque partie, retards et départs anticipés déduits de la bonne typologie.
     * Horaire : heures effectives × tarif (normal + examen) ; Fixe : salaire fixe + heures de cours
     * effectives × tarif (cas direction qui enseigne aussi).
     */
    @Transactional(readOnly = true)
    public MonthlyPay computeMonthlyPay(Long teacherId, int year, int month) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        Map<Long, Integer> realizedByClass = teachingProvider.realizedMinutesByClass(teacherId, from, to);
        Map<Long, TeacherWorkProvider.ClassLateEarly> lateEarlyByClass = workProvider.monthlyLateEarlyByClass(teacherId, year, month);

        Map<Long, ClassPublicInfo> classCache = new HashMap<>();
        int realized = 0, late = 0, early = 0;
        int normalRealized = 0, examRealized = 0;
        int normalLate = 0, examLate = 0, normalEarly = 0, examEarly = 0;
        for (var e : realizedByClass.entrySet()) {
            int minutes = e.getValue();
            realized += minutes;
            if (isExamClass(e.getKey(), classCache)) examRealized += minutes; else normalRealized += minutes;
        }
        for (var e : lateEarlyByClass.entrySet()) {
            int l = e.getValue().lateMinutes(), ea = e.getValue().earlyMinutes();
            late += l; early += ea;
            if (isExamClass(e.getKey(), classCache)) { examLate += l; examEarly += ea; }
            else { normalLate += l; normalEarly += ea; }
        }
        int normalEffective = Math.max(0, normalRealized - normalLate - normalEarly);
        int examEffective = Math.max(0, examRealized - examLate - examEarly);
        int effective = normalEffective + examEffective;

        TeacherCompensation comp = compRepo.findByTeacherId(teacherId).orElse(null);
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

    private static final String[] FR_MONTHS = {"Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
            "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"};

    /** Génère automatiquement la fiche de paie du mois à partir des heures réalisées (brut calculé). */
    public Payslip generateFromHours(Long tenantId, Long teacherId, String teacherName, int year, int month, Long structureId) {
        MonthlyPay mp = computeMonthlyPay(teacherId, year, month);
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        String periodLabel = FR_MONTHS[month - 1] + " " + year;
        String notes = String.format(
                "%dh%02d réalisées − %d min de retard − %d min de départ anticipé = %dh%02d effectives "
                        + "(dont %dh%02d classes normales, %dh%02d classes d'examen)%s",
                mp.realizedMinutes() / 60, mp.realizedMinutes() % 60, mp.lateMinutes(), mp.earlyMinutes(),
                mp.effectiveMinutes() / 60, mp.effectiveMinutes() % 60,
                mp.normalEffectiveMinutes() / 60, mp.normalEffectiveMinutes() % 60,
                mp.examEffectiveMinutes() / 60, mp.examEffectiveMinutes() % 60,
                "FIXED".equals(mp.payMode()) ? " + salaire fixe" : "");
        return generate(new GenerateCommand(tenantId, gn.lacaeducation.payroll.domain.EmployeeType.TEACHER,
                teacherId, teacherName, periodLabel, from, to,
                mp.grossPay(), structureId, notes, null, null, null, null, null));
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
     * configurés + paie mensuelle théorique (heures réelles × tarif normal/examen, ou salaire fixe). */
    public record TeacherPayrollMass(BigDecimal totalHourlyRateNormal, BigDecimal totalHourlyRateExam,
                                     BigDecimal monthlyPayroll, int teacherCount) {}

    @Transactional(readOnly = true)
    public TeacherPayrollMass teacherPayrollMass(Long tenantId, int year, int month) {
        List<TeacherCompensation> comps = compRepo.findByTenantId(tenantId);
        BigDecimal totalNormal = BigDecimal.ZERO;
        BigDecimal totalExam = BigDecimal.ZERO;
        BigDecimal monthly = BigDecimal.ZERO;
        for (TeacherCompensation c : comps) {
            totalNormal = totalNormal.add(c.getHourlyRateNormal() != null ? c.getHourlyRateNormal() : BigDecimal.ZERO);
            totalExam = totalExam.add(c.getHourlyRateExam() != null ? c.getHourlyRateExam() : BigDecimal.ZERO);
            if (c.getPayMode() == PayMode.FIXED) {
                monthly = monthly.add(c.getFixedSalary() != null ? c.getFixedSalary() : BigDecimal.ZERO);
            } else {
                monthly = monthly.add(computeTheoreticalMonthlyPay(c.getTeacherId(), year, month).grossPay());
            }
        }
        return new TeacherPayrollMass(totalNormal, totalExam, monthly, comps.size());
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
        List<TeacherCompensation> comps = compRepo.findByTenantId(tenantId);
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
        return new RealTeacherPayrollMass(monthly, totalLate, totalEarly, totalRealized, comps.size());
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
    public Payslip generateFromTimetable(Long tenantId, Long teacherId, String teacherName, int year, int month, Long structureId) {
        TheoreticalMonthlyPay tp = computeTheoreticalMonthlyPay(teacherId, year, month);
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        String periodLabel = FR_MONTHS[month - 1] + " " + year;
        String notes = String.format("%dh%02d (classes normales) + %dh%02d (classes d'examen), depuis l'emploi du temps%s",
                tp.normalMinutes() / 60, tp.normalMinutes() % 60, tp.examMinutes() / 60, tp.examMinutes() % 60,
                "FIXED".equals(tp.payMode()) ? " + salaire fixe" : "");
        return generate(new GenerateCommand(tenantId, gn.lacaeducation.payroll.domain.EmployeeType.TEACHER,
                teacherId, teacherName, periodLabel, from, to,
                tp.grossPay(), structureId, notes, null, null, null, null, null));
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
        if (linkedTeacherId == null) {
            throw new BusinessRuleException("Aucun enseignant lié à ce personnel — renseignez le lien dans sa fiche avant de générer la fiche combinée.");
        }
        String periodLabel = FR_MONTHS[month - 1] + " " + year;
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        Payslip p = generate(new GenerateCommand(tenantId, employeeType, employeeId, employeeName, periodLabel,
                from, to, baseSalary, structureId,
                "Fiche combinée : salaire de base (direction/administratif) + heures d'enseignement",
                null, null, null, null, null));

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
                                  BigDecimal overtimeHours, BigDecimal overtimeRate, BigDecimal overtimeMultiplier) {}

    public Payslip generate(GenerateCommand c) {
        if (c.teacherId() == null) throw new BusinessRuleException("Le salarié est requis");
        if (c.periodLabel() == null || c.periodLabel().isBlank()) throw new BusinessRuleException("La période est requise");
        gn.lacaeducation.payroll.domain.EmployeeType type = c.employeeType() != null
                ? c.employeeType() : gn.lacaeducation.payroll.domain.EmployeeType.TEACHER;
        BigDecimal base = c.baseSalary() != null ? c.baseSalary() : BigDecimal.ZERO;
        Payslip p = payslipRepo.save(Payslip.builder()
                .tenantId(c.tenantId()).employeeType(type)
                .teacherId(c.teacherId()).teacherName(c.teacherName()).periodLabel(c.periodLabel())
                .periodStart(c.periodStart()).periodEnd(c.periodEnd())
                .baseSalary(base).grossSalary(base).netSalary(base)
                .status(PayslipStatus.DRAFT).notes(c.notes()).createdAt(LocalDateTime.now()).build());
        if (c.structureId() != null) applyStructure(p, c.structureId());
        applyAttendanceAndOvertime(p, c);
        applySchoolFeeDeduction(p, c);
        return recompute(p.getId());
    }

    /**
     * Retenue sur salaire des frais scolaires des enfants du personnel (§7) : montant paramétrable
     * transféré du module scolarité vers la paie, ajouté comme ligne de retenue et historisé sur la fiche.
     */
    private void applySchoolFeeDeduction(Payslip p, GenerateCommand c) {
        var deduction = salaryFeeDeductionUseCase.computeFeeDeduction(c.tenantId(), c.teacherId());
        if (deduction.amount() != null && deduction.amount().signum() > 0) {
            String label = deduction.label() != null ? deduction.label() : "Frais scolaires enfants";
            saveLine(p, label + " (" + deduction.childrenCount() + " enfant(s))",
                    PayslipLineType.DEDUCTION, deduction.amount());
        }
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
                        w.teacherId(), null, periodLabel, start, end,
                        w.baseSalary(), structureId, null, null, null, null, null, null)))
                .toList();
    }

    public record TeacherWage(Long teacherId, BigDecimal baseSalary) {}

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

    public Payslip markPaid(Long id, String paymentMethod) {
        Payslip p = loadPayslip(id);
        if (p.getStatus() == PayslipStatus.DRAFT) throw new BusinessRuleException("Validez la fiche avant de la payer");
        p.setStatus(PayslipStatus.PAID);
        p.setPaymentMethod(paymentMethod != null ? paymentMethod : "BANK_TRANSFER");
        p.setPaidAt(LocalDateTime.now());
        return payslipRepo.save(p);
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

    /** Octets du PDF (A5 stylé) de la fiche de paie, avec les heures dues/réalisées du mois pour un enseignant. */
    @Transactional(readOnly = true)
    public byte[] pdfBytes(Long id) {
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
                    due, mp.realizedMinutes(), Math.max(0, due - mp.realizedMinutes()), mp.lateMinutes(), mp.earlyMinutes());
        }
        return pdfGenerator.generate(p, lineRepo.findByPayslipId(id), hours);
    }

    /** Fichier bancaire simple (CSV) : une ligne par fiche payée/validée avec le net à virer. */
    @Transactional(readOnly = true)
    public String bankFileCsv(Long tenantId) {
        StringBuilder sb = new StringBuilder("teacher_id;periode;net;mode\n");
        for (Payslip p : payslipRepo.findByTenantId(tenantId)) {
            if (p.getStatus() == PayslipStatus.DRAFT) continue;
            sb.append(p.getTeacherId()).append(';').append(p.getPeriodLabel()).append(';')
              .append(p.getNetSalary() == null ? "0" : p.getNetSalary().toPlainString()).append(';')
              .append(p.getPaymentMethod() == null ? "BANK_TRANSFER" : p.getPaymentMethod()).append('\n');
        }
        return sb.toString();
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
