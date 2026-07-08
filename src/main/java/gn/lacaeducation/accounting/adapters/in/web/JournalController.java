package gn.lacaeducation.accounting.adapters.in.web;

import gn.lacaeducation.accounting.application.service.DoubleEntryService;
import gn.lacaeducation.accounting.domain.Journal;
import gn.lacaeducation.common.TenantContext;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Journals", description = "Accounting journals")
@RestController
@RequestMapping("/api/v1/accounting/journaux")
@RequiredArgsConstructor
public class JournalController {

    private final DoubleEntryService service;

    public record JournalRequest(String code, String label, String type, Long defaultCounterpartAccountId) {}

    @GetMapping public List<Journal> list() { return service.journals(TenantContext.get()); }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
    @PostMapping public Journal create(@RequestBody JournalRequest r) {
        return service.saveJournal(Journal.builder().tenantId(TenantContext.get())
                .code(r.code()).label(r.label()).type(r.type()).defaultCounterpartAccountId(r.defaultCounterpartAccountId()).build());
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SCHOOL_ADMIN','DIRECTOR')")
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { service.deleteJournal(id); return ResponseEntity.noContent().build(); }
}
