package gn.lacaeducation.expenses.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.expenses.application.service.ExpenseService;
import gn.lacaeducation.expenses.application.service.ExpenseService.CreateExpenseCommand;
import gn.lacaeducation.expenses.domain.Expense;
import gn.lacaeducation.expenses.domain.ExpensePaymentMode;
import gn.lacaeducation.expenses.domain.ExpenseStatus;
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

@Tag(name = "Expenses", description = "School expenses with approval workflow")
@RestController
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService service;

    public record ExpenseRequest(Long categoryId, Long supplierId, String label, BigDecimal amount,
                                 String currency, LocalDate expenseDate, ExpensePaymentMode paymentMode,
                                 String reference, String justificationUrl, Long budgetLineId) {}
    public record RejectRequest(String reason) {}

    @Operation(summary = "List expenses for the current tenant (optional status filter)")
    @GetMapping
    public List<Expense> list(@RequestParam(required = false) ExpenseStatus status) {
        Long tenantId = TenantContext.get();
        return status != null ? service.findByStatus(tenantId, status) : service.findByTenant(tenantId);
    }

    @Operation(summary = "Get an expense by ID")
    @GetMapping("/{id}")
    public Expense findById(@PathVariable Long id) { return service.findById(id); }

    @Operation(summary = "Create a draft expense")
    @PreAuthorize("hasAuthority('depense:create')")
    @PostMapping
    public Expense create(@RequestBody ExpenseRequest r) {
        return service.create(toCommand(r));
    }

    @Operation(summary = "Update a draft expense")
    @PreAuthorize("hasAuthority('depense:create')")
    @PutMapping("/{id}")
    public Expense update(@PathVariable Long id, @RequestBody ExpenseRequest r) {
        return service.update(id, toCommand(r));
    }

    @Operation(summary = "Submit a draft expense for approval")
    @PreAuthorize("hasAuthority('depense:create')")
    @PatchMapping("/{id}/submit")
    public Expense submit(@PathVariable Long id) { return service.submit(id); }

    @Operation(summary = "Approve a submitted expense (director)")
    @PreAuthorize("hasAuthority('depense:approve')")
    @PatchMapping("/{id}/approve")
    public Expense approve(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return service.approve(id, currentUserId(jwt));
    }

    @Operation(summary = "Reject a submitted expense with a reason (director)")
    @PreAuthorize("hasAuthority('depense:approve')")
    @PatchMapping("/{id}/reject")
    public Expense reject(@PathVariable Long id, @RequestBody RejectRequest r, @AuthenticationPrincipal Jwt jwt) {
        return service.reject(id, currentUserId(jwt), r.reason());
    }

    @Operation(summary = "Mark an approved expense as paid (accountant)")
    @PreAuthorize("hasAuthority('depense:create')")
    @PatchMapping("/{id}/pay")
    public Expense pay(@PathVariable Long id) { return service.pay(id); }

    @Operation(summary = "Delete a draft expense")
    @PreAuthorize("hasAuthority('depense:create')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.noContent().build(); }

    private CreateExpenseCommand toCommand(ExpenseRequest r) {
        return new CreateExpenseCommand(TenantContext.get(), r.categoryId(), r.supplierId(), r.label(),
                r.amount(), r.currency(), r.expenseDate(), r.paymentMode(), r.reference(), r.justificationUrl(),
                r.budgetLineId());
    }

    private Long currentUserId(Jwt jwt) {
        try { return Long.parseLong(jwt.getSubject()); } catch (RuntimeException e) { return null; }
    }
}
