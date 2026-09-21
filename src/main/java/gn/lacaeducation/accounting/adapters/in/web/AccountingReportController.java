package gn.lacaeducation.accounting.adapters.in.web;

import gn.lacaeducation.accounting.application.service.AccountingReportService;
import gn.lacaeducation.accounting.application.service.AccountingReportService.*;
import gn.lacaeducation.common.TenantContext;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Accounting Reports", description = "SYSCOHADA statements")
@RestController
@RequestMapping("/api/v1/accounting")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('comptabilite:manage')")
public class AccountingReportController {

    private final AccountingReportService service;

    @GetMapping("/balance") public List<BalanceRow> balance(@RequestParam(required = false) Long exercice) {
        return service.balance(TenantContext.get(), exercice);
    }
    @GetMapping("/grand-livre") public List<LedgerRow> generalLedger(@RequestParam Long compte, @RequestParam(required = false) Long exercice) {
        return service.generalLedger(TenantContext.get(), exercice, compte);
    }
    @GetMapping("/compte-resultat") public IncomeStatement incomeStatement(@RequestParam(required = false) Long exercice) {
        return service.incomeStatement(TenantContext.get(), exercice);
    }
    @GetMapping("/bilan") public BalanceSheet balanceSheet(@RequestParam(required = false) Long exercice) {
        return service.balanceSheet(TenantContext.get(), exercice);
    }
}
