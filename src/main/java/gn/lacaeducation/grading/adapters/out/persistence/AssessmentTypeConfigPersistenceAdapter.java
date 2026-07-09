package gn.lacaeducation.grading.adapters.out.persistence;

import gn.lacaeducation.grading.application.port.out.AssessmentTypeConfigRepository;
import gn.lacaeducation.grading.domain.AssessmentTypeConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class AssessmentTypeConfigPersistenceAdapter implements AssessmentTypeConfigRepository {

    private final AssessmentTypeConfigJpaRepository jpa;

    @Override
    public AssessmentTypeConfig save(AssessmentTypeConfig c) {
        return toDomain(jpa.save(toEntity(c)));
    }

    @Override
    public Optional<AssessmentTypeConfig> findById(Long id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<AssessmentTypeConfig> findByTenantId(Long tenantId) {
        return jpa.findByTenantId(tenantId).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsByTenantIdAndCode(Long tenantId, String code) {
        return jpa.existsByTenantIdAndCode(tenantId, code);
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    private AssessmentTypeConfig toDomain(AssessmentTypeConfigJpaEntity e) {
        return AssessmentTypeConfig.builder()
                .id(e.getId()).tenantId(e.getTenantId()).code(e.getCode()).label(e.getLabel())
                .defaultCoefficient(e.getDefaultCoefficient()).weight(e.getWeight())
                .color(e.getColor()).active(e.isActive())
                .build();
    }

    private AssessmentTypeConfigJpaEntity toEntity(AssessmentTypeConfig d) {
        AssessmentTypeConfigJpaEntity e = new AssessmentTypeConfigJpaEntity();
        e.setId(d.getId()); e.setTenantId(d.getTenantId()); e.setCode(d.getCode()); e.setLabel(d.getLabel());
        e.setDefaultCoefficient(d.getDefaultCoefficient()); e.setWeight(d.getWeight());
        e.setColor(d.getColor()); e.setActive(d.isActive());
        return e;
    }
}
