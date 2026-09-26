package gn.lacaeducation.expenses.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.expenses.application.service.MonthlyExpenseSheetService;
import gn.lacaeducation.expenses.application.service.MonthlyExpenseSheetService.ExpenseLineCommand;
import gn.lacaeducation.expenses.application.service.MonthlyExpenseSheetService.RevenueLineCommand;
import gn.lacaeducation.expenses.application.service.MonthlyExpenseSheetService.SheetCommand;
import gn.lacaeducation.expenses.application.service.MonthlyExpenseSheetService.SheetDetail;
import gn.lacaeducation.expenses.domain.Expense;
import gn.lacaeducation.expenses.domain.FinancingMode;
import gn.lacaeducation.expenses.domain.MonthlyExpenseSheet;
import gn.lacaeducation.expenses.domain.RevenueLine;
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

/** Fiches de dépense mensuelles (§retour comptable, point 31) — remplace le budget annuel prévisionnel. */
@Tag(name = "Monthly Expense Sheets", description = "Fiches de dépense mensuelles (dépenses + recettes d'événement)")
@RestController
@RequestMapping("/api/v1/monthly-expense-sheets")
@RequiredArgsConstructor
public class MonthlyExpenseSheetController {

    private final MonthlyExpenseSheetService service;

    public record SheetRequest(String label, String academicYear, FinancingMode financingMode, BigDecimal financingAmount) {}
    public record ExpenseLineRequest(String label, BigDecimal amount, boolean necessary, LocalDate expenseDate) {}
    public record RevenueLineRequest(String label, BigDecimal amount, LocalDate revenueDate) {}

    @Operation(summary = "Lister les fiches de dépense mensuelles du tenant courant (historique)")
    @GetMapping
    public List<MonthlyExpenseSheet> list() { return service.list(TenantContext.get()); }

    @Operation(summary = "Détail d'une fiche : lignes de dépense, de recette et totaux")
    @GetMapping("/{id}")
    public SheetDetail detail(@PathVariable Long id) { return service.detail(id); }

    @Operation(summary = "Créer une nouvelle fiche de dépense mensuelle")
    @PreAuthorize("hasAuthority('depense:create')")
    @PostMapping
    public MonthlyExpenseSheet create(@RequestBody SheetRequest r, @AuthenticationPrincipal Jwt jwt) {
        return service.create(new SheetCommand(TenantContext.get(), r.label(), r.academicYear(),
                r.financingMode(), r.financingAmount()), currentUserId(jwt));
    }

    @Operation(summary = "Clôturer une fiche : verrouille définitivement ses lignes")
    @PreAuthorize("hasAuthority('depense:create')")
    @PatchMapping("/{id}/close")
    public MonthlyExpenseSheet close(@PathVariable Long id) { return service.close(id); }

    @Operation(summary = "Ajouter une ligne de dépense à la fiche")
    @PreAuthorize("hasAuthority('depense:create')")
    @PostMapping("/{id}/expenses")
    public Expense addExpense(@PathVariable Long id, @RequestBody ExpenseLineRequest r) {
        return service.addExpense(id, new ExpenseLineCommand(r.label(), r.amount(), r.necessary(), r.expenseDate()));
    }

    @Operation(summary = "Modifier une ligne de dépense (fiche ouverte uniquement)")
    @PreAuthorize("hasAuthority('depense:create')")
    @PutMapping("/expenses/{lineId}")
    public Expense updateExpense(@PathVariable Long lineId, @RequestBody ExpenseLineRequest r) {
        return service.updateExpense(lineId, new ExpenseLineCommand(r.label(), r.amount(), r.necessary(), r.expenseDate()));
    }

    @Operation(summary = "Supprimer une ligne de dépense (fiche ouverte uniquement)")
    @PreAuthorize("hasAuthority('depense:create')")
    @DeleteMapping("/expenses/{lineId}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long lineId) { service.deleteExpense(lineId); return ResponseEntity.noContent().build(); }

    @Operation(summary = "Ajouter une ligne de recette à la fiche (budgets d'événement)")
    @PreAuthorize("hasAuthority('depense:create')")
    @PostMapping("/{id}/revenues")
    public RevenueLine addRevenue(@PathVariable Long id, @RequestBody RevenueLineRequest r) {
        return service.addRevenue(id, new RevenueLineCommand(r.label(), r.amount(), r.revenueDate()));
    }

    @Operation(summary = "Modifier une ligne de recette (fiche ouverte uniquement)")
    @PreAuthorize("hasAuthority('depense:create')")
    @PutMapping("/revenues/{lineId}")
    public RevenueLine updateRevenue(@PathVariable Long lineId, @RequestBody RevenueLineRequest r) {
        return service.updateRevenue(lineId, new RevenueLineCommand(r.label(), r.amount(), r.revenueDate()));
    }

    @Operation(summary = "Supprimer une ligne de recette (fiche ouverte uniquement)")
    @PreAuthorize("hasAuthority('depense:create')")
    @DeleteMapping("/revenues/{lineId}")
    public ResponseEntity<Void> deleteRevenue(@PathVariable Long lineId) { service.deleteRevenue(lineId); return ResponseEntity.noContent().build(); }

    private Long currentUserId(Jwt jwt) {
        try { return Long.parseLong(jwt.getSubject()); } catch (RuntimeException e) { return null; }
    }
}
