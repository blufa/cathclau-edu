package gn.lacaeducation.grading.adapters.out.persistence;

import gn.lacaeducation.grading.application.port.out.EliminatoryRuleRepository;
import gn.lacaeducation.grading.domain.EliminatoryRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class EliminatoryRulePersistenceAdapter implements EliminatoryRuleRepository {

    private final EliminatoryRuleJpaRepository jpa;

    @Override
    public EliminatoryRule save(EliminatoryRule r) {
        return toDomain(jpa.save(toEntity(r)));
    }

    @Override
    public Optional<EliminatoryRule> findById(Long id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<EliminatoryRule> findByTenantId(Long tenantId) {
        return jpa.findByTenantId(tenantId).stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    private EliminatoryRule toDomain(EliminatoryRuleJpaEntity e) {
        return EliminatoryRule.builder()
                .id(e.getId()).tenantId(e.getTenantId()).subjectId(e.getSubjectId())
                .threshold(e.getThreshold()).decisionLabel(e.getDecisionLabel()).active(e.isActive())
                .build();
    }

    private EliminatoryRuleJpaEntity toEntity(EliminatoryRule d) {
        EliminatoryRuleJpaEntity e = new EliminatoryRuleJpaEntity();
        e.setId(d.getId()); e.setTenantId(d.getTenantId()); e.setSubjectId(d.getSubjectId());
        e.setThreshold(d.getThreshold()); e.setDecisionLabel(d.getDecisionLabel()); e.setActive(d.isActive());
        return e;
    }
}
