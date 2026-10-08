package gn.lacaeducation.billing.adapters.out.persistence;

import gn.lacaeducation.billing.application.port.out.ClassTransferRefundRepository;
import gn.lacaeducation.billing.domain.ClassTransferRefund;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class ClassTransferRefundPersistenceAdapter implements ClassTransferRefundRepository {

    private final ClassTransferRefundJpaRepository jpa;

    @Override
    public ClassTransferRefund save(ClassTransferRefund r) {
        return toDomain(jpa.save(toEntity(r)));
    }

    @Override
    public Optional<ClassTransferRefund> findById(Long id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<ClassTransferRefund> findByStudentId(Long studentId) {
        return jpa.findByStudentId(studentId).stream().map(this::toDomain).toList();
    }

    private ClassTransferRefund toDomain(ClassTransferRefundJpaEntity e) {
        return ClassTransferRefund.builder()
                .id(e.getId()).tenantId(e.getTenantId()).studentId(e.getStudentId())
                .oldClassId(e.getOldClassId()).newClassId(e.getNewClassId())
                .amount(e.getAmount()).reason(e.getReason())
                .createdAt(e.getCreatedAt()).refundedAt(e.getRefundedAt())
                .build();
    }

    private ClassTransferRefundJpaEntity toEntity(ClassTransferRefund d) {
        ClassTransferRefundJpaEntity e = new ClassTransferRefundJpaEntity();
        e.setId(d.getId()); e.setTenantId(d.getTenantId()); e.setStudentId(d.getStudentId());
        e.setOldClassId(d.getOldClassId()); e.setNewClassId(d.getNewClassId());
        e.setAmount(d.getAmount()); e.setReason(d.getReason());
        e.setCreatedAt(d.getCreatedAt()); e.setRefundedAt(d.getRefundedAt());
        return e;
    }
}
