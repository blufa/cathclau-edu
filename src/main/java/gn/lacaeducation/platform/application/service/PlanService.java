package gn.lacaeducation.platform.application.service;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.platform.application.port.in.PlanManagementUseCase;
import gn.lacaeducation.platform.application.port.in.PlanQueryUseCase;
import gn.lacaeducation.platform.application.port.out.PlanRepository;
import gn.lacaeducation.platform.domain.Plan;
import gn.lacaeducation.platform.domain.PlanCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
@Service @Transactional @RequiredArgsConstructor
public class PlanService implements PlanQueryUseCase, PlanManagementUseCase {
    private final PlanRepository repo;

    // ── PlanQueryUseCase ──────────────────────────────────────────────
    @Override @Transactional(readOnly=true)
    public Plan findById(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + id));
    }
    @Override @Transactional(readOnly=true)
    public List<Plan> findAll() { return repo.findAll(); }

    // ── PlanManagementUseCase ─────────────────────────────────────────
    @Override
    public Plan create(CreatePlanCommand c) {
        return repo.save(Plan.builder().name(c.name()).code(c.code()).studentLimit(c.studentLimit())
                .monthlyPrice(c.monthlyPrice()).annualPrice(c.annualPrice()).description(c.description()).build());
    }
    @Override
    public Plan update(Long id, UpdatePlanCommand c) {
        Plan p = load(id);
        p.setName(c.name()); p.setStudentLimit(c.studentLimit());
        p.setMonthlyPrice(c.monthlyPrice()); p.setAnnualPrice(c.annualPrice()); p.setDescription(c.description());
        return repo.save(p);
    }
    @Override @Transactional(readOnly=true)
    public Optional<Plan> findByCode(PlanCode code) { return repo.findByCode(code); }
    @Override
    public void delete(Long id) { repo.deleteById(id); }
    @Override @Transactional(readOnly=true)
    public boolean existsById(Long id) { return repo.existsById(id); }

    private Plan load(Long id) { return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + id)); }
}
