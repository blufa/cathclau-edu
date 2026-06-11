package gn.lacaeducation.platform.application.port.out;
import gn.lacaeducation.platform.domain.Plan;
import gn.lacaeducation.platform.domain.PlanCode;
import java.util.List;
import java.util.Optional;
public interface PlanRepository {
    Plan save(Plan plan);
    Optional<Plan> findById(Long id);
    Optional<Plan> findByCode(PlanCode code);
    List<Plan> findAll();
    void deleteById(Long id);
    boolean existsById(Long id);
}
