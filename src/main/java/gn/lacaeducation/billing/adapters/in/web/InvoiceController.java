package gn.lacaeducation.billing.adapters.in.web;
import gn.lacaeducation.billing.application.port.in.InvoiceUseCase;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.InvoiceStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
@Tag(name = "Invoices", description = "Manage student invoices for school fees and track payment status")
@RestController @RequestMapping("/api/v1/billing/invoices") @RequiredArgsConstructor
public class InvoiceController {
    private final InvoiceUseCase useCase;
    public record CreateRequest(Long tenantId, Long studentId, Long feeId, String reference, BigDecimal totalAmount) {}
    public record PaymentRequest(BigDecimal amount) {}
    @Operation(summary = "Create a new invoice for a student")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT','SECRETARY')")
    @PostMapping public ResponseEntity<Invoice> create(@RequestBody CreateRequest r) {
        return ResponseEntity.ok(useCase.create(new InvoiceUseCase.CreateInvoiceCommand(r.tenantId(), r.studentId(), r.feeId(), r.reference(), r.totalAmount())));
    }
    @Operation(summary = "Get an invoice by ID")
    @GetMapping("/{id}") public ResponseEntity<Invoice> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @Operation(summary = "Get an invoice by its reference number")
    @GetMapping("/reference/{reference}") public ResponseEntity<Invoice> findByReference(@PathVariable String reference) {
        return useCase.findByReference(reference).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @Operation(summary = "List all invoices for a student")
    @GetMapping("/student/{studentId}") public List<Invoice> findByStudent(@PathVariable Long studentId) { return useCase.findByStudentId(studentId); }
    @Operation(summary = "List all invoices for a tenant")
    @GetMapping("/tenant/{tenantId}") public List<Invoice> findByTenant(@PathVariable Long tenantId) { return useCase.findByTenantId(tenantId); }
    @Operation(summary = "List invoices for a tenant filtered by payment status")
    @GetMapping("/tenant/{tenantId}/status/{status}") public List<Invoice> findByStatus(@PathVariable Long tenantId, @PathVariable InvoiceStatus status) { return useCase.findByTenantIdAndStatus(tenantId, status); }
    @Operation(summary = "Apply a payment amount to an invoice")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
    @PutMapping("/{id}/payment") public ResponseEntity<Invoice> applyPayment(@PathVariable Long id, @RequestBody PaymentRequest r) { return ResponseEntity.ok(useCase.applyPayment(id, r.amount())); }
    @Operation(summary = "Cancel an invoice")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
    @DeleteMapping("/{id}/cancel") public ResponseEntity<Void> cancel(@PathVariable Long id) { useCase.cancel(id); return ResponseEntity.noContent().build(); }
    @Operation(summary = "Delete an invoice")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { useCase.delete(id); return ResponseEntity.noContent().build(); }
}
