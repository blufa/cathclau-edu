package gn.lacaeducation.pettycash.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.pettycash.application.service.PettyCashService;
import gn.lacaeducation.pettycash.application.service.PettyCashService.FundCommand;
import gn.lacaeducation.pettycash.application.service.PettyCashService.TxCommand;
import gn.lacaeducation.pettycash.domain.PettyCashFund;
import gn.lacaeducation.pettycash.domain.PettyCashTransaction;
import gn.lacaeducation.pettycash.domain.PettyCashTxType;
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
import java.util.Map;

@Tag(name = "Petty Cash", description = "Petite caisse à fonds fixe : bons de sortie et réapprovisionnements")
@RestController
@RequestMapping("/api/v1/petty-cash")
@RequiredArgsConstructor
public class PettyCashController {

    private final PettyCashService service;

    public record FundRequest(String label, BigDecimal floatAmount, String custodian, Boolean active) {}
    public record TxRequest(LocalDate txDate, PettyCashTxType txType, BigDecimal amount, String voucherNumber,
                            String description, String category, String recipient, String justificationUrl) {}

    @Operation(summary = "Lister les caisses du tenant")
    @GetMapping("/funds")
    public List<PettyCashFund> funds() { return service.funds(TenantContext.get()); }

    @Operation(summary = "Détail d'une caisse")
    @GetMapping("/funds/{id}")
    public PettyCashFund fund(@PathVariable Long id) { return service.fund(id); }

    @Operation(summary = "Opérations d'une caisse")
    @GetMapping("/funds/{id}/transactions")
    public List<PettyCashTransaction> transactions(@PathVariable Long id) { return service.transactions(id); }

    @Operation(summary = "Montant suggéré de réapprovisionnement (fonds fixe - solde)")
    @GetMapping("/funds/{id}/replenishment-suggestion")
    public Map<String, BigDecimal> replenishment(@PathVariable Long id) {
        return Map.of("amount", service.replenishmentSuggestion(id));
    }

    @Operation(summary = "Créer une caisse")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
    @PostMapping("/funds")
    public PettyCashFund createFund(@RequestBody FundRequest r) {
        return service.createFund(new FundCommand(TenantContext.get(), r.label(), r.floatAmount(), r.custodian(), r.active()));
    }

    @Operation(summary = "Modifier une caisse")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
    @PutMapping("/funds/{id}")
    public PettyCashFund updateFund(@PathVariable Long id, @RequestBody FundRequest r) {
        return service.updateFund(id, new FundCommand(TenantContext.get(), r.label(), r.floatAmount(), r.custodian(), r.active()));
    }

    @Operation(summary = "Supprimer une caisse")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @DeleteMapping("/funds/{id}")
    public ResponseEntity<Void> deleteFund(@PathVariable Long id) { service.deleteFund(id); return ResponseEntity.noContent().build(); }

    @Operation(summary = "Enregistrer une opération (bon de sortie ou réapprovisionnement)")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT','SECRETARY')")
    @PostMapping("/funds/{id}/transactions")
    public PettyCashTransaction record(@PathVariable Long id, @RequestBody TxRequest r, @AuthenticationPrincipal Jwt jwt) {
        return service.record(id, new TxCommand(r.txDate(), r.txType(), r.amount(), r.voucherNumber(),
                r.description(), r.category(), r.recipient(), r.justificationUrl(), currentUserId(jwt)));
    }

    @Operation(summary = "Supprimer une opération")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
    @DeleteMapping("/transactions/{txId}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long txId) { service.deleteTransaction(txId); return ResponseEntity.noContent().build(); }

    private Long currentUserId(Jwt jwt) {
        try { return jwt == null ? null : Long.parseLong(jwt.getSubject()); } catch (RuntimeException e) { return null; }
    }
}
