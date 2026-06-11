package gn.lacaeducation.payments.adapters.out.persistence;
import gn.lacaeducation.payments.application.port.out.ReceiptRepository;
import gn.lacaeducation.payments.domain.Receipt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Optional;
@Component @RequiredArgsConstructor
class ReceiptPersistenceAdapter implements ReceiptRepository {
    private final ReceiptJpaRepository jpa;
    private final PaymentsMapper mapper;
    @Override public Receipt save(Receipt r) { return mapper.toDomain(jpa.save(mapper.toEntity(r))); }
    @Override public Optional<Receipt> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public Optional<Receipt> findByPaymentId(Long id) { return jpa.findByPaymentId(id).map(mapper::toDomain); }
    @Override public Optional<Receipt> findByReference(String ref) { return jpa.findByReference(ref).map(mapper::toDomain); }
}
