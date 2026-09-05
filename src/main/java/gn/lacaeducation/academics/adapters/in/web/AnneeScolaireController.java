package gn.lacaeducation.academics.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.academics.application.port.in.GererAnneeScolaireUseCase;
import gn.lacaeducation.academics.application.port.in.GererAnneeScolaireUseCase.CreateAcademicYearCommand;
import gn.lacaeducation.academics.application.port.in.GererAnneeScolaireUseCase.CreatePeriodCommand;
import gn.lacaeducation.academics.domain.AnneeScolaire;
import gn.lacaeducation.academics.domain.Periode;
import gn.lacaeducation.academics.domain.valueobject.StatutAnneeScolaire;
import gn.lacaeducation.common.BusinessRuleException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@Tag(name = "Academic Years", description = "Manage academic years and periods for the school")
@RestController
@RequestMapping("/api/v1/academics/academic-years")
@RequiredArgsConstructor
public class AnneeScolaireController {

    private final GererAnneeScolaireUseCase useCase;

    @Operation(summary = "Create a new academic year")
    @PostMapping
    @PreAuthorize("hasAuthority('annee:manage')")
    public ResponseEntity<AnneeScolaire> create(@Valid @RequestBody CreateAcademicYearCommand command) {
        AnneeScolaire annee = useCase.create(new CreateAcademicYearCommand(
                TenantContext.get(), command.label(), command.startDate(), command.endDate()
        ));
        return ResponseEntity.created(URI.create("/api/academics/academic-years/" + annee.getId())).body(annee);
    }

    @Operation(summary = "List all academic years for the current tenant")
    @GetMapping
    public List<AnneeScolaire> findAll() {
        return useCase.findByTenantId(TenantContext.get());
    }

    @Operation(summary = "Get the current active academic year")
    @GetMapping("/current")
    public ResponseEntity<AnneeScolaire> findCurrent() {
        return useCase.findCurrent(TenantContext.get())
                .map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get an academic year by ID")
    @GetMapping("/{id}")
    public ResponseEntity<AnneeScolaire> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Update an academic year (label, dates)")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('annee:manage')")
    public ResponseEntity<AnneeScolaire> update(@PathVariable Long id,
                                                @Valid @RequestBody CreateAcademicYearCommand cmd) {
        return ResponseEntity.ok(useCase.update(id, new CreateAcademicYearCommand(
                TenantContext.get(), cmd.label(), cmd.startDate(), cmd.endDate())));
    }

    @Operation(summary = "Change the status: EN_COURS (start), CLOTUREE (close) or ARCHIVEE (archive)")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('annee:manage')")
    public ResponseEntity<AnneeScolaire> changeStatus(@PathVariable Long id, @RequestParam StatutAnneeScolaire status) {
        AnneeScolaire result = switch (status) {
            case EN_COURS -> useCase.start(id);
            case CLOTUREE -> useCase.close(id);
            case ARCHIVEE -> useCase.archive(id);
            case PLANIFIEE -> throw new BusinessRuleException("Cannot transition back to PLANIFIEE.");
        };
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Set this academic year as the current one (only one current per tenant)")
    @PatchMapping("/{id}/set-current")
    @PreAuthorize("hasAuthority('annee:manage')")
    public ResponseEntity<AnneeScolaire> setCurrent(@PathVariable Long id) {
        return ResponseEntity.ok(useCase.setCurrent(id));
    }

    @Operation(summary = "Start (activate) an academic year")
    @PutMapping("/{id}/start")
    @PreAuthorize("hasAuthority('annee:manage')")
    public ResponseEntity<AnneeScolaire> start(@PathVariable Long id) {
        return ResponseEntity.ok(useCase.start(id));
    }

    @Operation(summary = "Close an academic year")
    @PutMapping("/{id}/close")
    @PreAuthorize("hasAuthority('annee:manage')")
    public ResponseEntity<AnneeScolaire> close(@PathVariable Long id) {
        return ResponseEntity.ok(useCase.close(id));
    }

    @Operation(summary = "Create a period (term/semester) within an academic year")
    @PostMapping("/{academicYearId}/periods")
    @PreAuthorize("hasAuthority('periode:manage')")
    public ResponseEntity<Periode> createPeriod(@PathVariable Long academicYearId,
                                                @RequestBody CreatePeriodCommand cmd) {
        Periode p = useCase.createPeriod(new CreatePeriodCommand(
                academicYearId, cmd.label(), cmd.type(), cmd.cycle(), cmd.sortOrder(), cmd.startDate(), cmd.endDate()
        ));
        return ResponseEntity.created(URI.create("/api/academics/periods/" + p.getId())).body(p);
    }

    @Operation(summary = "List all periods for an academic year")
    @GetMapping("/{academicYearId}/periods")
    public List<Periode> findPeriods(@PathVariable Long academicYearId) {
        return useCase.findPeriods(academicYearId);
    }

    @Operation(summary = "Update a period (label, type, order, dates) — only while open")
    @PutMapping("/periods/{periodId}")
    @PreAuthorize("hasAuthority('periode:manage')")
    public ResponseEntity<Periode> updatePeriod(@PathVariable Long periodId,
                                                @RequestBody CreatePeriodCommand cmd) {
        return ResponseEntity.ok(useCase.updatePeriod(periodId, new CreatePeriodCommand(
                null, cmd.label(), cmd.type(), cmd.cycle(), cmd.sortOrder(), cmd.startDate(), cmd.endDate())));
    }

    @Operation(summary = "Close a period, freezing grades and triggering report card generation")
    @PutMapping("/periods/{periodId}/close")
    @PreAuthorize("hasAuthority('periode:manage')")
    public ResponseEntity<Periode> closePeriod(@PathVariable Long periodId) {
        return ResponseEntity.ok(useCase.closePeriod(periodId));
    }
}
