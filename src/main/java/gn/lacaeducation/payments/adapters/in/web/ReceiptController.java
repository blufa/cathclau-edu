package gn.lacaeducation.payments.adapters.in.web;
import gn.lacaeducation.payments.application.port.in.ReceiptUseCase;
import gn.lacaeducation.payments.domain.Receipt;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
@Tag(name = "Receipts", description = "Generate and retrieve payment receipts")
@RestController @RequestMapping("/api/v1/payments/receipts") @RequiredArgsConstructor
public class ReceiptController {
    private final ReceiptUseCase useCase;

    @Operation(summary = "List receipts, optionally filtered by student (§9.3)")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT','SECRETARY')")
    @GetMapping public List<Receipt> list(@RequestParam(required = false) Long studentId) {
        return studentId == null ? List.of() : useCase.findByStudentId(studentId);
    }

    @Operation(summary = "Get the receipt PDF URL (§9.3)")
    @GetMapping("/{id}/pdf") public ResponseEntity<Map<String, String>> pdf(@PathVariable Long id) {
        return useCase.findById(id)
                .map(r -> ResponseEntity.ok(Map.of("url", r.getPdfFile() == null ? "" : r.getPdfFile())))
                .orElse(ResponseEntity.notFound().build());
    }
    @Operation(summary = "Generate a receipt for a successful payment")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT','SECRETARY')")
    @PostMapping("/payment/{paymentId}") public ResponseEntity<Receipt> generate(@PathVariable Long paymentId) { return ResponseEntity.ok(useCase.generate(paymentId)); }
    @Operation(summary = "Get a receipt by ID")
    @GetMapping("/{id}") public ResponseEntity<Receipt> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @Operation(summary = "Get the receipt for a specific payment")
    @GetMapping("/payment/{paymentId}") public ResponseEntity<Receipt> findByPayment(@PathVariable Long paymentId) {
        return useCase.findByPaymentId(paymentId).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @Operation(summary = "Get a receipt by its reference number")
    @GetMapping("/reference/{reference}") public ResponseEntity<Receipt> findByReference(@PathVariable String reference) {
        return useCase.findByReference(reference).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
