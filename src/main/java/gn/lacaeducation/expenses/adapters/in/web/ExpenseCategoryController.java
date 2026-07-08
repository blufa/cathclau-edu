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

@Tag(name = "Expense Categories", description = "Expense categories (tree)")
@RestController
@RequestMapping("/api/v1/expense-categories")
@RequiredArgsConstructor
public class ExpenseCategoryController {

    private final ExpenseReferenceService service;

    public record CategoryRequest(String name, Long parentId) {}

    @Operation(summary = "List expense categories for the current tenant")
    @GetMapping
    public List<ExpenseCategory> list() { return service.categories(TenantContext.get()); }

    @Operation(summary = "Create an expense category")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
    @PostMapping
    public ExpenseCategory create(@RequestBody CategoryRequest r) {
        return service.saveCategory(ExpenseCategory.builder().tenantId(TenantContext.get()).name(r.name()).parentId(r.parentId()).build());
    }

    @Operation(summary = "Update an expense category")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
    @PutMapping("/{id}")
    public ExpenseCategory update(@PathVariable Long id, @RequestBody CategoryRequest r) {
        return service.updateCategory(id, ExpenseCategory.builder().name(r.name()).parentId(r.parentId()).build());
    }

    @Operation(summary = "Delete an expense category")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { service.deleteCategory(id); return ResponseEntity.noContent().build(); }
}
