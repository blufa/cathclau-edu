package gn.lacaeducation.calendar.adapters.in.web;

import gn.lacaeducation.calendar.application.service.AcademicEventService;
import gn.lacaeducation.calendar.application.service.AcademicEventService.EventCommand;
import gn.lacaeducation.calendar.domain.AcademicEvent;
import gn.lacaeducation.calendar.domain.AcademicEventType;
import gn.lacaeducation.common.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Academic Calendar", description = "Jours fériés, vacances et dates importantes par année scolaire")
@RestController
@RequestMapping("/api/v1/calendar/events")
@RequiredArgsConstructor
public class AcademicEventController {

    private final AcademicEventService service;

    public record EventRequest(Long academicYearId, String title, AcademicEventType eventType,
                               LocalDate startDate, LocalDate endDate, String description) {}

    @Operation(summary = "Lister les événements (filtre année)")
    @GetMapping
    public List<AcademicEvent> list(@RequestParam(required = false) Long academicYearId) {
        return service.find(TenantContext.get(), academicYearId);
    }

    @Operation(summary = "Détail d'un événement")
    @GetMapping("/{id}")
    public AcademicEvent get(@PathVariable Long id) { return service.findById(id); }

    @Operation(summary = "Créer un événement (férié, composition, échéance…)")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
    @PostMapping
    public AcademicEvent create(@RequestBody EventRequest r) { return service.create(toCommand(r)); }

    @Operation(summary = "Modifier un événement")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
    @PutMapping("/{id}")
    public AcademicEvent update(@PathVariable Long id, @RequestBody EventRequest r) { return service.update(id, toCommand(r)); }

    @Operation(summary = "Supprimer un événement")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.noContent().build(); }

    private EventCommand toCommand(EventRequest r) {
        return new EventCommand(TenantContext.get(), r.academicYearId(), r.title(), r.eventType(),
                r.startDate(), r.endDate(), r.description());
    }
}
