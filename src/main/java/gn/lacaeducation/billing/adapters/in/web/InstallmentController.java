package gn.lacaeducation.billing.adapters.in.web;
import gn.lacaeducation.billing.application.port.in.InstallmentUseCase;
import gn.lacaeducation.billing.domain.Installment;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
@RestController @RequestMapping("/api/billing/installments") @RequiredArgsConstructor
public class InstallmentController {
    private final InstallmentUseCase useCase;
    public record CreateRequest(Long feeId, String label, BigDecimal amount, LocalDate dueDate) {}
    public record UpdateRequest(String label, BigDecimal amount, LocalDate dueDate) {}
    @PostMapping public ResponseEntity<Installment> create(@RequestBody CreateRequest r) {
        return ResponseEntity.ok(useCase.create(new InstallmentUseCase.CreateInstallmentCommand(r.feeId(), r.label(), r.amount(), r.dueDate())));
    }
    @PutMapping("/{id}") public ResponseEntity<Installment> update(@PathVariable Long id, @RequestBody UpdateRequest r) {
        return ResponseEntity.ok(useCase.update(id, new InstallmentUseCase.UpdateInstallmentCommand(r.label(), r.amount(), r.dueDate())));
    }
    @GetMapping("/{id}") public ResponseEntity<Installment> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/fee/{feeId}") public List<Installment> findByFee(@PathVariable Long feeId) { return useCase.findByFeeId(feeId); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { useCase.delete(id); return ResponseEntity.noContent().build(); }
}
