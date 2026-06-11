package gn.lacaeducation.payments.adapters.in.web;
import gn.lacaeducation.payments.application.port.in.ReceiptUseCase;
import gn.lacaeducation.payments.domain.Receipt;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/payments/receipts") @RequiredArgsConstructor
public class ReceiptController {
    private final ReceiptUseCase useCase;
    @PostMapping("/payment/{paymentId}") public ResponseEntity<Receipt> generate(@PathVariable Long paymentId) { return ResponseEntity.ok(useCase.generate(paymentId)); }
    @GetMapping("/{id}") public ResponseEntity<Receipt> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/payment/{paymentId}") public ResponseEntity<Receipt> findByPayment(@PathVariable Long paymentId) {
        return useCase.findByPaymentId(paymentId).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/reference/{reference}") public ResponseEntity<Receipt> findByReference(@PathVariable String reference) {
        return useCase.findByReference(reference).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
