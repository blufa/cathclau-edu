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
}
