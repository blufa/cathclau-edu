package gn.lacaeducation.payroll.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.attendance.TeacherWorkProvider;
import gn.lacaeducation.lessons.TeacherTeachingProvider;
import gn.lacaeducation.payroll.application.port.in.SalaryFeeDeductionUseCase;
import gn.lacaeducation.payroll.application.port.out.PayslipLineRepository;
import gn.lacaeducation.payroll.application.port.out.PayslipPdfGenerator;
import gn.lacaeducation.payroll.application.port.out.PayslipRepository;
import gn.lacaeducation.payroll.application.port.out.SalaryRuleRepository;
import gn.lacaeducation.payroll.application.port.out.SalaryStructureRepository;
import gn.lacaeducation.payroll.application.port.out.TeacherCompensationRepository;
import gn.lacaeducation.payroll.domain.EmployeeType;
import gn.lacaeducation.payroll.domain.PayMode;
import gn.lacaeducation.payroll.domain.Payslip;
import gn.lacaeducation.payroll.domain.PayslipLine;
import gn.lacaeducation.payroll.domain.PayslipLineType;
import gn.lacaeducation.payroll.domain.TeacherCompensation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    @Mock PayslipRepository payslipRepo;
    @Mock PayslipLineRepository lineRepo;
    @Mock SalaryStructureRepository structureRepo;
    @Mock SalaryRuleRepository ruleRepo;
    @Mock PayslipPdfGenerator pdfGenerator;
    @Mock TeacherCompensationRepository compRepo;
    @Mock gn.lacaeducation.payroll.application.port.out.TeacherVacationPayRepository vacationPayRepo;
    @Mock TeacherTeachingProvider teachingProvider;
    @Mock TeacherWorkProvider workProvider;
    @Mock SalaryFeeDeductionUseCase salaryFeeDeductionUseCase;
    @Mock AcademicsDataProvider academics;
    @Mock org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks PayrollService payrollService;

    @Test
    void teacherPayrollMass_sumsFixedSalaryDirectlyAndHourlyViaTheoreticalPay() {
        TeacherCompensation fixedTeacher = TeacherCompensation.builder().teacherId(1L)
                .payMode(PayMode.FIXED).hourlyRateNormal(BigDecimal.ZERO).hourlyRateExam(BigDecimal.ZERO)
                .fixedSalary(new BigDecimal("1500000")).build();
        TeacherCompensation hourlyTeacher = TeacherCompensation.builder().teacherId(2L)
                .payMode(PayMode.HOURLY).hourlyRateNormal(new BigDecimal("20000")).hourlyRateExam(new BigDecimal("25000"))
                .fixedSalary(BigDecimal.ZERO).build();

        when(compRepo.findByTenantId(9L)).thenReturn(List.of(fixedTeacher, hourlyTeacher));
        // Le calcul théorique de l'enseignant HOURLY interroge compRepo.findByTeacherId puis
        // l'emploi du temps (vide ici -> aucune heure théorique, gross = 0).
        when(compRepo.findByTeacherId(2L)).thenReturn(Optional.of(hourlyTeacher));
        when(academics.findScheduleByTeacher(2L)).thenReturn(List.of());

        PayrollService.TeacherPayrollMass mass = payrollService.teacherPayrollMass(9L, 2026, 3);

        assertThat(mass.teacherCount()).isEqualTo(2);
        assertThat(mass.totalHourlyRateNormal()).isEqualByComparingTo("20000");
        assertThat(mass.totalHourlyRateExam()).isEqualByComparingTo("25000");
        // FIXED teacher contributes his fixed salary directly ; HOURLY teacher has no scheduled
        // hours in this test, so his theoretical gross pay is 0.
        assertThat(mass.monthlyPayroll()).isEqualByComparingTo("1500000");
    }

    @Test
    void teacherPayrollMass_noTeachers_isZero() {
        when(compRepo.findByTenantId(9L)).thenReturn(List.of());

        PayrollService.TeacherPayrollMass mass = payrollService.teacherPayrollMass(9L, 2026, 3);

        assertThat(mass.teacherCount()).isZero();
        assertThat(mass.totalHourlyRateNormal()).isEqualByComparingTo("0");
        assertThat(mass.monthlyPayroll()).isEqualByComparingTo("0");
    }

    /** La durée choisie séance par séance dans le modal « Réaliser » (théorique OU réellement pointée)
     * est la seule source de vérité pour la paie — le retard/départ anticipé cumulé du pointage reste
     * calculé et affiché sur la fiche de paie (information), mais n'est plus déduit une seconde fois du
     * temps réalisé : sans quoi choisir la durée RÉELLE pointée (qui exclut déjà le retard) pénalisait
     * l'enseignant deux fois pour le même retard. */
    @Test
    void computeMonthlyPay_doesNotDeductLateOrEarlyFromRealizedMinutes() {
        TeacherCompensation comp = TeacherCompensation.builder().teacherId(5L)
                .payMode(PayMode.HOURLY).hourlyRateNormal(new BigDecimal("10000")).hourlyRateExam(BigDecimal.ZERO)
                .fixedSalary(BigDecimal.ZERO).build();
        when(compRepo.findByTeacherId(5L)).thenReturn(Optional.of(comp));
        when(teachingProvider.realizedMinutesByClass(eq(5L), any(), any())).thenReturn(Map.of(20L, 120));
        when(workProvider.monthlyLateEarlyByClass(5L, 2026, 9))
                .thenReturn(Map.of(20L, new TeacherWorkProvider.ClassLateEarly(351, 40)));
        when(academics.findClass(20L)).thenReturn(Optional.empty());

        PayrollService.MonthlyPay pay = payrollService.computeMonthlyPay(5L, 2026, 9);

        // 120 min réalisées (choisies dans le modal) = 2h x 10 000 GNF, sans déduction du retard/départ
        // anticipé.
        assertThat(pay.effectiveMinutes()).isEqualTo(120);
        assertThat(pay.teachingPay()).isEqualByComparingTo("20000");
        assertThat(pay.grossPay()).isEqualByComparingTo("20000");
        // Retard/départ anticipé restent réellement calculés et exposés (affichage informatif seulement).
        assertThat(pay.lateMinutes()).isEqualTo(351);
        assertThat(pay.earlyMinutes()).isEqualTo(40);
    }

    /** Fiche combinée : personnel de direction (salaire fixe) qui enseigne aussi (heures réalisées) —
     * une seule fiche, salaire de base + une ligne EARNING pour les heures d'enseignement. */
    @Test
    void generateCombinedPayslip_addsBaseSalaryPlusTeachingHoursLine() {
        Long employeeId = 5L;
        Long linkedTeacherId = 42L;
        BigDecimal baseSalary = new BigDecimal("2000000");

        when(salaryFeeDeductionUseCase.computeFeeDeduction(anyLong(), eq(employeeId)))
                .thenReturn(new SalaryFeeDeductionUseCase.FeeDeduction(BigDecimal.ZERO, null, 0));

        AtomicReference<Payslip> saved = new AtomicReference<>();
        when(payslipRepo.save(any())).thenAnswer(i -> {
            Payslip p = i.getArgument(0);
            if (p.getId() == null) p.setId(100L);
            saved.set(p);
            return p;
        });
        when(payslipRepo.findById(100L)).thenAnswer(i -> Optional.of(saved.get()));

        List<PayslipLine> lines = new ArrayList<>();
        when(lineRepo.save(any())).thenAnswer(i -> {
            PayslipLine l = i.getArgument(0);
            lines.add(l);
            return l;
        });
        when(lineRepo.findByPayslipId(100L)).thenAnswer(i -> lines);

        // Enseignant lié : 2h réalisées (120 min), aucun retard/départ anticipé, tarif normal 10 000.
        when(teachingProvider.realizedMinutesByClass(eq(linkedTeacherId), any(), any())).thenReturn(Map.of(1L, 120));
        when(workProvider.monthlyLateEarlyByClass(eq(linkedTeacherId), eq(2026), eq(3))).thenReturn(Map.of());
        when(compRepo.findByTeacherId(linkedTeacherId)).thenReturn(Optional.of(
                TeacherCompensation.builder().teacherId(linkedTeacherId).payMode(PayMode.HOURLY)
                        .hourlyRateNormal(new BigDecimal("10000")).hourlyRateExam(BigDecimal.ZERO)
                        .fixedSalary(BigDecimal.ZERO).build()));

        Payslip result = payrollService.generateCombinedPayslip(9L, employeeId, "Mariam Bah",
                EmployeeType.DIRECTION, baseSalary, linkedTeacherId, 2026, 3, null);

        assertThat(result.getEmployeeType()).isEqualTo(EmployeeType.DIRECTION);
        assertThat(result.getTeacherId()).isEqualTo(employeeId);
        assertThat(result.getBaseSalary()).isEqualByComparingTo(baseSalary);
        // 2h × 10 000 = 20 000 de paie d'enseignement, ajoutée en plus du salaire de base fixe.
        assertThat(result.getGrossSalary()).isEqualByComparingTo(baseSalary.add(new BigDecimal("20000.00")));
        assertThat(lines).hasSize(1);
        assertThat(lines.get(0).getLineType()).isEqualTo(PayslipLineType.EARNING);
    }

    @Test
    void generateCombinedPayslip_withoutLinkedTeacher_throws() {
        assertThatThrownBy(() -> payrollService.generateCombinedPayslip(9L, 5L, "Mariam Bah",
                EmployeeType.DIRECTION, new BigDecimal("2000000"), null, 2026, 3, null))
                .isInstanceOf(gn.lacaeducation.common.BusinessRuleException.class);
    }

    /** Fait remonter la paie dans la trésorerie (§payroll ↔ accounting), jamais dans le budget annuel
     * voté (masse salariale toujours puisée dans les recettes de l'école, pas dans ce budget) : au
     * paiement d'une fiche, l'événement porte le net à virer. */
    @Test
    void markPaid_publishesPayslipPaidEventWithNetSalary() {
        Payslip validated = Payslip.builder().id(1L).tenantId(9L).status(gn.lacaeducation.payroll.domain.PayslipStatus.VALIDATED)
                .netSalary(new BigDecimal("850000")).teacherName("Mamadou Diallo").teacherPhone("622000000")
                .periodLabel("Mars 2026").build();
        when(payslipRepo.findById(1L)).thenReturn(java.util.Optional.of(validated));
        when(payslipRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        payrollService.markPaid(1L, "BANK_TRANSFER");

        org.mockito.Mockito.verify(eventPublisher).publishEvent(
                new gn.lacaeducation.payroll.application.event.PayslipPaidEvent(9L, 1L, new BigDecimal("850000"),
                        "Mamadou Diallo", "622000000", "Mars 2026"));
    }

    /** Empêche un doublon silencieux (double-clic sur « Générer en lot »/« Générer une fiche ») : une
     * fiche existe déjà pour ce salarié sur cette période exacte (même type d'employé). */
    @Test
    void generate_duplicateForSameTeacherAndPeriod_throwsConflict() {
        when(payslipRepo.existsByTenantIdAndTeacherIdAndPeriodLabelAndEmployeeType(9L, 7L, "Mars 2026", EmployeeType.TEACHER))
                .thenReturn(true);

        assertThatThrownBy(() -> payrollService.generate(new PayrollService.GenerateCommand(
                9L, null, 7L, "M. Diallo", "Mars 2026",
                null, null, new BigDecimal("300000"), null, null,
                null, null, null, null, null, null, null)))
                .isInstanceOf(gn.lacaeducation.common.ConflictException.class);
    }

}
