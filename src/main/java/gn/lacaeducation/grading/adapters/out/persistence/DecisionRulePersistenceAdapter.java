package gn.lacaeducation.grading.adapters.out.persistence;

import gn.lacaeducation.grading.application.port.out.DecisionRuleRepository;
import gn.lacaeducation.grading.domain.DecisionRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class DecisionRulePersistenceAdapter implements DecisionRuleRepository {

    private final DecisionRuleJpaRepository jpa;

    @Override
    public DecisionRule save(DecisionRule r) {
        return toDomain(jpa.save(toEntity(r)));
    }

    @Override
    public Optional<DecisionRule> findById(Long id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<DecisionRule> findByTenantId(Long tenantId) {
        return jpa.findByTenantId(tenantId).stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    private DecisionRule toDomain(DecisionRuleJpaEntity e) {
        return DecisionRule.builder()
                .id(e.getId()).tenantId(e.getTenantId()).name(e.getName()).conditionType(e.getConditionType())
                .threshold(e.getThreshold()).subjectId(e.getSubjectId()).minCount(e.getMinCount())
                .decision(e.getDecision()).priority(e.getPriority()).active(e.isActive())
                .validFrom(e.getValidFrom()).validTo(e.getValidTo())
                .build();
    }

    private DecisionRuleJpaEntity toEntity(DecisionRule d) {
        DecisionRuleJpaEntity e = new DecisionRuleJpaEntity();
        e.setId(d.getId()); e.setTenantId(d.getTenantId()); e.setName(d.getName()); e.setConditionType(d.getConditionType());
        e.setThreshold(d.getThreshold()); e.setSubjectId(d.getSubjectId()); e.setMinCount(d.getMinCount());
        e.setDecision(d.getDecision()); e.setPriority(d.getPriority()); e.setActive(d.isActive());
        e.setValidFrom(d.getValidFrom()); e.setValidTo(d.getValidTo());
        return e;
    }
}
