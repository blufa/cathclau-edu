package gn.lacaeducation.payroll.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.academics.ClassPublicInfo;
import gn.lacaeducation.academics.ScheduleSlotPublicInfo;
import gn.lacaeducation.attendance.TeacherWorkProvider;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
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
    private final TeacherTeachingProvider teachingProvider;
    private final TeacherWorkProvider workProvider;
    private final gn.lacaeducation.payroll.application.port.in.SalaryFeeDeductionUseCase salaryFeeDeductionUseCase;
    private final AcademicsDataProvider academics;

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    // ── Rémunération (mode horaire / fixe) & calcul mensuel ───────────────────

    public record MonthlyPay(Long teacherId, int year, int month, String payMode,
                             int realizedMinutes, int lateMinutes, int effectiveMinutes,
                             BigDecimal hourlyRate, BigDecimal fixedSalary, BigDecimal teachingPay, BigDecimal grossPay) {}

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

    /**
     * Paie mensuelle basée sur les heures RÉALISÉES (séances « réalisées » du cahier de textes),
     * diminuées des retards (pointage par séance). Horaire : heures effectives × tarif ; Fixe : salaire
     * fixe + heures de cours effectives × tarif (cas direction qui enseigne aussi).
     */
    @Transactional(readOnly = true)
    public MonthlyPay computeMonthlyPay(Long teacherId, int year, int month) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        int realized = teachingProvider.realizedMinutes(teacherId, from, to);
        int late = workProvider.monthlyWork(teacherId, year, month).lateMinutes();
        int effective = Math.max(0, realized - late);

        TeacherCompensation comp = compRepo.findByTeacherId(teacherId).orElse(null);
        PayMode mode = comp != null && comp.getPayMode() != null ? comp.getPayMode() : PayMode.HOURLY;
        // Les heures réalisées (cahier de textes) ne distinguent pas la typologie de classe : on
        // applique le tarif "classe normale" par défaut (voir computeTheoreticalMonthlyPay pour le
        // calcul automatique par typologie, basé sur l'emploi du temps — §6).
        BigDecimal rate = comp != null && comp.getHourlyRateNormal() != null ? comp.getHourlyRateNormal() : BigDecimal.ZERO;
        BigDecimal fixed = comp != null && comp.getFixedSalary() != null ? comp.getFixedSalary() : BigDecimal.ZERO;

        BigDecimal effHours = BigDecimal.valueOf(effective).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        BigDecimal teachingPay = effHours.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal gross = mode == PayMode.FIXED ? fixed.add(teachingPay) : teachingPay;
        return new MonthlyPay(teacherId, year, month, mode.name(), realized, late, effective, rate, fixed, teachingPay, gross);
    }

    private static final String[] FR_MONTHS = {"Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
            "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"};

    /** Génère automatiquement la fiche de paie du mois à partir des heures réalisées (brut calculé). */
    public Payslip generateFromHours(Long tenantId, Long teacherId, String teacherName, int year, int month, Long structureId) {
        MonthlyPay mp = computeMonthlyPay(teacherId, year, month);
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        String periodLabel = FR_MONTHS[month - 1] + " " + year;
        String notes = String.format("%dh%02d réalisées − %d min de retard = %dh%02d effectives%s",
                mp.realizedMinutes() / 60, mp.realizedMinutes() % 60, mp.lateMinutes(),
                mp.effectiveMinutes() / 60, mp.effectiveMinutes() % 60,
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

    /**
     * Calcule automatiquement, sans saisie manuelle, le nombre d'heures théoriques que l'enseignant
     * devait effectuer ce mois-ci (projection de son emploi du temps hebdomadaire sur les
     * occurrences de chaque jour ouvré du mois), séparément pour les classes normales et les classes
     * d'examen, puis applique le tarif horaire correspondant à chacune (§6).
     */
    @Transactional(readOnly = true)
    public TheoreticalMonthlyPay computeTheoreticalMonthlyPay(Long teacherId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        Map<DayOfWeek, Integer> occurrencesByDay = new HashMap<>();
        for (int day = 1; day <= ym.lengthOfMonth(); day++) {
            occurrencesByDay.merge(ym.atDay(day).getDayOfWeek(), 1, Integer::sum);
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
            ClassPublicInfo classInfo = slot.classId() != null
                    ? classCache.computeIfAbsent(slot.classId(), id -> academics.findClass(id).orElse(null))
                    : null;
            boolean exam = classInfo != null && "EXAMEN".equals(classInfo.classType());
            if (exam) examMinutes += minutes; else normalMinutes += minutes;
        }

        TeacherCompensation comp = compRepo.findByTeacherId(teacherId).orElse(null);
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
            int realized = computeMonthlyPay(p.getTeacherId(), year, month).realizedMinutes();
            hours = new gn.lacaeducation.payroll.application.port.out.PayslipPdfGenerator.TeacherHours(
                    due, realized, Math.max(0, due - realized));
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
