package gn.lacaeducation.billing.adapters.out.persistence;

import gn.lacaeducation.billing.application.port.out.StaffChildBenefitPolicyRepository;
import gn.lacaeducation.billing.domain.StaffChildBenefitPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class StaffChildBenefitPolicyPersistenceAdapter implements StaffChildBenefitPolicyRepository {

    private final StaffChildBenefitPolicyJpaRepository jpa;

    @Override
    public StaffChildBenefitPolicy save(StaffChildBenefitPolicy p) {
        return toDomain(jpa.save(toEntity(p)));
    }

    @Override
    public Optional<StaffChildBenefitPolicy> findById(Long id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<StaffChildBenefitPolicy> findByTenantId(Long tenantId) {
        return jpa.findByTenantId(tenantId).stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    private StaffChildBenefitPolicy toDomain(StaffChildBenefitPolicyJpaEntity e) {
        return StaffChildBenefitPolicy.builder()
                .id(e.getId()).tenantId(e.getTenantId()).label(e.getLabel())
                .coveredChildrenCount(e.getCoveredChildrenCount()).coveragePercent(e.getCoveragePercent())
                .feeTypes(e.getFeeTypes()).active(e.isActive())
                .build();
    }

    private StaffChildBenefitPolicyJpaEntity toEntity(StaffChildBenefitPolicy d) {
        StaffChildBenefitPolicyJpaEntity e = new StaffChildBenefitPolicyJpaEntity();
        e.setId(d.getId()); e.setTenantId(d.getTenantId()); e.setLabel(d.getLabel());
        e.setCoveredChildrenCount(d.getCoveredChildrenCount()); e.setCoveragePercent(d.getCoveragePercent());
        e.setFeeTypes(d.getFeeTypes()); e.setActive(d.isActive());
        return e;
    }
}
