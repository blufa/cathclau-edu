package gn.lacaeducation.payments.adapters.out.persistence;
import gn.lacaeducation.payments.application.port.out.PaymentRepository;
import gn.lacaeducation.payments.domain.Payment;
import gn.lacaeducation.payments.domain.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
class PaymentPersistenceAdapter implements PaymentRepository {
    private final PaymentJpaRepository jpa;
    private final PaymentsMapper mapper;
    @Override public Payment save(Payment p) { return mapper.toDomain(jpa.save(mapper.toEntity(p))); }
    @Override public Optional<Payment> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public Optional<Payment> findByReference(String ref) { return jpa.findByReference(ref).map(mapper::toDomain); }
    @Override public List<Payment> findByInvoiceId(Long id) { return jpa.findByInvoiceId(id).stream().map(mapper::toDomain).toList(); }
    @Override public List<Payment> findByStudentId(Long id) { return jpa.findByStudentId(id).stream().map(mapper::toDomain).toList(); }
    @Override public List<Payment> findByTenantId(Long id) { return jpa.findByTenantId(id).stream().map(mapper::toDomain).toList(); }
    @Override public List<Payment> findByTenantIdAndStatus(Long t, PaymentStatus s) { return jpa.findByTenantIdAndStatus(t, s).stream().map(mapper::toDomain).toList(); }
    @Override public List<Payment> findByTenantIdAndAcademicYearId(Long t, Long y) { return jpa.findByTenantIdAndAcademicYearId(t, y).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
