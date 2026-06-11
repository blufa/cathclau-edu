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
public class Subscription {
    private Long id;
    private Long tenantId;
    private Long planId;
    private LocalDate startDate;
    private LocalDate endDate;
    private SubscriptionStatus status;
    private Boolean autoRenewal;
    private BigDecimal amount;
    private LocalDateTime createdAt;

    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE && LocalDate.now().isBefore(endDate);
    }
}
