package gn.lacaeducation.expenses.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.expenses.application.service.BudgetService;
import gn.lacaeducation.expenses.application.service.BudgetService.BudgetCommand;
import gn.lacaeducation.expenses.domain.Budget;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "Budgets", description = "Dotations budgétaires annuelles par catégorie de dépense")
@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService service;

    public record BudgetRequest(Long categoryId, String academicYear, BigDecimal allocatedAmount) {}
    public record UpdateBudgetRequest(BigDecimal allocatedAmount) {}

    @Operation(summary = "Lister les dotations budgétaires du tenant courant")
    @GetMapping
    public List<Budget> list() { return service.findByTenant(TenantContext.get()); }

    @Operation(summary = "Créer une dotation budgétaire")
    @PreAuthorize("hasAuthority('budget:approve')")
    @PostMapping
    public Budget create(@RequestBody BudgetRequest r) {
        return service.save(new BudgetCommand(TenantContext.get(), r.categoryId(), r.academicYear(), r.allocatedAmount()));
    }

    @Operation(summary = "Modifier le montant d'une dotation budgétaire")
    @PreAuthorize("hasAuthority('budget:approve')")
    @PutMapping("/{id}")
    public Budget update(@PathVariable Long id, @RequestBody UpdateBudgetRequest r) {
        return service.update(id, r.allocatedAmount());
    }

    @Operation(summary = "Supprimer une dotation budgétaire")
    @PreAuthorize("hasAuthority('budget:approve')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.noContent().build(); }
}
