package gn.lacaeducation.billing.adapters.in.web;
import gn.lacaeducation.billing.application.port.in.InvoiceUseCase;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.InvoiceStatus;
import gn.lacaeducation.common.PortalAccess;
import gn.lacaeducation.students.ParentAccessProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
@Tag(name = "Invoices", description = "Manage student invoices for school fees and track payment status")
@RestController @RequestMapping("/api/v1/billing/invoices") @RequiredArgsConstructor
public class InvoiceController {
    private final InvoiceUseCase useCase;
    private final ParentAccessProvider parentAccessProvider;

    private void assertParentScope(Jwt jwt, Long studentId) {
        if (jwt == null) return;
        Long userId;
        try { userId = Long.parseLong(jwt.getSubject()); } catch (RuntimeException e) { userId = null; }
        PortalAccess.assertParentCanAccessStudent(jwt.getClaimAsStringList("roles"), userId, studentId,
                parentAccessProvider::isParentOfStudent);
    }
    public record CreateRequest(Long tenantId, Long studentId, Long feeId, String reference, BigDecimal totalAmount) {}
    public record PaymentRequest(BigDecimal amount) {}
    public record DiscountRequest(BigDecimal amount, String reason) {}
    public record BulkGenerateRequest(Long tenantId, Long classId, Long feeId) {}
    public record GenerateMonthlyRequest(Long tenantId, Long studentId, Long feeId) {}
    @Operation(summary = "Create a new invoice for a student")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT','SECRETARY')")
    @PostMapping public ResponseEntity<Invoice> create(@RequestBody CreateRequest r) {
        return ResponseEntity.ok(useCase.create(new InvoiceUseCase.CreateInvoiceCommand(r.tenantId(), r.studentId(), r.feeId(), r.reference(), r.totalAmount())));
    }
    @Operation(summary = "Generate invoices in bulk for all students enrolled in a class")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
    @PostMapping("/generate-bulk") public ResponseEntity<List<Invoice>> generateBulk(@RequestBody BulkGenerateRequest r) {
        return ResponseEntity.ok(useCase.generateBulk(new InvoiceUseCase.BulkGenerateCommand(r.tenantId(), r.classId(), r.feeId())));
    }
    @Operation(summary = "Generate one invoice per month (installment) of a MONTHLY fee for a student")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT','SECRETARY')")
    @PostMapping("/generate-monthly") public ResponseEntity<List<Invoice>> generateMonthly(@RequestBody GenerateMonthlyRequest r) {
        return ResponseEntity.ok(useCase.generateMonthly(new InvoiceUseCase.GenerateMonthlyCommand(r.tenantId(), r.studentId(), r.feeId())));
    }
    @Operation(summary = "Get an invoice by ID")
    @GetMapping("/{id}") public ResponseEntity<Invoice> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @Operation(summary = "Get the invoice PDF URL — generated on demand if missing")
    @GetMapping("/{id}/pdf") public ResponseEntity<java.util.Map<String, String>> pdf(@PathVariable Long id) {
        if (useCase.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(java.util.Map.of("url", useCase.pdfUrl(id)));
    }

    @Operation(summary = "Stream the invoice PDF bytes (inline) — independent of Cloudinary delivery")
    @GetMapping("/{id}/pdf/download") public ResponseEntity<byte[]> pdfDownload(@PathVariable Long id) {
        if (useCase.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"facture-" + id + ".pdf\"")
                .body(useCase.pdfBytes(id));
    }
    @Operation(summary = "Get an invoice by its reference number")
    @GetMapping("/reference/{reference}") public ResponseEntity<Invoice> findByReference(@PathVariable String reference) {
        return useCase.findByReference(reference).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @Operation(summary = "List all invoices for a student")
    @GetMapping("/student/{studentId}") public List<Invoice> findByStudent(@PathVariable Long studentId,
            @AuthenticationPrincipal Jwt jwt) { assertParentScope(jwt, studentId); return useCase.findByStudentId(studentId); }
    @Operation(summary = "List all invoices for a tenant")
    @GetMapping("/tenant/{tenantId}") public List<Invoice> findByTenant(@PathVariable Long tenantId) { return useCase.findByTenantId(tenantId); }
    @Operation(summary = "List invoices for a tenant filtered by payment status")
    @GetMapping("/tenant/{tenantId}/status/{status}") public List<Invoice> findByStatus(@PathVariable Long tenantId, @PathVariable InvoiceStatus status) { return useCase.findByTenantIdAndStatus(tenantId, status); }
    @Operation(summary = "Apply a payment amount to an invoice")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
    @PutMapping("/{id}/payment") public ResponseEntity<Invoice> applyPayment(@PathVariable Long id, @RequestBody PaymentRequest r) { return ResponseEntity.ok(useCase.applyPayment(id, r.amount())); }
    @Operation(summary = "Appliquer une remise / bourse à une facture (recalcule le reste à payer)")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
    @PatchMapping("/{id}/discount") public ResponseEntity<Invoice> applyDiscount(@PathVariable Long id, @RequestBody DiscountRequest r) { return ResponseEntity.ok(useCase.applyDiscount(id, r.amount(), r.reason())); }
    @Operation(summary = "Avancer le recouvrement d'une étape (relance 1 → relance 2 → exclusion)")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
    @PatchMapping("/{id}/recovery/advance") public ResponseEntity<Invoice> advanceRecovery(@PathVariable Long id) { return ResponseEntity.ok(useCase.advanceRecovery(id)); }
    @Operation(summary = "Positionner l'étape de recouvrement")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
    @PatchMapping("/{id}/recovery") public ResponseEntity<Invoice> setRecovery(@PathVariable Long id, @RequestParam gn.lacaeducation.billing.domain.RecoveryStage stage) { return ResponseEntity.ok(useCase.setRecoveryStage(id, stage)); }
    @Operation(summary = "Cancel an invoice")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
    @DeleteMapping("/{id}/cancel") public ResponseEntity<Void> cancel(@PathVariable Long id) { useCase.cancel(id); return ResponseEntity.noContent().build(); }
    @Operation(summary = "Delete an invoice")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { useCase.delete(id); return ResponseEntity.noContent().build(); }
}
