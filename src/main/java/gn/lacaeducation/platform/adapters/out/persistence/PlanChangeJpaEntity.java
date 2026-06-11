package gn.lacaeducation.platform.adapters.out.persistence;

import gn.lacaeducation.platform.domain.PlanChangeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "plan_change")
@Getter
@Setter
@NoArgsConstructor
public class PlanChangeJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subscription_id", nullable = false)
    private Long subscriptionId;

    @Column(name = "old_plan_id", nullable = false)
    private Long oldPlanId;

    @Column(name = "new_plan_id", nullable = false)
    private Long newPlanId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanChangeType type;

    @Column(name = "prorated_amount", precision = 12, scale = 2)
    private BigDecimal proratedAmount;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
