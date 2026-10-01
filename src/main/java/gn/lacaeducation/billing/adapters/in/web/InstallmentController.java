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
@Tag(name = "Installments", description = "Définir les échéanciers de paiement (dates limites et montants) des frais scolaires")
@RestController @RequestMapping("/api/v1/billing/installments") @RequiredArgsConstructor
public class InstallmentController {
    private final InstallmentUseCase useCase;
    public record CreateRequest(Long feeId, String label, BigDecimal amount, LocalDate dueDate, List<String> months) {}
    public record UpdateRequest(String label, BigDecimal amount, LocalDate dueDate, List<String> months) {}
    public record TermRequest(String label, BigDecimal percentage, LocalDate dueDate) {}
    public record GenerateTermsRequest(Long feeId, BigDecimal annualTotal, List<TermRequest> terms) {}
    @Operation(summary = "Créer une nouvelle échéance pour un frais scolaire")
    @PreAuthorize("hasAuthority('frais_scolaire:create')")
    @PostMapping public ResponseEntity<Installment> create(@RequestBody CreateRequest r) {
        return ResponseEntity.ok(useCase.create(new InstallmentUseCase.CreateInstallmentCommand(r.feeId(), r.label(), r.amount(), r.dueDate(), r.months())));
    }
    @Operation(summary = "Générer des échéances trimestrielles pondérées (ex. 40/30/30) depuis un total annuel")
    @PreAuthorize("hasAuthority('frais_scolaire:create')")
    @PostMapping("/generate-terms") public List<Installment> generateTerms(@RequestBody GenerateTermsRequest r) {
        List<InstallmentUseCase.TermSpec> terms = r.terms().stream()
                .map(t -> new InstallmentUseCase.TermSpec(t.label(), t.percentage(), t.dueDate())).toList();
        return useCase.generateTerms(new InstallmentUseCase.GenerateTermsCommand(r.feeId(), r.annualTotal(), terms));
    }
    @Operation(summary = "Modifier une échéance")
    @PreAuthorize("hasAuthority('frais_scolaire:update')")
    @PutMapping("/{id}") public ResponseEntity<Installment> update(@PathVariable Long id, @RequestBody UpdateRequest r) {
        return ResponseEntity.ok(useCase.update(id, new InstallmentUseCase.UpdateInstallmentCommand(r.label(), r.amount(), r.dueDate(), r.months())));
    }
    @Operation(summary = "Consulter une échéance par son ID")
    @PreAuthorize("hasAuthority('frais_scolaire:read')")
    @GetMapping("/{id}") public ResponseEntity<Installment> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @Operation(summary = "Lister les échéances d'un frais scolaire")
    @PreAuthorize("hasAuthority('frais_scolaire:read')")
    @GetMapping("/fee/{feeId}") public List<Installment> findByFee(@PathVariable Long feeId) { return useCase.findByFeeId(feeId); }
    @Operation(summary = "Supprimer une échéance")
    @PreAuthorize("hasAuthority('frais_scolaire:delete')")
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { useCase.delete(id); return ResponseEntity.noContent().build(); }

    @Operation(summary = "Déclenche immédiatement la vérification des échéances en retard (même traitement "
            + "que le job planifié de 6h) — pour tester une relance sans attendre le lendemain matin")
    @PreAuthorize("hasAuthority('frais_scolaire:update')")
    @PostMapping("/check-overdue-now") public ResponseEntity<Void> checkOverdueNow() {
        useCase.checkOverdueNow();
        return ResponseEntity.noContent().build();
    }
}
