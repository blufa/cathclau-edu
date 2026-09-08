package gn.lacaeducation.billing.application.port.out;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.InvoiceStatus;
import java.util.List;
import java.util.Optional;
public interface InvoiceRepository {
    Invoice save(Invoice invoice);
    Optional<Invoice> findById(Long id);
    Optional<Invoice> findByReference(String reference);
    List<Invoice> findByStudentId(Long studentId);
    List<Invoice> findByTenantId(Long tenantId);
    List<Invoice> findByTenantIdAndStatus(Long tenantId, InvoiceStatus status);
    List<Invoice> findByTenantIdAndAcademicYearId(Long tenantId, Long academicYearId);
    List<Invoice> findByFeeId(Long feeId);
    List<Invoice> findByInstallmentId(Long installmentId);
    boolean existsByStudentIdAndFeeId(Long studentId, Long feeId);
    boolean existsByStudentIdAndInstallmentId(Long studentId, Long installmentId);
    void deleteById(Long id);
    boolean existsById(Long id);
}
