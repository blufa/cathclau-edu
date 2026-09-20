package gn.lacaeducation.academics.adapters.in.web;

import gn.lacaeducation.academics.application.port.in.GererSchoolEventUseCase;
import gn.lacaeducation.academics.application.port.in.GererSchoolEventUseCase.CreateSchoolEventCommand;
import gn.lacaeducation.academics.domain.SchoolEvent;
import gn.lacaeducation.common.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Événements de l'école (kermesse, match, entraînement...) qui empêchent les élèves de suivre le cours
 * sur une période donnée — décidés par le fondateur/la direction. Les enseignants dont un créneau
 * tombe dans cette période restent payés sur ces heures (§payroll ↔ academics).
 */
@Tag(name = "School events", description = "Événements école empêchant le cours (kermesse, match...) — les enseignants concernés restent payés")
@RestController
@RequestMapping("/api/v1/academics/school-events")
@RequiredArgsConstructor
public class SchoolEventController {

    private final GererSchoolEventUseCase useCase;

    public record CreateRequest(String label, LocalDate startDate, LocalDate endDate,
                                LocalTime startTime, LocalTime endTime, String reason) {}

    @Operation(summary = "Créer un événement école (les élèves ne suivent pas cours, les enseignants concernés restent payés)")
    @PreAuthorize("hasAuthority('evenement:manage')")
    @PostMapping
    public SchoolEvent create(@RequestBody CreateRequest r) {
        return useCase.create(new CreateSchoolEventCommand(TenantContext.get(), r.label(), r.startDate(), r.endDate(),
                r.startTime(), r.endTime(), r.reason()));
    }

    @Operation(summary = "Lister les événements école du tenant")
    @PreAuthorize("hasAuthority('evenement:manage')")
    @GetMapping
    public List<SchoolEvent> list() {
        return useCase.findByTenant(TenantContext.get());
    }

    @Operation(summary = "Supprimer un événement école")
    @PreAuthorize("hasAuthority('evenement:manage')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        useCase.delete(id);
    }
}
