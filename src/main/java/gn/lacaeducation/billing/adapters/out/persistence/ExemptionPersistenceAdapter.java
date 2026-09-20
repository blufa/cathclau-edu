package gn.lacaeducation.billing.adapters.out.persistence;

import gn.lacaeducation.billing.application.port.out.ExemptionRepository;
import gn.lacaeducation.billing.domain.Exemption;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class ExemptionPersistenceAdapter implements ExemptionRepository {

    private final ExemptionJpaRepository jpa;

    @Override
    public Exemption save(Exemption e) {
        return toDomain(jpa.save(toEntity(e)));
    }

    @Override
    public Optional<Exemption> findById(Long id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<Exemption> findByTenantId(Long tenantId) {
        return jpa.findByTenantIdOrderByCreatedAtDesc(tenantId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Exemption> findByStudentId(Long studentId) {
        return jpa.findByStudentIdOrderByCreatedAtDesc(studentId).stream().map(this::toDomain).toList();
    }

    private Exemption toDomain(ExemptionJpaEntity e) {
        return Exemption.builder()
                .id(e.getId()).tenantId(e.getTenantId()).studentId(e.getStudentId()).type(e.getType())
                .percentage(e.getPercentage()).amount(e.getAmount()).feeTypes(e.getFeeTypes())
                .reason(e.getReason()).startDate(e.getStartDate()).endDate(e.getEndDate())
                .validatedBy(e.getValidatedBy()).active(e.isActive()).createdAt(e.getCreatedAt())
                .build();
    }

    private ExemptionJpaEntity toEntity(Exemption d) {
        ExemptionJpaEntity e = new ExemptionJpaEntity();
        e.setId(d.getId()); e.setTenantId(d.getTenantId()); e.setStudentId(d.getStudentId()); e.setType(d.getType());
        e.setPercentage(d.getPercentage()); e.setAmount(d.getAmount()); e.setFeeTypes(d.getFeeTypes());
        e.setReason(d.getReason()); e.setStartDate(d.getStartDate()); e.setEndDate(d.getEndDate());
        e.setValidatedBy(d.getValidatedBy()); e.setActive(d.isActive()); e.setCreatedAt(d.getCreatedAt());
        return e;
    }
}
