package gn.lacaeducation.payroll.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Fiche de paie d'un enseignant/personnel pour une période : base + lignes → brut puis net. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payslip {
    private Long id;
    private Long tenantId;
    /** Nature du salarié : enseignant (défaut) ou personnel non-enseignant (§7.1). */
    private EmployeeType employeeType;
    /** Identifiant du salarié dans son référentiel (teacher ou employee selon {@link #employeeType}). */
    private Long teacherId;
    private String teacherName;
    private String periodLabel;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal baseSalary;
    private BigDecimal grossSalary;
    private BigDecimal netSalary;
    private PayslipStatus status;
    private String paymentMethod;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}
