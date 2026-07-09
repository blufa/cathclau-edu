package gn.lacaeducation.accounting.adapters.in.web;

import gn.lacaeducation.accounting.application.service.AccountingSettingsService;
import gn.lacaeducation.accounting.domain.AccountingMode;
import gn.lacaeducation.accounting.domain.AccountingSettings;
import gn.lacaeducation.common.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Tag(name = "Accounting Settings", description = "Per-tenant accounting configuration")
@RestController
@RequestMapping("/api/v1/accounting/settings")
@RequiredArgsConstructor
public class AccountingSettingsController {

    private final AccountingSettingsService service;

    public record SettingsRequest(AccountingMode mode, String referential, String currency,
                                  Integer fiscalYearStartMonth, Boolean vatApplicable, BigDecimal defaultVatRate,
                                  Boolean autoNumbering, Boolean active) {}

    @Operation(summary = "Get accounting settings for the current tenant")
    @GetMapping
    public AccountingSettings get() { return service.getOrDefault(TenantContext.get()); }

    @Operation(summary = "Update accounting settings")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
    @PutMapping
    public AccountingSettings update(@RequestBody SettingsRequest r) {
        return service.update(TenantContext.get(), AccountingSettings.builder()
                .mode(r.mode()).referential(r.referential()).currency(r.currency())
                .fiscalYearStartMonth(r.fiscalYearStartMonth()).vatApplicable(r.vatApplicable())
                .defaultVatRate(r.defaultVatRate()).autoNumbering(r.autoNumbering()).active(r.active()).build());
    }
}
