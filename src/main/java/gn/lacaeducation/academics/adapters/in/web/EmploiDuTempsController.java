package gn.lacaeducation.academics.adapters.in.web;

import gn.lacaeducation.academics.application.port.in.GererEmploiDuTempsUseCase;
import gn.lacaeducation.academics.application.port.in.GererEmploiDuTempsUseCase.CreateScheduleCommand;
import gn.lacaeducation.academics.domain.EmploiDuTemps;
import gn.lacaeducation.academics.domain.valueobject.JourSemaine;
import gn.lacaeducation.common.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@Tag(name = "Timetable", description = "Gérer l'emploi du temps des classes avec détection des conflits")
@RestController
@RequestMapping("/api/v1/academics/schedules")
@RequiredArgsConstructor
public class EmploiDuTempsController {

    private final GererEmploiDuTempsUseCase useCase;

    @Operation(summary = "Créer un créneau d'emploi du temps (rejette les conflits salle/enseignant/classe avec 409)")
    @PostMapping
    @PreAuthorize("hasAuthority('emploi:manage')")
    public ResponseEntity<EmploiDuTemps> create(@Valid @RequestBody CreateScheduleCommand cmd) {
        EmploiDuTemps e = useCase.create(new CreateScheduleCommand(
                TenantContext.get(), cmd.schoolClassId(), cmd.subjectId(), cmd.teacherId(),
                cmd.roomId(), cmd.day(), cmd.startTime(), cmd.endTime(), cmd.label()));
        return ResponseEntity.created(URI.create("/api/v1/academics/schedules/" + e.getId())).body(e);
    }

    @Operation(summary = "Rechercher des créneaux par classe, enseignant, salle et/ou jour")
    @GetMapping
    public List<EmploiDuTemps> search(@RequestParam(required = false) Long classId,
                                      @RequestParam(required = false) Long teacherId,
                                      @RequestParam(required = false) Long roomId,
                                      @RequestParam(required = false) JourSemaine day) {
        return useCase.search(classId, teacherId, roomId, day);
    }

    @Operation(summary = "Consulter un créneau par son identifiant")
    @GetMapping("/{id}")
    public ResponseEntity<EmploiDuTemps> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Modifier un créneau (revérifie les conflits)")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('emploi:manage')")
    public EmploiDuTemps update(@PathVariable Long id, @Valid @RequestBody CreateScheduleCommand cmd) {
        return useCase.update(id, new CreateScheduleCommand(
                TenantContext.get(), cmd.schoolClassId(), cmd.subjectId(), cmd.teacherId(),
                cmd.roomId(), cmd.day(), cmd.startTime(), cmd.endTime(), cmd.label()));
    }

    @Operation(summary = "Supprimer un créneau")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('emploi:manage')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { useCase.delete(id); }

    @Operation(summary = "Consulter l'emploi du temps d'une classe")
    @GetMapping("/by-class/{classId}")
    public List<EmploiDuTemps> byClass(@PathVariable Long classId) {
        return useCase.findByClass(classId);
    }

    @Operation(summary = "Consulter l'emploi du temps d'un enseignant")
    @GetMapping("/by-teacher/{teacherId}")
    public List<EmploiDuTemps> byTeacher(@PathVariable Long teacherId) {
        return useCase.findByTeacher(teacherId);
    }
}
