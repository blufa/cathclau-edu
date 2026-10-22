package gn.lacaeducation.billing.adapters.out.persistence;

import gn.lacaeducation.billing.application.port.out.StudentCreditRepository;
import gn.lacaeducation.billing.domain.StudentCredit;
import gn.lacaeducation.billing.domain.StudentCreditUsage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class StudentCreditPersistenceAdapter implements StudentCreditRepository {

    private final StudentCreditJpaRepository jpa;
    private final StudentCreditUsageJpaRepository usageJpa;

    @Override
    public StudentCredit save(StudentCredit credit) {
        return toDomain(jpa.save(toEntity(credit)));
    }

    @Override
    public Optional<StudentCredit> findById(Long id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<StudentCredit> lockById(Long id) {
        return jpa.lockById(id).map(this::toDomain);
    }

    @Override
    public List<StudentCredit> findByStudentId(Long studentId) {
        return jpa.findByStudentIdOrderByCreatedAtAsc(studentId).stream().map(this::toDomain).toList();
    }

    @Override
    public StudentCreditUsage saveUsage(StudentCreditUsage usage) {
        StudentCreditUsageJpaEntity e = new StudentCreditUsageJpaEntity();
        e.setId(usage.getId());
        e.setTenantId(usage.getTenantId());
        e.setStudentId(usage.getStudentId());
        e.setCreditId(usage.getCreditId());
        e.setTargetInvoiceId(usage.getTargetInvoiceId());
        e.setAmount(usage.getAmount());
        e.setAppliedByUserId(usage.getAppliedByUserId());
        e.setAppliedAt(usage.getAppliedAt());
        StudentCreditUsageJpaEntity saved = usageJpa.save(e);
        return StudentCreditUsage.builder()
                .id(saved.getId()).tenantId(saved.getTenantId()).studentId(saved.getStudentId())
                .creditId(saved.getCreditId()).targetInvoiceId(saved.getTargetInvoiceId())
                .amount(saved.getAmount()).appliedByUserId(saved.getAppliedByUserId())
                .appliedAt(saved.getAppliedAt())
                .build();
    }

    @Override
    public List<StudentCreditUsage> findUsagesByStudentId(Long studentId) {
        return usageJpa.findByStudentIdOrderByAppliedAtDesc(studentId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<StudentCreditUsage> findUsagesByCreditId(Long creditId) {
        return usageJpa.findByCreditId(creditId).stream().map(this::toDomain).toList();
    }

    private StudentCredit toDomain(StudentCreditJpaEntity e) {
        return StudentCredit.builder()
                .id(e.getId()).tenantId(e.getTenantId()).studentId(e.getStudentId())
                .amount(e.getAmount()).remainingAmount(e.getRemainingAmount())
                .sourcePaymentId(e.getSourcePaymentId()).sourceInvoiceId(e.getSourceInvoiceId())
                .reason(e.getReason()).createdAt(e.getCreatedAt())
                .build();
    }

    private StudentCreditJpaEntity toEntity(StudentCredit d) {
        StudentCreditJpaEntity e = new StudentCreditJpaEntity();
        e.setId(d.getId()); e.setTenantId(d.getTenantId()); e.setStudentId(d.getStudentId());
        e.setAmount(d.getAmount()); e.setRemainingAmount(d.getRemainingAmount());
        e.setSourcePaymentId(d.getSourcePaymentId()); e.setSourceInvoiceId(d.getSourceInvoiceId());
        e.setReason(d.getReason()); e.setCreatedAt(d.getCreatedAt());
        return e;
    }

    private StudentCreditUsage toDomain(StudentCreditUsageJpaEntity e) {
        return StudentCreditUsage.builder()
                .id(e.getId()).tenantId(e.getTenantId()).studentId(e.getStudentId())
                .creditId(e.getCreditId()).targetInvoiceId(e.getTargetInvoiceId())
                .amount(e.getAmount()).appliedByUserId(e.getAppliedByUserId())
                .appliedAt(e.getAppliedAt())
                .build();
    }
}
