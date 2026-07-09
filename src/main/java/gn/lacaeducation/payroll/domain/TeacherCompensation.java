package gn.lacaeducation.payroll.domain;

import lombok.*;

import java.math.BigDecimal;

/** Paramètres de rémunération d'un enseignant / membre du personnel. */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TeacherCompensation {
    private Long id;
    private Long tenantId;
    private Long teacherId;
    private PayMode payMode;
    private BigDecimal hourlyRate;
    private BigDecimal fixedSalary;
}
