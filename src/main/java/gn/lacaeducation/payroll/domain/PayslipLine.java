package gn.lacaeducation.payroll.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Une ligne d'une fiche de paie : un gain ou une retenue. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayslipLine {
    private Long id;
    private Long tenantId;
    private Long payslipId;
    private String label;
    private PayslipLineType lineType;
    private BigDecimal amount;
}
