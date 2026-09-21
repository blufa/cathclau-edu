package gn.lacaeducation.accounting.adapters.in.web;

import gn.lacaeducation.accounting.application.service.TreasuryService;
import gn.lacaeducation.accounting.application.service.TreasuryService.MovementCommand;
import gn.lacaeducation.accounting.domain.*;
import gn.lacaeducation.common.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Tag(name = "Treasury", description = "Caisses / comptes bancaires et mouvements de trésorerie (comptabilité SIMPLE)")
@RestController
@RequestMapping("/api/v1/accounting")
@RequiredArgsConstructor
public class TreasuryController {

    private final TreasuryService service;

    public record AccountRequest(String label, TreasuryAccountType type, String bank, String accountNumber,
                                 BigDecimal initialBalance, String currency, Boolean active) {}
    public record AdjustRequest(BigDecimal balance) {}
    public record MovementRequest(Long accountId, CashFlowDirection direction, BigDecimal amount, LocalDate date,
                                  CashFlowCategory category, String label, String justificationUrl) {}
    public record JournalLine(CashFlowEntry entry, BigDecimal runningBalance) {}

    // ── Accounts ──────────────────────────────────────────────────────
    @Operation(summary = "Lister les comptes de trésorerie (caisses et comptes bancaires)")
    @GetMapping("/comptes")
    public List<TreasuryAccount> accounts() { return service.accounts(TenantContext.get()); }

    @Operation(summary = "Créer un compte de trésorerie")
    @PreAuthorize("hasAuthority('comptabilite:manage')")
    @PostMapping("/comptes")
    public TreasuryAccount create(@RequestBody AccountRequest r) {
        return service.saveAccount(TreasuryAccount.builder().tenantId(TenantContext.get())
                .label(r.label()).type(r.type()).bank(r.bank()).accountNumber(r.accountNumber())
                .initialBalance(r.initialBalance()).currency(r.currency()).active(r.active()).build());
    }

    @Operation(summary = "Ajuster le solde d'un compte (régularisation)")
    @PreAuthorize("hasAuthority('comptabilite:manage')")
    @PatchMapping("/comptes/{id}/ajuster-solde")
    public TreasuryAccount adjust(@PathVariable Long id, @RequestBody AdjustRequest r) { return service.adjustBalance(id, r.balance()); }

    @Operation(summary = "Supprimer un compte de trésorerie")
    @PreAuthorize("hasAuthority('comptabilite:approve')")
    @DeleteMapping("/comptes/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) { service.deleteAccount(id); return ResponseEntity.noContent().build(); }

    // ── Movements ─────────────────────────────────────────────────────
    @Operation(summary = "Lister les mouvements de trésorerie de l'établissement")
    @GetMapping("/mouvements")
    public List<CashFlowEntry> movements() { return service.movements(TenantContext.get()); }

    @Operation(summary = "Enregistrer un mouvement de trésorerie manuel")
    @PreAuthorize("hasAuthority('comptabilite:manage')")
    @PostMapping("/mouvements")
    public CashFlowEntry createMovement(@RequestBody MovementRequest r) {
        return service.createMovement(new MovementCommand(TenantContext.get(), r.accountId(), r.direction(),
                r.amount(), r.date(), r.category(), r.label(), r.justificationUrl()));
    }

    @Operation(summary = "Modifier un mouvement")
    @PreAuthorize("hasAuthority('comptabilite:manage')")
    @PutMapping("/mouvements/{id}")
    public CashFlowEntry updateMovement(@PathVariable Long id, @RequestBody MovementRequest r) {
        return service.updateMovement(id, new MovementCommand(TenantContext.get(), r.accountId(), r.direction(),
                r.amount(), r.date(), r.category(), r.label(), r.justificationUrl()));
    }

    @Operation(summary = "Supprimer un mouvement")
    @PreAuthorize("hasAuthority('comptabilite:manage')")
    @DeleteMapping("/mouvements/{id}")
    public ResponseEntity<Void> deleteMovement(@PathVariable Long id) { service.deleteMovement(id); return ResponseEntity.noContent().build(); }

    @Operation(summary = "Rapprocher un mouvement (rapprochement bancaire)")
    @PreAuthorize("hasAuthority('comptabilite:manage')")
    @PostMapping("/mouvements/{id}/rapprocher")
    public CashFlowEntry reconcile(@PathVariable Long id) { return service.reconcile(id); }

    // ── Cash journal / livre de caisse ────────────────────────────────
    @Operation(summary = "Journal de caisse d'un compte : écritures avec solde progressif")
    @GetMapping("/comptes/{id}/journal")
    public List<JournalLine> journal(@PathVariable Long id) {
        TreasuryAccount account = service.accounts(TenantContext.get()).stream().filter(a -> a.getId().equals(id)).findFirst().orElse(null);
        BigDecimal running = account != null && account.getInitialBalance() != null ? account.getInitialBalance() : BigDecimal.ZERO;
        List<JournalLine> out = new ArrayList<>();
        for (CashFlowEntry e : service.accountJournal(id)) {
            running = e.getDirection() == CashFlowDirection.IN ? running.add(e.getAmount()) : running.subtract(e.getAmount());
            out.add(new JournalLine(e, running));
        }
        return out;
    }
}
