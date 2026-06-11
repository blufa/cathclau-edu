package gn.lacaeducation.platform.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanChange {
    private Long id;
    private Long subscriptionId;
    private Long oldPlanId;
    private Long newPlanId;
    private PlanChangeType type;
    private BigDecimal proratedAmount;
    private LocalDate effectiveDate;
    private LocalDateTime createdAt;
}
