package gn.lacaeducation.platform.adapters.out.persistence;

import gn.lacaeducation.platform.domain.PlanCode;
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

import java.math.BigDecimal;

@Entity
@Table(name = "plan")
@Getter
@Setter
@NoArgsConstructor
public class PlanJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private PlanCode code;

    @Column(name = "student_limit")
    private Integer studentLimit;

    @Column(name = "monthly_price", precision = 12, scale = 2)
    private BigDecimal monthlyPrice;

    @Column(name = "annual_price", precision = 12, scale = 2)
    private BigDecimal annualPrice;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Boolean archived = false;

    /** Trial plan = price 0 + trialDays; one plan can be flagged as the default trial. */
    @Column(name = "is_trial", nullable = false)
    private Boolean trial = false;

    @Column(name = "trial_days")
    private Integer trialDays;

    @Column(name = "default_trial", nullable = false)
    private Boolean defaultTrial = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", length = 16)
    private gn.lacaeducation.platform.domain.PlanType planType;

    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    private gn.lacaeducation.platform.domain.Periodicity periodicity;

    @Column(name = "user_limit")
    private Integer userLimit;

    @Column(name = "sms_limit")
    private Integer smsLimit;

    @Column(name = "public_visible", nullable = false)
    private Boolean publicVisible = true;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(nullable = false)
    private Boolean highlighted = false;
}
