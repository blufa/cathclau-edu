package gn.lacaeducation.payments.adapters.out.persistence;
import gn.lacaeducation.payments.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, Long> {
    Optional<PaymentJpaEntity> findByReference(String reference);
    Optional<PaymentJpaEntity> findByIdempotencyKey(String idempotencyKey);
    List<PaymentJpaEntity> findByInvoiceId(Long invoiceId);
    List<PaymentJpaEntity> findByStudentId(Long studentId);
    /** Paiements groupés (fratrie) du tenant — bornés aux lignes ayant des élèves additionnels, filtrés
     * en mémoire par appartenance (voir {@link PaymentPersistenceAdapter#findByStudentId}). */
    List<PaymentJpaEntity> findByStudentIdsIsNotNull();
    List<PaymentJpaEntity> findByTenantId(Long tenantId);
    List<PaymentJpaEntity> findByTenantIdAndStatus(Long tenantId, PaymentStatus status);
    List<PaymentJpaEntity> findByTenantIdAndAcademicYearId(Long tenantId, Long academicYearId);
    List<PaymentJpaEntity> findByCollectedByUserIdAndPaidAtBetween(Long collectedByUserId, LocalDateTime from, LocalDateTime to);
}
