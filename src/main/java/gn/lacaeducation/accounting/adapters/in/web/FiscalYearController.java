package gn.lacaeducation.accounting.adapters.in.web;

import gn.lacaeducation.accounting.application.service.FiscalYearService;
import gn.lacaeducation.accounting.application.service.FiscalYearService.CreateFiscalYearCommand;
import gn.lacaeducation.accounting.domain.FiscalYear;
import gn.lacaeducation.common.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Fiscal Years", description = "Accounting fiscal years")
@RestController
@RequestMapping("/api/v1/accounting/exercices")
@RequiredArgsConstructor
public class FiscalYearController {

    private final FiscalYearService service;

    public record FiscalYearRequest(String label, LocalDate startDate, LocalDate endDate, Long academicYearId) {}

    @Operation(summary = "List fiscal years for the current tenant")
    @GetMapping
    public List<FiscalYear> list() { return service.findByTenant(TenantContext.get()); }

    @Operation(summary = "Create a fiscal year")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
    @PostMapping
    public FiscalYear create(@RequestBody FiscalYearRequest r) {
        return service.create(new CreateFiscalYearCommand(TenantContext.get(), r.label(), r.startDate(), r.endDate(), r.academicYearId()));
    }

    @Operation(summary = "Close a fiscal year (read-only afterwards)")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @PatchMapping("/{id}/cloturer")
    public FiscalYear close(@PathVariable Long id) { return service.close(id); }
}
