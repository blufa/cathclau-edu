package gn.lacaeducation.academics.adapters.in.web;

import gn.lacaeducation.academics.application.port.in.GererSchoolEventUseCase;
import gn.lacaeducation.academics.application.port.in.GererSchoolEventUseCase.CreateSchoolEventCommand;
import gn.lacaeducation.academics.application.port.in.GererSchoolEventUseCase.TouchedTeacher;
import gn.lacaeducation.academics.domain.SchoolEvent;
import gn.lacaeducation.common.TenantContext;
import org.springframework.web.bind.annotation.PutMapping;
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
 * tombe dans cette période ne sont payés que si la direction les a explicitement crédités pour CET
 * événement (§payroll ↔ academics). Un jour férié (chômé et payé par l'État) existe séparément dans
 * le calendrier académique et reste toujours payé automatiquement, sans passer par ici.
 */
@Tag(name = "School events", description = "Événements école empêchant le cours (kermesse, match...) — la direction choisit les enseignants payés")
@RestController
@RequestMapping("/api/v1/academics/school-events")
@RequiredArgsConstructor
public class SchoolEventController {

    private final GererSchoolEventUseCase useCase;

    public record CreateRequest(String label, LocalDate startDate, LocalDate endDate,
                                LocalTime startTime, LocalTime endTime, String reason) {}
    public record CreditedTeachersRequest(List<Long> teacherIds) {}

    @Operation(summary = "Créer un événement école (kermesse, sortie... la direction choisit ensuite les enseignants payés)")
    @PreAuthorize("hasAuthority('evenement:manage')")
    @PostMapping
    public SchoolEvent create(@RequestBody CreateRequest r) {
        return useCase.create(new CreateSchoolEventCommand(TenantContext.get(), r.label(), r.startDate(), r.endDate(),
                r.startTime(), r.endTime(), r.reason()));
    }

    @Operation(summary = "Lister les enseignants dont un créneau est touché par cet événement, avec leur statut de crédit")
    @PreAuthorize("hasAuthority('evenement:manage')")
    @GetMapping("/{id}/enseignants-touches")
    public List<TouchedTeacher> touchedTeachers(@PathVariable Long id) {
        return useCase.findTouchedTeachers(id);
    }

    @Operation(summary = "Choisir les enseignants payés (crédités) pour cet événement")
    @PreAuthorize("hasAuthority('evenement:manage')")
    @PutMapping("/{id}/enseignants-credites")
    public void setCreditedTeachers(@PathVariable Long id, @RequestBody CreditedTeachersRequest r) {
        useCase.setCreditedTeachers(id, r.teacherIds());
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
