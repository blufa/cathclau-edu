package gn.lacaeducation.discipline.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.discipline.application.service.DisciplineService;
import gn.lacaeducation.discipline.application.service.DisciplineService.CreateIncidentCommand;
import gn.lacaeducation.discipline.domain.DisciplineCategory;
import gn.lacaeducation.discipline.domain.DisciplineIncident;
import gn.lacaeducation.discipline.domain.DisciplineSeverity;
import gn.lacaeducation.discipline.domain.DisciplineStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Discipline", description = "Incidents et sanctions disciplinaires par élève (inspiré OpenEduCat)")
@RestController
@RequestMapping("/api/v1/discipline")
@RequiredArgsConstructor
public class DisciplineController {

    private final DisciplineService service;

    public record IncidentRequest(Long studentId, Long classId, LocalDate incidentDate,
                                  DisciplineCategory category, DisciplineSeverity severity,
                                  String description, String sanction,
                                  gn.lacaeducation.discipline.domain.MeasureKind measureKind,
                                  gn.lacaeducation.discipline.domain.DisciplineMeasure measure,
                                  gn.lacaeducation.discipline.domain.Suspension suspension,
                                  Integer exclusionDays,
                                  gn.lacaeducation.discipline.domain.DecisionBody decisionBody) {}

    @Operation(summary = "Lister les incidents (filtres élève / statut)")
    @PreAuthorize("hasAuthority('discipline:manage')")
    @GetMapping
    public List<DisciplineIncident> list(@RequestParam(required = false) Long studentId,
                                         @RequestParam(required = false) DisciplineStatus status) {
        return service.find(TenantContext.get(), studentId, status);
    }

    @Operation(summary = "Détail d'un incident")
    @GetMapping("/{id}")
    public DisciplineIncident findById(@PathVariable Long id) { return service.findById(id); }

    @Operation(summary = "Signaler un incident")
    @PreAuthorize("hasAuthority('discipline:manage')")
    @PostMapping
    public DisciplineIncident create(@RequestBody IncidentRequest r, @AuthenticationPrincipal Jwt jwt) {
        return service.create(new CreateIncidentCommand(TenantContext.get(), r.studentId(), r.classId(),
                r.incidentDate(), r.category(), r.severity(), r.description(), r.sanction(), currentUserId(jwt),
                r.measureKind(), r.measure(), r.suspension(), r.exclusionDays(), r.decisionBody()));
    }

    @Operation(summary = "Modifier un incident")
    @PreAuthorize("hasAuthority('discipline:manage')")
    @PutMapping("/{id}")
    public DisciplineIncident update(@PathVariable Long id, @RequestBody IncidentRequest r) {
        return service.update(id, new CreateIncidentCommand(TenantContext.get(), r.studentId(), r.classId(),
                r.incidentDate(), r.category(), r.severity(), r.description(), r.sanction(), null,
                r.measureKind(), r.measure(), r.suspension(), r.exclusionDays(), r.decisionBody()));
    }

    @Operation(summary = "Marquer l'incident comme résolu")
    @PreAuthorize("hasAuthority('discipline:manage')")
    @PatchMapping("/{id}/resolve")
    public DisciplineIncident resolve(@PathVariable Long id) { return service.resolve(id); }

    @Operation(summary = "Classer l'incident sans suite")
    @PreAuthorize("hasAuthority('discipline:manage')")
    @PatchMapping("/{id}/dismiss")
    public DisciplineIncident dismiss(@PathVariable Long id) { return service.dismiss(id); }

    @Operation(summary = "Marquer les parents comme notifiés")
    @PreAuthorize("hasAuthority('discipline:manage')")
    @PatchMapping("/{id}/notified")
    public DisciplineIncident markNotified(@PathVariable Long id) { return service.markParentNotified(id); }

    @Operation(summary = "Supprimer un incident")
    @PreAuthorize("hasAuthority('discipline:delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.noContent().build(); }

    private Long currentUserId(Jwt jwt) {
        try { return jwt == null ? null : Long.parseLong(jwt.getSubject()); } catch (RuntimeException e) { return null; }
    }
}
