package gn.lacaeducation.billing.adapters.in.web;
import gn.lacaeducation.billing.application.port.in.InvoiceUseCase;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.InvoiceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
@RestController @RequestMapping("/api/billing/invoices") @RequiredArgsConstructor
public class InvoiceController {
    private final InvoiceUseCase useCase;
    public record CreateRequest(Long tenantId, Long studentId, Long feeId, String reference, BigDecimal totalAmount) {}
    public record PaymentRequest(BigDecimal amount) {}
    @PostMapping public ResponseEntity<Invoice> create(@RequestBody CreateRequest r) {
        return ResponseEntity.ok(useCase.create(new InvoiceUseCase.CreateInvoiceCommand(r.tenantId(), r.studentId(), r.feeId(), r.reference(), r.totalAmount())));
    }
    @GetMapping("/{id}") public ResponseEntity<Invoice> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/reference/{reference}") public ResponseEntity<Invoice> findByReference(@PathVariable String reference) {
        return useCase.findByReference(reference).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/student/{studentId}") public List<Invoice> findByStudent(@PathVariable Long studentId) { return useCase.findByStudentId(studentId); }
    @GetMapping("/tenant/{tenantId}") public List<Invoice> findByTenant(@PathVariable Long tenantId) { return useCase.findByTenantId(tenantId); }
    @GetMapping("/tenant/{tenantId}/status/{status}") public List<Invoice> findByStatus(@PathVariable Long tenantId, @PathVariable InvoiceStatus status) { return useCase.findByTenantIdAndStatus(tenantId, status); }
    @PutMapping("/{id}/payment") public ResponseEntity<Invoice> applyPayment(@PathVariable Long id, @RequestBody PaymentRequest r) { return ResponseEntity.ok(useCase.applyPayment(id, r.amount())); }
    @DeleteMapping("/{id}/cancel") public ResponseEntity<Void> cancel(@PathVariable Long id) { useCase.cancel(id); return ResponseEntity.noContent().build(); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { useCase.delete(id); return ResponseEntity.noContent().build(); }
}
