package gn.lacaeducation.platform.application.service;
import gn.lacaeducation.common.ConflictException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.platform.application.event.PlanTrialDefinedEvent;
import gn.lacaeducation.platform.application.port.in.PlanManagementUseCase;
import gn.lacaeducation.platform.application.port.in.PlanQueryUseCase;
import gn.lacaeducation.platform.application.port.out.PlanRepository;
import gn.lacaeducation.platform.domain.Plan;
import gn.lacaeducation.platform.domain.PlanCode;
import gn.lacaeducation.platform.domain.PlanType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
@Service @Transactional @RequiredArgsConstructor
public class PlanService implements PlanQueryUseCase, PlanManagementUseCase {
    private final PlanRepository repo;
    private final ApplicationEventPublisher eventPublisher;

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
        // The plan code is unique (DB constraint plan_code_key). Reject duplicates with a clean 409
        // instead of letting the insert fail with a 500.
        if (c.code() != null && repo.findByCode(c.code()).isPresent()) {
            throw new ConflictException("Un plan avec le code " + c.code() + " existe déjà.");
        }
        // archived/trial/public flags default explicitly so null doesn't break the NOT NULL columns.
        return repo.save(Plan.builder().name(c.name()).code(c.code()).studentLimit(c.studentLimit())
                .monthlyPrice(c.monthlyPrice()).annualPrice(c.annualPrice()).description(c.description())
                .archived(false)
                .trial(Boolean.TRUE.equals(c.trial()))
                .trialDays(c.trialDays())
                .defaultTrial(false)
                .planType(c.planType() != null ? c.planType() : PlanType.STANDARD)
                .periodicity(c.periodicity())
                .userLimit(c.userLimit())
                .smsLimit(c.smsLimit())
                .publicVisible(c.publicVisible() == null || c.publicVisible())
                .displayOrder(c.displayOrder())
                .highlighted(Boolean.TRUE.equals(c.highlighted()))
                .build());
    }

    @Override
    public Plan setDefaultTrial(Long id) {
        Plan target = load(id);
        // Only one default trial: clear the flag on every other plan, then mark this one.
        for (Plan p : repo.findAll()) {
            if (Boolean.TRUE.equals(p.getDefaultTrial()) && !p.getId().equals(id)) {
                p.setDefaultTrial(false);
                repo.save(p);
            }
        }
        target.setTrial(true);
        target.setDefaultTrial(true);
        Plan saved = repo.save(target);
        eventPublisher.publishEvent(new PlanTrialDefinedEvent(saved.getId()));
        return saved;
    }
    @Override
    public Plan update(Long id, UpdatePlanCommand c) {
        Plan p = load(id);
        p.setName(c.name()); p.setStudentLimit(c.studentLimit());
        p.setMonthlyPrice(c.monthlyPrice()); p.setAnnualPrice(c.annualPrice()); p.setDescription(c.description());
        if (c.planType() != null) p.setPlanType(c.planType());
        if (c.periodicity() != null) p.setPeriodicity(c.periodicity());
        if (c.userLimit() != null) p.setUserLimit(c.userLimit());
        if (c.smsLimit() != null) p.setSmsLimit(c.smsLimit());
        if (c.publicVisible() != null) p.setPublicVisible(c.publicVisible());
        if (c.displayOrder() != null) p.setDisplayOrder(c.displayOrder());
        if (c.highlighted() != null) p.setHighlighted(c.highlighted());
        return repo.save(p);
    }
    @Override
    public Plan archive(Long id) {
        Plan p = load(id);
        p.setArchived(true);
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
