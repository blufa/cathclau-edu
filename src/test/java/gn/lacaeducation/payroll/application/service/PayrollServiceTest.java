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
import gn.lacaeducation.payroll.domain.PayMode;
import gn.lacaeducation.payroll.domain.TeacherCompensation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    @Mock PayslipRepository payslipRepo;
    @Mock PayslipLineRepository lineRepo;
    @Mock SalaryStructureRepository structureRepo;
    @Mock SalaryRuleRepository ruleRepo;
    @Mock PayslipPdfGenerator pdfGenerator;
    @Mock TeacherCompensationRepository compRepo;
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
}
