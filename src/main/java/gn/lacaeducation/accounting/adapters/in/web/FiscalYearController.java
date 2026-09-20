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

@Tag(name = "Fiscal Years", description = "Exercices comptables")
@RestController
@RequestMapping("/api/v1/accounting/exercices")
@RequiredArgsConstructor
public class FiscalYearController {

    private final FiscalYearService service;

    public record FiscalYearRequest(String label, LocalDate startDate, LocalDate endDate, Long academicYearId) {}

    @Operation(summary = "Lister les exercices comptables de l'établissement")
    @GetMapping
    public List<FiscalYear> list() { return service.findByTenant(TenantContext.get()); }

    @Operation(summary = "Créer un exercice comptable")
    @PreAuthorize("hasAuthority('comptabilite:manage')")
    @PostMapping
    public FiscalYear create(@RequestBody FiscalYearRequest r) {
        return service.create(new CreateFiscalYearCommand(TenantContext.get(), r.label(), r.startDate(), r.endDate(), r.academicYearId()));
    }

    @Operation(summary = "Clôturer un exercice comptable (lecture seule ensuite)")
    @PreAuthorize("hasAuthority('comptabilite:approve')")
    @PatchMapping("/{id}/cloturer")
    public FiscalYear close(@PathVariable Long id) { return service.close(id); }
}
