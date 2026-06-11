package gn.lacaeducation.billing.application.port.in;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.InvoiceStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
public interface InvoiceUseCase {
    record CreateInvoiceCommand(Long tenantId, Long studentId, Long feeId, String reference, BigDecimal totalAmount) {}
    record BulkGenerateCommand(Long tenantId, Long classId, Long feeId) {}
    Invoice create(CreateInvoiceCommand command);
    List<Invoice> generateBulk(BulkGenerateCommand command);
    Optional<Invoice> findById(Long id);
    Optional<Invoice> findByReference(String reference);
    List<Invoice> findByStudentId(Long studentId);
    List<Invoice> findByTenantId(Long tenantId);
    List<Invoice> findByTenantIdAndStatus(Long tenantId, InvoiceStatus status);
    List<Invoice> findByFeeId(Long feeId);
    boolean existsByStudentIdAndFeeId(Long studentId, Long feeId);
    Invoice applyPayment(Long invoiceId, BigDecimal amount);
    void cancel(Long id);
    void delete(Long id);
    boolean existsById(Long id);
}
