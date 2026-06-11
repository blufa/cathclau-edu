package gn.lacaeducation.billing.adapters.out.persistence;
import gn.lacaeducation.billing.application.port.out.InvoiceRepository;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.InvoiceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
class InvoicePersistenceAdapter implements InvoiceRepository {
    private final InvoiceJpaRepository jpa;
    private final BillingMapper mapper;
    @Override public Invoice save(Invoice i) { return mapper.toDomain(jpa.save(mapper.toEntity(i))); }
    @Override public Optional<Invoice> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public Optional<Invoice> findByReference(String ref) { return jpa.findByReference(ref).map(mapper::toDomain); }
    @Override public List<Invoice> findByStudentId(Long s) { return jpa.findByStudentId(s).stream().map(mapper::toDomain).toList(); }
    @Override public List<Invoice> findByTenantId(Long t) { return jpa.findByTenantId(t).stream().map(mapper::toDomain).toList(); }
    @Override public List<Invoice> findByTenantIdAndStatus(Long t, InvoiceStatus s) { return jpa.findByTenantIdAndStatus(t, s).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
    @Override public boolean existsById(Long id) { return jpa.existsById(id); }
}
