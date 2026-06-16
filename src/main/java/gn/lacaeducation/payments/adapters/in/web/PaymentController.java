package gn.lacaeducation.payments.adapters.in.web;
import gn.lacaeducation.payments.application.port.in.PaymentTransactionUseCase;
import gn.lacaeducation.payments.application.port.in.PaymentUseCase;
import gn.lacaeducation.payments.application.port.in.ReceiptUseCase;
import gn.lacaeducation.payments.domain.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
@Tag(name = "Payments", description = "Initiate and manage student fee payments via mobile money and card providers")
@RestController @RequestMapping("/api/v1/payments") @RequiredArgsConstructor
public class PaymentController {
    private final PaymentUseCase useCase;
    private final PaymentTransactionUseCase transactionUseCase;
    private final ReceiptUseCase receiptUseCase;
    public record InitiateRequest(Long tenantId, Long invoiceId, Long studentId, String reference, BigDecimal amount, PaymentChannel channel, PaymentType type,
                                  String payerNumber) {}
    /** Initiation result: the payment plus the aggregator redirect URL (Djomy) when present. */
    public record InitiateResponse(Payment payment, String redirectUrl) {}
    @Operation(summary = "Initiate a new payment for a student invoice (returns redirectUrl for aggregator channels)")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT','SECRETARY')")
    @PostMapping public ResponseEntity<InitiateResponse> initiate(@RequestBody InitiateRequest r) {
        Payment payment = useCase.initiate(new PaymentUseCase.InitiatePaymentCommand(
                r.tenantId(), r.invoiceId(), r.studentId(), r.reference(), r.amount(), r.channel(), r.type(), r.payerNumber()));
        String redirectUrl = transactionUseCase.findByPaymentId(payment.getId()).stream()
                .map(PaymentTransaction::getRedirectUrl)
                .filter(u -> u != null && !u.isBlank())
                .findFirst().orElse(null);
        return ResponseEntity.ok(new InitiateResponse(payment, redirectUrl));
    }
    @Operation(summary = "Confirm a payment as successful")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
    @PutMapping("/{id}/success") public ResponseEntity<Payment> confirmSuccess(@PathVariable Long id) { return ResponseEntity.ok(useCase.confirmSuccess(id)); }
    @Operation(summary = "Mark a payment as failed")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
    @PutMapping("/{id}/fail") public ResponseEntity<Payment> markFailed(@PathVariable Long id) { return ResponseEntity.ok(useCase.markFailed(id)); }
    @Operation(summary = "Mark a payment as refunded")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
    @PutMapping("/{id}/refund") public ResponseEntity<Payment> markRefunded(@PathVariable Long id) { return ResponseEntity.ok(useCase.markRefunded(id)); }
    @Operation(summary = "Get a payment by ID")
    @GetMapping("/{id}") public ResponseEntity<Payment> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @Operation(summary = "Get a payment by its reference number")
    @GetMapping("/reference/{reference}") public ResponseEntity<Payment> findByReference(@PathVariable String reference) {
        return useCase.findByReference(reference).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @Operation(summary = "Get the provider transactions associated with a payment (§9.1)")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
    @GetMapping("/{id}/transactions") public List<PaymentTransaction> transactions(@PathVariable Long id) {
        return transactionUseCase.findByPaymentId(id);
    }
    @Operation(summary = "Get the receipt PDF URL for a successful payment (§9.1)")
    @GetMapping("/{id}/receipt/pdf") public ResponseEntity<Map<String, String>> receiptPdf(@PathVariable Long id) {
        return receiptUseCase.findByPaymentId(id)
                .map(r -> ResponseEntity.ok(Map.of("url", r.getPdfFile() == null ? "" : r.getPdfFile())))
                .orElse(ResponseEntity.notFound().build());
    }
    @Operation(summary = "List all payments for an invoice")
    @GetMapping("/invoice/{invoiceId}") public List<Payment> findByInvoice(@PathVariable Long invoiceId) { return useCase.findByInvoiceId(invoiceId); }
    @Operation(summary = "List all payments for a student")
    @GetMapping("/student/{studentId}") public List<Payment> findByStudent(@PathVariable Long studentId) { return useCase.findByStudentId(studentId); }
    @Operation(summary = "List all payments for a tenant")
    @GetMapping("/tenant/{tenantId}") public List<Payment> findByTenant(@PathVariable Long tenantId) { return useCase.findByTenantId(tenantId); }
    @Operation(summary = "Delete a payment record")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { useCase.delete(id); return ResponseEntity.noContent().build(); }
}
