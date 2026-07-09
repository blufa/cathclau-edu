package gn.lacaeducation.grading.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.grading.application.port.in.DecisionRuleUseCase;
import gn.lacaeducation.grading.application.port.out.DecisionRuleRepository;
import gn.lacaeducation.grading.domain.DecisionConditionType;
import gn.lacaeducation.grading.domain.DecisionRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class DecisionRuleService implements DecisionRuleUseCase {

    private final DecisionRuleRepository repo;

    @Override
    public DecisionRule create(Long tenantId, RuleCommand c) {
        validate(c);
        return repo.save(DecisionRule.builder()
                .tenantId(tenantId).name(c.name()).conditionType(c.conditionType())
                .threshold(c.threshold()).subjectId(c.subjectId()).minCount(c.minCount())
                .decision(c.decision()).priority(c.priority() != null ? c.priority() : 100)
                .active(c.active() == null || c.active()).validFrom(c.validFrom()).validTo(c.validTo())
                .build());
    }

    @Override
    public DecisionRule update(Long id, RuleCommand c) {
        validate(c);
        DecisionRule r = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("DecisionRule", id));
        r.setName(c.name()); r.setConditionType(c.conditionType()); r.setThreshold(c.threshold());
        r.setSubjectId(c.subjectId()); r.setMinCount(c.minCount()); r.setDecision(c.decision());
        if (c.priority() != null) r.setPriority(c.priority());
        if (c.active() != null) r.setActive(c.active());
        r.setValidFrom(c.validFrom()); r.setValidTo(c.validTo());
        return repo.save(r);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DecisionRule> findByTenant(Long tenantId) {
        return repo.findByTenantId(tenantId);
    }

    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }

    private void validate(RuleCommand c) {
        if (c.name() == null || c.name().isBlank()) throw new BusinessRuleException("Le nom de la règle est requis.");
        if (c.conditionType() == null) throw new BusinessRuleException("Le type de condition est requis.");
        if (c.decision() == null || c.decision().isBlank()) throw new BusinessRuleException("La décision est requise.");
        if (c.conditionType() == DecisionConditionType.SUBJECT_BELOW && c.subjectId() == null) {
            throw new BusinessRuleException("Une condition par matière doit préciser la matière.");
        }
        if (c.conditionType() == DecisionConditionType.SUBJECTS_BELOW_COUNT && c.minCount() == null) {
            throw new BusinessRuleException("Une condition « N matières sous le seuil » doit préciser le nombre.");
        }
        if (c.validFrom() != null && c.validTo() != null && c.validTo().isBefore(c.validFrom())) {
            throw new BusinessRuleException("La date de fin de validité doit être postérieure à la date de début.");
        }
    }
}
