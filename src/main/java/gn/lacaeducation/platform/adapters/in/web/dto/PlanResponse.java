package gn.lacaeducation.platform.adapters.in.web.dto;

import gn.lacaeducation.platform.domain.Periodicity;
import gn.lacaeducation.platform.domain.Plan;
import gn.lacaeducation.platform.domain.PlanCode;
import gn.lacaeducation.platform.domain.PlanType;

import java.math.BigDecimal;

public record PlanResponse(
    Long id,
    String name,
    PlanCode code,
    Integer studentLimit,
    BigDecimal monthlyPrice,
    BigDecimal annualPrice,
    String description,
    boolean archived,
    boolean trial,
    Integer trialDays,
    boolean defaultTrial,
    PlanType planType,
    Periodicity periodicity,
    Integer userLimit,
    Integer smsLimit,
    boolean publicVisible,
    Integer displayOrder,
    boolean highlighted
) {
    public static PlanResponse from(Plan plan) {
        return new PlanResponse(
            plan.getId(), plan.getName(), plan.getCode(), plan.getStudentLimit(),
            plan.getMonthlyPrice(), plan.getAnnualPrice(), plan.getDescription(), plan.isArchived(),
            Boolean.TRUE.equals(plan.getTrial()), plan.getTrialDays(),
            Boolean.TRUE.equals(plan.getDefaultTrial()), plan.getPlanType(), plan.getPeriodicity(),
            plan.getUserLimit(), plan.getSmsLimit(), Boolean.TRUE.equals(plan.getPublicVisible()),
            plan.getDisplayOrder(), Boolean.TRUE.equals(plan.getHighlighted())
        );
    }
}
