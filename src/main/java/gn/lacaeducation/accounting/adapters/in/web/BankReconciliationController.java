package gn.lacaeducation.accounting.adapters.in.web;

import gn.lacaeducation.accounting.application.service.BankReconciliationService;
import gn.lacaeducation.accounting.application.service.BankReconciliationService.ItemCommand;
import gn.lacaeducation.accounting.application.service.BankReconciliationService.ReconciliationCommand;
import gn.lacaeducation.accounting.domain.BankReconciliation;
import gn.lacaeducation.accounting.domain.ReconciliationItem;
import gn.lacaeducation.accounting.domain.ReconciliationItemType;
import gn.lacaeducation.common.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "Bank Reconciliation", description = "Rapprochement bancaire : relevé vs livre, éléments en suspens")
@RestController
@RequestMapping("/api/v1/accounting/reconciliations")
@RequiredArgsConstructor
public class BankReconciliationController {

    private final BankReconciliationService service;

    public record ReconciliationRequest(Long treasuryAccountId, LocalDate statementDate, BigDecimal statementBalance, String notes) {}
    public record ItemRequest(ReconciliationItemType itemType, String description, BigDecimal amount) {}

    @Operation(summary = "Lister les rapprochements du tenant")
    @GetMapping
    public List<BankReconciliation> list() { return service.list(TenantContext.get()); }

    @Operation(summary = "Détail d'un rapprochement")
    @GetMapping("/{id}")
    public BankReconciliation get(@PathVariable Long id) { return service.get(id); }

    @Operation(summary = "Éléments en suspens d'un rapprochement")
    @GetMapping("/{id}/items")
    public List<ReconciliationItem> items(@PathVariable Long id) { return service.items(id); }

    @Operation(summary = "Synthèse (soldes ajustés et écart)")
    @GetMapping("/{id}/summary")
    public BankReconciliationService.ReconciliationSummary summary(@PathVariable Long id) { return service.summary(id); }

    @Operation(summary = "Démarrer un rapprochement (capture le solde du livre)")
    @PreAuthorize("hasAuthority('comptabilite:manage')")
    @PostMapping
    public BankReconciliation create(@RequestBody ReconciliationRequest r) {
        return service.create(new ReconciliationCommand(TenantContext.get(), r.treasuryAccountId(),
                r.statementDate(), r.statementBalance(), r.notes()));
    }

    @Operation(summary = "Clôturer un rapprochement (écart nul requis)")
    @PreAuthorize("hasAuthority('comptabilite:manage')")
    @PatchMapping("/{id}/complete")
    public BankReconciliation complete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return service.complete(id, currentUserId(jwt));
    }

    @Operation(summary = "Rouvrir un rapprochement")
    @PreAuthorize("hasAuthority('comptabilite:manage')")
    @PatchMapping("/{id}/reopen")
    public BankReconciliation reopen(@PathVariable Long id) { return service.reopen(id); }

    @Operation(summary = "Supprimer un rapprochement")
    @PreAuthorize("hasAuthority('comptabilite:approve')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.noContent().build(); }

    @Operation(summary = "Ajouter un élément en suspens")
    @PreAuthorize("hasAuthority('comptabilite:manage')")
    @PostMapping("/{id}/items")
    public ReconciliationItem addItem(@PathVariable Long id, @RequestBody ItemRequest r) {
        return service.addItem(id, new ItemCommand(r.itemType(), r.description(), r.amount()));
    }

    @Operation(summary = "Supprimer un élément en suspens")
    @PreAuthorize("hasAuthority('comptabilite:manage')")
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long itemId) { service.deleteItem(itemId); return ResponseEntity.noContent().build(); }

    private Long currentUserId(Jwt jwt) {
        try { return jwt == null ? null : Long.parseLong(jwt.getSubject()); } catch (RuntimeException e) { return null; }
    }
}
