package gn.lacaeducation.payments.adapters.in.web;
import gn.lacaeducation.payments.application.port.in.PaymentUseCase;
import gn.lacaeducation.payments.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
@RestController @RequestMapping("/api/payments") @RequiredArgsConstructor
public class PaymentController {
    private final PaymentUseCase useCase;
    public record InitiateRequest(Long tenantId, Long invoiceId, Long studentId, String reference, BigDecimal amount, PaymentChannel channel, PaymentType type) {}
    @PostMapping public ResponseEntity<Payment> initiate(@RequestBody InitiateRequest r) {
        return ResponseEntity.ok(useCase.initiate(new PaymentUseCase.InitiatePaymentCommand(r.tenantId(), r.invoiceId(), r.studentId(), r.reference(), r.amount(), r.channel(), r.type())));
    }
    @PutMapping("/{id}/success") public ResponseEntity<Payment> confirmSuccess(@PathVariable Long id) { return ResponseEntity.ok(useCase.confirmSuccess(id)); }
    @PutMapping("/{id}/fail") public ResponseEntity<Payment> markFailed(@PathVariable Long id) { return ResponseEntity.ok(useCase.markFailed(id)); }
    @PutMapping("/{id}/refund") public ResponseEntity<Payment> markRefunded(@PathVariable Long id) { return ResponseEntity.ok(useCase.markRefunded(id)); }
    @GetMapping("/{id}") public ResponseEntity<Payment> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/reference/{reference}") public ResponseEntity<Payment> findByReference(@PathVariable String reference) {
        return useCase.findByReference(reference).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/invoice/{invoiceId}") public List<Payment> findByInvoice(@PathVariable Long invoiceId) { return useCase.findByInvoiceId(invoiceId); }
    @GetMapping("/student/{studentId}") public List<Payment> findByStudent(@PathVariable Long studentId) { return useCase.findByStudentId(studentId); }
    @GetMapping("/tenant/{tenantId}") public List<Payment> findByTenant(@PathVariable Long tenantId) { return useCase.findByTenantId(tenantId); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { useCase.delete(id); return ResponseEntity.noContent().build(); }
}
