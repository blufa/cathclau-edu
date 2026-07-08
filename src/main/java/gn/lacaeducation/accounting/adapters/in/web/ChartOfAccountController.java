package gn.lacaeducation.accounting.adapters.in.web;

import gn.lacaeducation.accounting.application.service.DoubleEntryService;
import gn.lacaeducation.accounting.domain.AccountType;
import gn.lacaeducation.accounting.domain.ChartOfAccount;
import gn.lacaeducation.common.TenantContext;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Chart of Accounts", description = "SYSCOHADA chart of accounts")
@RestController
@RequestMapping("/api/v1/accounting/plan-comptable")
@RequiredArgsConstructor
public class ChartOfAccountController {

    private final DoubleEntryService service;

    public record AccountRequest(String accountNumber, String label, Integer accountClass, AccountType type, Long parentId, Boolean lettrable, Boolean active) {}

    @GetMapping public List<ChartOfAccount> list() { return service.accounts(TenantContext.get()); }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
    @PostMapping public ChartOfAccount create(@RequestBody AccountRequest r) {
        return service.saveAccount(ChartOfAccount.builder().tenantId(TenantContext.get())
                .accountNumber(r.accountNumber()).label(r.label()).accountClass(r.accountClass())
                .type(r.type()).parentId(r.parentId()).lettrable(r.lettrable()).active(r.active()).build());
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
    @PutMapping("/{id}") public ChartOfAccount update(@PathVariable Long id, @RequestBody AccountRequest r) {
        return service.updateAccount(id, ChartOfAccount.builder()
                .accountNumber(r.accountNumber()).label(r.label()).accountClass(r.accountClass())
                .type(r.type()).parentId(r.parentId()).lettrable(r.lettrable()).active(r.active()).build());
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SCHOOL_ADMIN','DIRECTOR')")
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { service.deleteAccount(id); return ResponseEntity.noContent().build(); }
}
