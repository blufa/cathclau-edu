package gn.lacaeducation.platform.domain;

import gn.lacaeducation.common.BusinessRuleException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

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

    /**
     * Strict subscription state machine (features/01-platform.md §1.3):
     * EN_ATTENTE → ACTIF → (SUSPENDU | EXPIRE | ANNULE). Renewal sends an
     * ACTIVE/SUSPENDED/EXPIRED subscription back to PENDING (new invoice pending payment).
     */
    private static final Map<SubscriptionStatus, Set<SubscriptionStatus>> ALLOWED =
            new EnumMap<>(SubscriptionStatus.class);
    static {
        ALLOWED.put(SubscriptionStatus.PENDING,
                Set.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELLED));
        ALLOWED.put(SubscriptionStatus.ACTIVE,
                Set.of(SubscriptionStatus.SUSPENDED, SubscriptionStatus.EXPIRED,
                        SubscriptionStatus.CANCELLED, SubscriptionStatus.PENDING));
        ALLOWED.put(SubscriptionStatus.SUSPENDED,
                Set.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.EXPIRED,
                        SubscriptionStatus.CANCELLED, SubscriptionStatus.PENDING));
        ALLOWED.put(SubscriptionStatus.EXPIRED,
                Set.of(SubscriptionStatus.PENDING, SubscriptionStatus.CANCELLED));
        ALLOWED.put(SubscriptionStatus.CANCELLED, Set.of());
    }

    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE && LocalDate.now().isBefore(endDate);
    }

    public boolean canTransitionTo(SubscriptionStatus target) {
        return ALLOWED.getOrDefault(status, Set.of()).contains(target);
    }

    /** Applies a status transition, rejecting illegal ones (mapped to HTTP 422). */
    public void transitionTo(SubscriptionStatus target) {
        if (status == target) {
            return;
        }
        if (!canTransitionTo(target)) {
            throw new BusinessRuleException(
                    "Illegal subscription transition: " + status + " → " + target);
        }
        this.status = target;
    }
}
