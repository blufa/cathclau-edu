package gn.lacaeducation.expenses.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.expenses.application.service.AnnualBudgetService;
import gn.lacaeducation.expenses.application.service.AnnualBudgetService.BudgetCommand;
import gn.lacaeducation.expenses.application.service.AnnualBudgetService.LineCommand;
import gn.lacaeducation.expenses.domain.AnnualBudget;
import gn.lacaeducation.expenses.domain.BudgetLine;
import gn.lacaeducation.expenses.domain.BudgetLineType;
import gn.lacaeducation.expenses.domain.FinancingMode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "Annual Budgets", description = "Budget annuel de l'école : lignes recettes/dépenses, prévu vs réalisé")
@RestController
@RequestMapping("/api/v1/annual-budgets")
@RequiredArgsConstructor
public class AnnualBudgetController {

    private final AnnualBudgetService service;

    public record BudgetRequest(String academicYear, String label, String notes,
                                FinancingMode financingMode, BigDecimal financingAmount) {}
    public record LineRequest(BudgetLineType lineType, String category, Integer quantity, BigDecimal unitPrice,
                              BigDecimal plannedAmount, BigDecimal actualAmount, String notes) {}

    @Operation(summary = "Lister les budgets annuels du tenant")
    @GetMapping
    public List<AnnualBudget> list() { return service.list(TenantContext.get()); }

    @Operation(summary = "Détail d'un budget annuel")
    @GetMapping("/{id}")
    public AnnualBudget get(@PathVariable Long id) { return service.get(id); }

    @Operation(summary = "Lignes d'un budget")
    @GetMapping("/{id}/lines")
    public List<BudgetLine> lines(@PathVariable Long id) { return service.lines(id); }

    @Operation(summary = "Synthèse prévu vs réalisé d'un budget")
    @GetMapping("/{id}/summary")
    public AnnualBudgetService.BudgetSummary summary(@PathVariable Long id) { return service.summary(id); }

    @Operation(summary = "Créer un budget annuel")
    @PreAuthorize("hasAuthority('budget:manage')")
    @PostMapping
    public AnnualBudget create(@RequestBody BudgetRequest r) {
        return service.create(new BudgetCommand(TenantContext.get(), r.academicYear(), r.label(), r.notes(),
                r.financingMode(), r.financingAmount()));
    }

    @Operation(summary = "Modifier un budget annuel")
    @PreAuthorize("hasAuthority('budget:manage')")
    @PutMapping("/{id}")
    public AnnualBudget update(@PathVariable Long id, @RequestBody BudgetRequest r) {
        return service.update(id, new BudgetCommand(TenantContext.get(), r.academicYear(), r.label(), r.notes(),
                r.financingMode(), r.financingAmount()));
    }

    @Operation(summary = "Obtenir (en la générant si besoin) l'URL du PDF récapitulatif du budget")
    @GetMapping("/{id}/pdf")
    public ResponseEntity<java.util.Map<String, String>> pdf(@PathVariable Long id) {
        return ResponseEntity.ok(java.util.Map.of("url", service.pdfUrl(id)));
    }

    @Operation(summary = "Flux des octets du PDF du budget (aperçu direct, indépendant de Cloudinary)")
    @GetMapping("/{id}/pdf/download")
    public ResponseEntity<byte[]> pdfDownload(@PathVariable Long id) {
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"budget-" + id + ".pdf\"")
                .body(service.pdfBytes(id));
    }

    @Operation(summary = "Soumettre le budget à la validation de la direction (comptable)")
    @PreAuthorize("hasAuthority('budget:manage')")
    @PatchMapping("/{id}/submit")
    public AnnualBudget submit(@PathVariable Long id) { return service.submit(id); }

    @Operation(summary = "Approuver un budget soumis (direction)")
    @PreAuthorize("hasAuthority('budget:approve')")
    @PatchMapping("/{id}/approve")
    public AnnualBudget approve(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return service.approve(id, currentUserId(jwt));
    }

    public record RejectRequest(String reason) {}

    @Operation(summary = "Rejeter un budget soumis, avec motif (direction)")
    @PreAuthorize("hasAuthority('budget:approve')")
    @PatchMapping("/{id}/reject")
    public AnnualBudget reject(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt, @RequestBody(required = false) RejectRequest r) {
        return service.reject(id, currentUserId(jwt), r != null ? r.reason() : null);
    }

    @Operation(summary = "Clôturer un budget")
    @PreAuthorize("hasAuthority('budget:approve')")
    @PatchMapping("/{id}/close")
    public AnnualBudget close(@PathVariable Long id) { return service.close(id); }

    @Operation(summary = "Supprimer un budget")
    @PreAuthorize("hasAuthority('budget:approve')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.noContent().build(); }

    // ── Lignes ──────────────────────────────────────────────────────────────────

    @Operation(summary = "Ajouter une ligne au budget")
    @PreAuthorize("hasAuthority('budget:manage')")
    @PostMapping("/{id}/lines")
    public BudgetLine addLine(@PathVariable Long id, @RequestBody LineRequest r) {
        return service.addLine(id, new LineCommand(r.lineType(), r.category(), r.quantity(), r.unitPrice(),
                r.plannedAmount(), r.actualAmount(), r.notes()));
    }

    @Operation(summary = "Modifier une ligne de budget")
    @PreAuthorize("hasAuthority('budget:manage')")
    @PutMapping("/lines/{lineId}")
    public BudgetLine updateLine(@PathVariable Long lineId, @RequestBody LineRequest r) {
        return service.updateLine(lineId, new LineCommand(r.lineType(), r.category(), r.quantity(), r.unitPrice(),
                r.plannedAmount(), r.actualAmount(), r.notes()));
    }

    @Operation(summary = "Supprimer une ligne de budget")
    @PreAuthorize("hasAuthority('budget:manage')")
    @DeleteMapping("/lines/{lineId}")
    public ResponseEntity<Void> deleteLine(@PathVariable Long lineId) { service.deleteLine(lineId); return ResponseEntity.noContent().build(); }

    private Long currentUserId(Jwt jwt) {
        try { return jwt == null ? null : Long.parseLong(jwt.getSubject()); } catch (RuntimeException e) { return null; }
    }
}
