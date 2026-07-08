package gn.lacaeducation.grading.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.grading.application.port.in.EliminatoryRuleUseCase;
import gn.lacaeducation.grading.application.port.out.EliminatoryRuleRepository;
import gn.lacaeducation.grading.domain.EliminatoryRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class EliminatoryRuleService implements EliminatoryRuleUseCase {

    private final EliminatoryRuleRepository repo;

    @Override
    public EliminatoryRule create(Long tenantId, RuleCommand c) {
        validate(c);
        return repo.save(EliminatoryRule.builder()
                .tenantId(tenantId).subjectId(c.subjectId()).threshold(c.threshold())
                .decisionLabel(c.decisionLabel() != null && !c.decisionLabel().isBlank() ? c.decisionLabel() : "REDOUBLE")
                .active(c.active() == null || c.active())
                .build());
    }

    @Override
    public EliminatoryRule update(Long id, RuleCommand c) {
        validate(c);
        EliminatoryRule r = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EliminatoryRule", id));
        r.setSubjectId(c.subjectId());
        r.setThreshold(c.threshold());
        if (c.decisionLabel() != null && !c.decisionLabel().isBlank()) r.setDecisionLabel(c.decisionLabel());
        if (c.active() != null) r.setActive(c.active());
        return repo.save(r);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EliminatoryRule> findByTenant(Long tenantId) {
        return repo.findByTenantId(tenantId);
    }

    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }

    private void validate(RuleCommand c) {
        if (c.subjectId() == null) throw new BusinessRuleException("La matière est requise.");
        if (c.threshold() == null) throw new BusinessRuleException("Le seuil éliminatoire est requis.");
    }
}
