package gn.lacaeducation.billing.adapters.out.persistence;
import gn.lacaeducation.billing.domain.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
interface InvoiceJpaRepository extends JpaRepository<InvoiceJpaEntity, Long> {
    Optional<InvoiceJpaEntity> findByReference(String reference);
    List<InvoiceJpaEntity> findByStudentId(Long studentId);
    List<InvoiceJpaEntity> findByTenantId(Long tenantId);
    List<InvoiceJpaEntity> findByTenantIdAndStatus(Long tenantId, InvoiceStatus status);
    List<InvoiceJpaEntity> findByTenantIdAndAcademicYearId(Long tenantId, Long academicYearId);
    boolean existsByStudentIdAndFeeId(Long studentId, Long feeId);
    boolean existsByStudentIdAndInstallmentId(Long studentId, Long installmentId);
    List<InvoiceJpaEntity> findByFeeId(Long feeId);
}
