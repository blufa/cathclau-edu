package gn.lacaeducation.accounting.adapters.in.web;

import gn.lacaeducation.accounting.application.service.DoubleEntryService;
import gn.lacaeducation.accounting.domain.JournalEntry;
import gn.lacaeducation.accounting.domain.JournalEntryLine;
import gn.lacaeducation.common.TenantContext;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Journal Entries", description = "Balanced double-entry vouchers")
@RestController
@RequestMapping("/api/v1/accounting/ecritures")
@RequiredArgsConstructor
public class JournalEntryController {

    private final DoubleEntryService service;

    public record EntryRequest(Long fiscalYearId, Long journalId, LocalDate date, String label,
                               String referenceType, Long referenceId, List<JournalEntryLine> lines) {}

    @GetMapping public List<JournalEntry> list(@RequestParam(required = false) Long fiscalYearId) {
        return service.entries(TenantContext.get(), fiscalYearId);
    }

    @PreAuthorize("hasAuthority('comptabilite:manage')")
    @PostMapping public JournalEntry create(@RequestBody EntryRequest r) {
        return service.createEntry(JournalEntry.builder().tenantId(TenantContext.get())
                .fiscalYearId(r.fiscalYearId()).journalId(r.journalId()).date(r.date()).label(r.label())
                .referenceType(r.referenceType()).referenceId(r.referenceId()).lines(r.lines()).build());
    }

    @PreAuthorize("hasAuthority('comptabilite:manage')")
    @PutMapping("/{id}") public JournalEntry update(@PathVariable Long id, @RequestBody EntryRequest r) {
        return service.updateEntry(id, JournalEntry.builder().date(r.date()).label(r.label()).lines(r.lines()).build());
    }

    @PreAuthorize("hasAuthority('comptabilite:manage')")
    @PatchMapping("/{id}/valider") public JournalEntry validate(@PathVariable Long id) { return service.validate(id); }

    @PreAuthorize("hasAuthority('comptabilite:manage')")
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { service.deleteEntry(id); return ResponseEntity.noContent().build(); }
}
