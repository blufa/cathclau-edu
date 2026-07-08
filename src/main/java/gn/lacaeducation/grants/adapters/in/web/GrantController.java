package gn.lacaeducation.grants.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.grants.application.service.GrantService;
import gn.lacaeducation.grants.application.service.GrantService.GrantCommand;
import gn.lacaeducation.grants.application.service.GrantService.PaymentCommand;
import gn.lacaeducation.grants.domain.Grant;
import gn.lacaeducation.grants.domain.GrantPayment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "Grants", description = "Subventions scolaires (recettes hors frais parents) et versements reçus")
@RestController
@RequestMapping("/api/v1/grants")
@RequiredArgsConstructor
public class GrantController {

    private final GrantService service;

    public record GrantRequest(String source, String reference, String purpose, String academicYear,
                               BigDecimal totalAmount, String notes) {}
    public record PaymentRequest(LocalDate paymentDate, BigDecimal amount, String method, String reference,
                                 Long treasuryAccountId, String notes) {}

    @Operation(summary = "Lister les subventions du tenant")
    @GetMapping
    public List<Grant> list() { return service.list(TenantContext.get()); }

    @Operation(summary = "Détail d'une subvention")
    @GetMapping("/{id}")
    public Grant get(@PathVariable Long id) { return service.get(id); }

    @Operation(summary = "Versements reçus d'une subvention")
    @GetMapping("/{id}/payments")
    public List<GrantPayment> payments(@PathVariable Long id) { return service.payments(id); }

    @Operation(summary = "Créer une subvention")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
    @PostMapping
    public Grant create(@RequestBody GrantRequest r) {
        return service.create(new GrantCommand(TenantContext.get(), r.source(), r.reference(), r.purpose(),
                r.academicYear(), r.totalAmount(), r.notes()));
    }

    @Operation(summary = "Modifier une subvention")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
    @PutMapping("/{id}")
    public Grant update(@PathVariable Long id, @RequestBody GrantRequest r) {
        return service.update(id, new GrantCommand(TenantContext.get(), r.source(), r.reference(), r.purpose(),
                r.academicYear(), r.totalAmount(), r.notes()));
    }

    @Operation(summary = "Annuler une subvention")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
    @PatchMapping("/{id}/cancel")
    public Grant cancel(@PathVariable Long id) { return service.cancel(id); }

    @Operation(summary = "Supprimer une subvention")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.noContent().build(); }

    @Operation(summary = "Enregistrer un versement reçu")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
    @PostMapping("/{id}/payments")
    public GrantPayment addPayment(@PathVariable Long id, @RequestBody PaymentRequest r) {
        return service.addPayment(id, new PaymentCommand(r.paymentDate(), r.amount(), r.method(), r.reference(),
                r.treasuryAccountId(), r.notes()));
    }

    @Operation(summary = "Supprimer un versement")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
    @DeleteMapping("/payments/{paymentId}")
    public ResponseEntity<Void> deletePayment(@PathVariable Long paymentId) { service.deletePayment(paymentId); return ResponseEntity.noContent().build(); }
}
