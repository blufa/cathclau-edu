package gn.lacaeducation.expenses.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.expenses.application.service.ExpenseReferenceService;
import gn.lacaeducation.expenses.domain.ExpenseCategory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Expense Categories", description = "Catégories de dépenses (arborescence)")
@RestController
@RequestMapping("/api/v1/expense-categories")
@RequiredArgsConstructor
public class ExpenseCategoryController {

    private final ExpenseReferenceService service;

    public record CategoryRequest(String name, Long parentId) {}

    @Operation(summary = "Lister les catégories de dépenses du tenant courant")
    @GetMapping
    public List<ExpenseCategory> list() { return service.categories(TenantContext.get()); }

    @Operation(summary = "Créer une catégorie de dépense")
    @PreAuthorize("hasAuthority('budget:manage')")
    @PostMapping
    public ExpenseCategory create(@RequestBody CategoryRequest r) {
        return service.saveCategory(ExpenseCategory.builder().tenantId(TenantContext.get()).name(r.name()).parentId(r.parentId()).build());
    }

    @Operation(summary = "Modifier une catégorie de dépense")
    @PreAuthorize("hasAuthority('budget:manage')")
    @PutMapping("/{id}")
    public ExpenseCategory update(@PathVariable Long id, @RequestBody CategoryRequest r) {
        return service.updateCategory(id, ExpenseCategory.builder().name(r.name()).parentId(r.parentId()).build());
    }

    @Operation(summary = "Supprimer une catégorie de dépense")
    @PreAuthorize("hasAuthority('budget:manage')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { service.deleteCategory(id); return ResponseEntity.noContent().build(); }
}
