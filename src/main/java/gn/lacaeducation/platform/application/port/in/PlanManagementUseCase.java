package gn.lacaeducation.platform.application.port.in;
import gn.lacaeducation.platform.domain.Plan;
import gn.lacaeducation.platform.domain.PlanCode;
import java.math.BigDecimal;
import java.util.Optional;
public interface PlanManagementUseCase {
    record CreatePlanCommand(String name, PlanCode code, Integer studentLimit,
                              BigDecimal monthlyPrice, BigDecimal annualPrice, String description) {}
    record UpdatePlanCommand(String name, Integer studentLimit,
                              BigDecimal monthlyPrice, BigDecimal annualPrice, String description) {}
    Plan create(CreatePlanCommand command);
    Plan update(Long id, UpdatePlanCommand command);
    Plan archive(Long id);
    Optional<Plan> findByCode(PlanCode code);
    void delete(Long id);
    boolean existsById(Long id);
}
