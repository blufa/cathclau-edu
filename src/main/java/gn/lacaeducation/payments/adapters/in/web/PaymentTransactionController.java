package gn.lacaeducation.payments.adapters.in.web;
import gn.lacaeducation.payments.application.port.in.PaymentTransactionUseCase;
import gn.lacaeducation.payments.domain.PaymentProvider;
import gn.lacaeducation.payments.domain.PaymentTransaction;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@Tag(name = "Payment Transactions", description = "Outbound provider call traces (§9.2)")
@RestController @RequestMapping("/api/v1/payments/transactions") @RequiredArgsConstructor
public class PaymentTransactionController {
    private final PaymentTransactionUseCase useCase;

    @Operation(summary = "List provider transactions, optionally filtered by provider or payment")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
    @GetMapping public List<PaymentTransaction> list(@RequestParam(required = false) PaymentProvider provider,
                                                     @RequestParam(required = false) Long paymentId) {
        if (paymentId != null) return useCase.findByPaymentId(paymentId);
        if (provider != null) return useCase.findByProvider(provider);
        return useCase.findAll();
    }

    @Operation(summary = "Get a provider transaction by ID (includes raw response payload)")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
    @GetMapping("/{id}") public ResponseEntity<PaymentTransaction> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
