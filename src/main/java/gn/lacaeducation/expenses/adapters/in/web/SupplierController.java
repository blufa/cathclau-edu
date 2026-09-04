package gn.lacaeducation.expenses.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.expenses.application.service.ExpenseReferenceService;
import gn.lacaeducation.expenses.domain.Supplier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Suppliers", description = "Expense suppliers / vendors")
@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final ExpenseReferenceService service;

    public record SupplierRequest(String name, String type, String phone, String email, String address, String bankAccount) {}

    @Operation(summary = "List suppliers for the current tenant")
    @GetMapping
    public List<Supplier> list() { return service.suppliers(TenantContext.get()); }

    @Operation(summary = "Create a supplier")
    @PreAuthorize("hasAuthority('budget:manage')")
    @PostMapping
    public Supplier create(@RequestBody SupplierRequest r) {
        return service.saveSupplier(Supplier.builder().tenantId(TenantContext.get())
                .name(r.name()).type(r.type()).phone(r.phone()).email(r.email())
                .address(r.address()).bankAccount(r.bankAccount()).build());
    }

    @Operation(summary = "Update a supplier")
    @PreAuthorize("hasAuthority('budget:manage')")
    @PutMapping("/{id}")
    public Supplier update(@PathVariable Long id, @RequestBody SupplierRequest r) {
        return service.updateSupplier(id, Supplier.builder()
                .name(r.name()).type(r.type()).phone(r.phone()).email(r.email())
                .address(r.address()).bankAccount(r.bankAccount()).build());
    }

    @Operation(summary = "Delete a supplier")
    @PreAuthorize("hasAuthority('budget:manage')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { service.deleteSupplier(id); return ResponseEntity.noContent().build(); }
}
