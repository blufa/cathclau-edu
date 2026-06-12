package gn.lacaeducation.platform.adapters.in.web.dto;

import gn.lacaeducation.platform.domain.Plan;
import gn.lacaeducation.platform.domain.PlanCode;

import java.math.BigDecimal;

public record PlanResponse(
    Long id,
    String name,
    PlanCode code,
    Integer studentLimit,
    BigDecimal monthlyPrice,
    BigDecimal annualPrice,
    String description,
    boolean archived
) {
    public static PlanResponse from(Plan plan) {
        return new PlanResponse(
            plan.getId(), plan.getName(), plan.getCode(), plan.getStudentLimit(),
            plan.getMonthlyPrice(), plan.getAnnualPrice(), plan.getDescription(), plan.isArchived()
        );
    }
}
