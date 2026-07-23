package gn.lacaeducation.payroll.application.service;

import gn.lacaeducation.attendance.TeacherWorkProvider;
import gn.lacaeducation.attendance.TeacherWorkProvider.MonthlyWork;
import gn.lacaeducation.payroll.application.port.in.SalaryFeeDeductionUseCase;
import gn.lacaeducation.payroll.application.port.in.SalaryFeeDeductionUseCase.FeeDeduction;
import gn.lacaeducation.payroll.application.port.out.PayslipLineRepository;
import gn.lacaeducation.payroll.application.port.out.PayslipRepository;
import gn.lacaeducation.payroll.domain.Payslip;
import gn.lacaeducation.payroll.domain.PayslipLine;
import gn.lacaeducation.payroll.domain.PayslipLineType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PayrollAbsenceDeductionTest {

    @Mock PayslipRepository payslipRepo;
    @Mock PayslipLineRepository lineRepo;
    @Mock gn.lacaeducation.payroll.application.port.out.SalaryStructureRepository structureRepo;
    @Mock gn.lacaeducation.payroll.application.port.out.SalaryRuleRepository ruleRepo;
    @Mock gn.lacaeducation.payroll.application.port.out.PayslipPdfGenerator pdfGenerator;
    @Mock gn.lacaeducation.payroll.application.port.out.TeacherCompensationRepository compRepo;
    @Mock gn.lacaeducation.lessons.TeacherTeachingProvider teachingProvider;
    @Mock TeacherWorkProvider workProvider;
    @Mock SalaryFeeDeductionUseCase salaryFeeDeductionUseCase;
    @Mock gn.lacaeducation.academics.AcademicsDataProvider academics;

    PayrollService service;
    List<PayslipLine> savedLines;

    @BeforeEach
    void setUp() {
        service = new PayrollService(payslipRepo, lineRepo, structureRepo, ruleRepo, pdfGenerator,
                compRepo, teachingProvider, workProvider, salaryFeeDeductionUseCase, academics);
        savedLines = new ArrayList<>();

        // payslipRepo.save : assigne un id au premier passage, renvoie l'entité.
        when(payslipRepo.save(any())).thenAnswer(i -> {
            Payslip p = i.getArgument(0);
            if (p.getId() == null) p.setId(1L);
            return p;
        });
        when(payslipRepo.findById(anyLong())).thenAnswer(i -> java.util.Optional.of(
                Payslip.builder().id(1L).tenantId(9L).teacherId(7L).baseSalary(BigDecimal.valueOf(300000)).build()));
        // lineRepo capture les lignes créées.
        when(lineRepo.save(any())).thenAnswer(i -> { savedLines.add(i.getArgument(0)); return i.getArgument(0); });
        when(lineRepo.findByPayslipId(anyLong())).thenAnswer(i -> savedLines);
        // Aucune retenue de frais scolaires dans ce scénario.
        lenient().when(salaryFeeDeductionUseCase.computeFeeDeduction(anyLong(), anyLong()))
                .thenReturn(new FeeDeduction(BigDecimal.ZERO, null, 0));
    }

    @Test
    void generate_autoDerivesAbsenceDeductionFromAttendance() {
        // 2 jours d'absence sur 20 jours planifiés (18 présents + 2 absents).
        when(workProvider.monthlyWork(7L, 2026, 3)).thenReturn(new MonthlyWork(0, 0, 0, 0, 18, 2));

        // Aucune valeur d'absence fournie → auto-dérivation attendue.
        service.generate(new PayrollService.GenerateCommand(
                9L, null, 7L, "M. Diallo", "Mars 2026",
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31),
                BigDecimal.valueOf(300000), null, null,
                null, null, null, null, null));

        // Retenue = (300000 / 20) × 2 = 30000, libellé "Absences (2 j)".
        PayslipLine absence = savedLines.stream()
                .filter(l -> l.getLabel() != null && l.getLabel().startsWith("Absences"))
                .findFirst().orElseThrow();
        assertThat(absence.getLineType()).isEqualTo(PayslipLineType.DEDUCTION);
        assertThat(absence.getLabel()).isEqualTo("Absences (2 j)");
        assertThat(absence.getAmount()).isEqualByComparingTo("30000");
    }

    @Test
    void generate_explicitAbsentDays_overridesAttendance() {
        // Valeur explicite fournie → le provider ne doit pas être consulté.
        service.generate(new PayrollService.GenerateCommand(
                9L, null, 7L, "M. Diallo", "Mars 2026",
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31),
                BigDecimal.valueOf(300000), null, null,
                1, 20, null, null, null));

        PayslipLine absence = savedLines.stream()
                .filter(l -> l.getLabel() != null && l.getLabel().startsWith("Absences"))
                .findFirst().orElseThrow();
        assertThat(absence.getLabel()).isEqualTo("Absences (1 j)");
        assertThat(absence.getAmount()).isEqualByComparingTo("15000"); // 300000/20 × 1
        org.mockito.Mockito.verify(workProvider, never()).monthlyWork(anyLong(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void generate_nonTeacher_doesNotConsultAttendance() {
        // Employé administratif : pas de pointage par séance → pas d'auto-dérivation d'absence.
        service.generate(new PayrollService.GenerateCommand(
                9L, gn.lacaeducation.payroll.domain.EmployeeType.ADMINISTRATIVE, 7L, "Mme Sow", "Mars 2026",
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31),
                BigDecimal.valueOf(300000), null, null,
                null, null, null, null, null));

        assertThat(savedLines).noneMatch(l -> l.getLabel() != null && l.getLabel().startsWith("Absences"));
        org.mockito.Mockito.verify(workProvider, never()).monthlyWork(anyLong(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void generate_noAbsence_noDeductionLine() {
        when(workProvider.monthlyWork(7L, 2026, 3)).thenReturn(new MonthlyWork(0, 0, 0, 0, 20, 0));

        service.generate(new PayrollService.GenerateCommand(
                9L, null, 7L, "M. Diallo", "Mars 2026",
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31),
                BigDecimal.valueOf(300000), null, null,
                null, null, null, null, null));

        assertThat(savedLines).noneMatch(l -> l.getLabel() != null && l.getLabel().startsWith("Absences"));
    }
}
