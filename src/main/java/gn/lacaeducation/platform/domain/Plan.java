package gn.lacaeducation.platform.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Plan {
    private Long id;
    private String name;
    private PlanCode code;
    private Integer studentLimit;
    private BigDecimal monthlyPrice;
    private BigDecimal annualPrice;
    private String description;
    private Boolean archived;
    private Boolean trial;
    private Integer trialDays;
    private Boolean defaultTrial;
    private PlanType planType;
    private Periodicity periodicity;
    private Integer userLimit;
    private Integer smsLimit;
    private Boolean publicVisible;
    private Integer displayOrder;
    private Boolean highlighted;

    public boolean isArchived() {
        return Boolean.TRUE.equals(archived);
    }
}
