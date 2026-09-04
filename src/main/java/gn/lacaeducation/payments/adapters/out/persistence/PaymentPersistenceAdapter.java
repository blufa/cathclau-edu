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
    @Override public Optional<Payment> findByIdempotencyKey(String key) { return jpa.findByIdempotencyKey(key).map(mapper::toDomain); }
    @Override public List<Payment> findByInvoiceId(Long id) { return jpa.findByInvoiceId(id).stream().map(mapper::toDomain).toList(); }
    /**
     * Élève principal (colonne indexée) OU élève additionnel d'un paiement groupé multi-enfants (§payments —
     * fratrie) : le filtrage d'appartenance se fait en mémoire sur le sous-ensemble déjà restreint aux
     * paiements groupés ({@code studentIds} non nul), jamais sur toute la table — reste tenant-filtré
     * puisque les deux requêtes sous-jacentes sont du JPQL dérivé (le filtre Hibernate s'applique).
     */
    @Override public List<Payment> findByStudentId(Long id) {
        List<Payment> primary = jpa.findByStudentId(id).stream().map(mapper::toDomain).toList();
        List<Payment> grouped = jpa.findByStudentIdsIsNotNull().stream().map(mapper::toDomain)
                .filter(p -> p.getStudentIds() != null && p.getStudentIds().contains(id))
                .toList();
        java.util.LinkedHashMap<Long, Payment> byId = new java.util.LinkedHashMap<>();
        for (Payment p : primary) byId.put(p.getId(), p);
        for (Payment p : grouped) byId.putIfAbsent(p.getId(), p);
        return List.copyOf(byId.values());
    }
    @Override public List<Payment> findByTenantId(Long id) { return jpa.findByTenantId(id).stream().map(mapper::toDomain).toList(); }
    @Override public List<Payment> findByTenantIdAndStatus(Long t, PaymentStatus s) { return jpa.findByTenantIdAndStatus(t, s).stream().map(mapper::toDomain).toList(); }
    @Override public List<Payment> findByTenantIdAndAcademicYearId(Long t, Long y) { return jpa.findByTenantIdAndAcademicYearId(t, y).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
