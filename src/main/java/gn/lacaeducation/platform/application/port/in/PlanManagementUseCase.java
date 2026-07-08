package gn.lacaeducation.platform.application.port.in;
import gn.lacaeducation.platform.domain.Periodicity;
import gn.lacaeducation.platform.domain.Plan;
import gn.lacaeducation.platform.domain.PlanCode;
import gn.lacaeducation.platform.domain.PlanType;
import java.math.BigDecimal;
import java.util.Optional;
public interface PlanManagementUseCase {
    record CreatePlanCommand(String name, PlanCode code, Integer studentLimit,
                              BigDecimal monthlyPrice, BigDecimal annualPrice, String description,
                              PlanType planType, Periodicity periodicity, Integer userLimit,
                              Integer smsLimit, Boolean publicVisible, Integer displayOrder,
                              Boolean highlighted, Boolean trial, Integer trialDays) {}
    record UpdatePlanCommand(String name, Integer studentLimit,
                              BigDecimal monthlyPrice, BigDecimal annualPrice, String description,
                              PlanType planType, Periodicity periodicity, Integer userLimit,
                              Integer smsLimit, Boolean publicVisible, Integer displayOrder,
                              Boolean highlighted) {}
    Plan create(CreatePlanCommand command);
    Plan update(Long id, UpdatePlanCommand command);
    Plan archive(Long id);
    /** Designates this plan as the default trial (price 0 + trialDays); clears the flag on others. */
    Plan setDefaultTrial(Long id);
    Optional<Plan> findByCode(PlanCode code);
    void delete(Long id);
    boolean existsById(Long id);
}
