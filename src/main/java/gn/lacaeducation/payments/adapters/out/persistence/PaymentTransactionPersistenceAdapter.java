package gn.lacaeducation.payments.adapters.out.persistence;
import gn.lacaeducation.payments.application.port.out.PaymentTransactionRepository;
import gn.lacaeducation.payments.domain.PaymentTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
class PaymentTransactionPersistenceAdapter implements PaymentTransactionRepository {
    private final PaymentTransactionJpaRepository jpa;
    private final PaymentsMapper mapper;
    @Override public PaymentTransaction save(PaymentTransaction t) { return mapper.toDomain(jpa.save(mapper.toEntity(t))); }
    @Override public Optional<PaymentTransaction> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<PaymentTransaction> findByPaymentId(Long id) { return jpa.findByPaymentId(id).stream().map(mapper::toDomain).toList(); }
    @Override public Optional<PaymentTransaction> findByExternalTransactionId(String ext) { return jpa.findByExternalTransactionId(ext).map(mapper::toDomain); }
}
