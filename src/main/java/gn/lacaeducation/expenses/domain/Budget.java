package gn.lacaeducation.expenses.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Annual budget allocation for an expense category. */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Budget {
    private Long id;
    private Long tenantId;
    private Long categoryId;
    private String academicYear;
    private BigDecimal allocatedAmount;
}
