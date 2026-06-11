package gn.lacaeducation.billing.adapters.out.persistence;
import gn.lacaeducation.billing.application.port.out.InstallmentRepository;
import gn.lacaeducation.billing.domain.Installment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
class InstallmentPersistenceAdapter implements InstallmentRepository {
    private final InstallmentJpaRepository jpa;
    private final BillingMapper mapper;
    @Override public Installment save(Installment i) { return mapper.toDomain(jpa.save(mapper.toEntity(i))); }
    @Override public Optional<Installment> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<Installment> findByFeeId(Long feeId) { return jpa.findByFeeId(feeId).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
