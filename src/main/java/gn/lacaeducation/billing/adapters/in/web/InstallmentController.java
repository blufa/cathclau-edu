package gn.lacaeducation.billing.adapters.in.web;
import gn.lacaeducation.billing.application.port.in.InstallmentUseCase;
import gn.lacaeducation.billing.domain.Installment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
@Tag(name = "Installments", description = "Define payment installment schedules (due dates and amounts) for school fees")
@RestController @RequestMapping("/api/v1/billing/installments") @RequiredArgsConstructor
public class InstallmentController {
    private final InstallmentUseCase useCase;
    public record CreateRequest(Long feeId, String label, BigDecimal amount, LocalDate dueDate, List<String> months) {}
    public record UpdateRequest(String label, BigDecimal amount, LocalDate dueDate, List<String> months) {}
    public record TermRequest(String label, BigDecimal percentage, LocalDate dueDate) {}
    public record GenerateTermsRequest(Long feeId, BigDecimal annualTotal, List<TermRequest> terms) {}
    @Operation(summary = "Create a new installment deadline for a school fee")
    @PreAuthorize("hasAuthority('echeance:manage')")
    @PostMapping public ResponseEntity<Installment> create(@RequestBody CreateRequest r) {
        return ResponseEntity.ok(useCase.create(new InstallmentUseCase.CreateInstallmentCommand(r.feeId(), r.label(), r.amount(), r.dueDate(), r.months())));
    }
    @Operation(summary = "Générer des échéances trimestrielles pondérées (ex. 40/30/30) depuis un total annuel")
    @PreAuthorize("hasAuthority('echeance:manage')")
    @PostMapping("/generate-terms") public List<Installment> generateTerms(@RequestBody GenerateTermsRequest r) {
        List<InstallmentUseCase.TermSpec> terms = r.terms().stream()
                .map(t -> new InstallmentUseCase.TermSpec(t.label(), t.percentage(), t.dueDate())).toList();
        return useCase.generateTerms(new InstallmentUseCase.GenerateTermsCommand(r.feeId(), r.annualTotal(), terms));
    }
    @Operation(summary = "Update an installment deadline")
    @PreAuthorize("hasAuthority('echeance:manage')")
    @PutMapping("/{id}") public ResponseEntity<Installment> update(@PathVariable Long id, @RequestBody UpdateRequest r) {
        return ResponseEntity.ok(useCase.update(id, new InstallmentUseCase.UpdateInstallmentCommand(r.label(), r.amount(), r.dueDate(), r.months())));
    }
    @Operation(summary = "Get an installment by ID")
    @GetMapping("/{id}") public ResponseEntity<Installment> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @Operation(summary = "List all installments for a school fee")
    @GetMapping("/fee/{feeId}") public List<Installment> findByFee(@PathVariable Long feeId) { return useCase.findByFeeId(feeId); }
    @Operation(summary = "Delete an installment deadline")
    @PreAuthorize("hasAuthority('echeance:manage')")
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { useCase.delete(id); return ResponseEntity.noContent().build(); }
}
