package gn.lacaeducation.academics.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.academics.application.port.in.GererClasseUseCase;
import gn.lacaeducation.academics.application.port.in.GererReferentielUseCase;
import gn.lacaeducation.academics.application.port.in.GererReferentielUseCase.CreateRoomCommand;
import gn.lacaeducation.academics.domain.ClasseMatiere;
import gn.lacaeducation.academics.domain.Matiere;
import gn.lacaeducation.academics.domain.Niveau;
import gn.lacaeducation.academics.domain.Salle;
import gn.lacaeducation.academics.domain.referentiel.MatiereModele;
import gn.lacaeducation.academics.domain.valueobject.SchoolCycle;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

@Tag(name = "Academic Referential", description = "Manage reference data: rooms, levels, and subjects")
@RestController
@RequestMapping("/api/v1/academics")
@RequiredArgsConstructor
public class ReferentielController {

    private final GererReferentielUseCase useCase;
    private final GererClasseUseCase classeUseCase;

    @Operation(summary = "Create a new room")
    @PostMapping("/rooms")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    public ResponseEntity<Salle> createRoom(@RequestBody CreateRoomCommand cmd) {
        Salle s = useCase.createRoom(new CreateRoomCommand(
                TenantContext.get(), cmd.label(), cmd.building(), cmd.capacity(), cmd.type()
        ));
        return ResponseEntity.created(URI.create("/api/academics/rooms/" + s.getId())).body(s);
    }

    @Operation(summary = "List all rooms for the current tenant")
    @GetMapping("/rooms")
    public List<Salle> findRooms() { return useCase.findRooms(TenantContext.get()); }

    @Operation(summary = "Update a room")
    @PutMapping("/rooms/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    public Salle updateRoom(@PathVariable Long id, @RequestBody CreateRoomCommand cmd) {
        return useCase.updateRoom(id, new CreateRoomCommand(
                TenantContext.get(), cmd.label(), cmd.building(), cmd.capacity(), cmd.type()));
    }

    @Operation(summary = "Set a room's availability")
    @PatchMapping("/rooms/{id}/availability")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    public Salle setRoomAvailability(@PathVariable Long id, @RequestParam boolean available) {
        return useCase.setRoomAvailability(id, available);
    }

    @Operation(summary = "Delete a room")
    @DeleteMapping("/rooms/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRoom(@PathVariable Long id) { useCase.deleteRoom(id); }

    @Operation(summary = "Create a new school level (grade level)")
    @PostMapping("/levels")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    public ResponseEntity<Niveau> createLevel(@RequestParam String label, @RequestParam Integer sortOrder,
            @RequestParam(required = false) SchoolCycle cycle) {
        Niveau n = useCase.createLevel(TenantContext.get(), label, sortOrder, cycle);
        return ResponseEntity.created(URI.create("/api/academics/levels/" + n.getId())).body(n);
    }

    @Operation(summary = "List all school levels for the current tenant")
    @GetMapping("/levels")
    public List<Niveau> findLevels() { return useCase.findLevels(TenantContext.get()); }

    @Operation(summary = "Update a school level")
    @PutMapping("/levels/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    public Niveau updateLevel(@PathVariable Long id, @RequestParam String label, @RequestParam Integer sortOrder,
            @RequestParam(required = false) SchoolCycle cycle) {
        return useCase.updateLevel(id, label, sortOrder, cycle);
    }

    @Operation(summary = "Delete a school level")
    @DeleteMapping("/levels/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLevel(@PathVariable Long id) { useCase.deleteLevel(id); }

    @Operation(summary = "Create a new subject")
    @PostMapping("/subjects")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    public ResponseEntity<Matiere> createSubject(
            @RequestParam String label,
            @RequestParam BigDecimal coefficient,
            @RequestParam(required = false) String syllabus) {
        Matiere m = useCase.createSubject(TenantContext.get(), label, coefficient, syllabus);
        return ResponseEntity.created(URI.create("/api/academics/subjects/" + m.getId())).body(m);
    }

    @Operation(summary = "List all subjects for the current tenant")
    @GetMapping("/subjects")
    public List<Matiere> findSubjects() { return useCase.findSubjects(TenantContext.get()); }

    @Operation(summary = "Update a subject")
    @PutMapping("/subjects/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    public Matiere updateSubject(@PathVariable Long id,
                                 @RequestParam String label,
                                 @RequestParam BigDecimal coefficient,
                                 @RequestParam(required = false) String syllabus) {
        return useCase.updateSubject(id, label, coefficient, syllabus);
    }

    @Operation(summary = "Delete a subject")
    @DeleteMapping("/subjects/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSubject(@PathVariable Long id) { useCase.deleteSubject(id); }

    @Operation(summary = "Get the official MEPUA subject/coefficient catalog for a cycle (série required for LYCEE: SCIENCES/SOCIALES for 11ème, SM/SE/SS for 12ème-Terminale)")
    @GetMapping("/subjects/catalogue-mepua")
    public List<MatiereModele> catalogueMepua(@RequestParam SchoolCycle cycle, @RequestParam(required = false) String serie) {
        return useCase.catalogueMepua(cycle, serie);
    }

    @Operation(summary = "Import the MEPUA subjects/coefficients for a cycle (and série) as subjects of the current tenant, skipping subjects that already exist")
    @PostMapping("/subjects/importer-mepua")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    public List<Matiere> importerMepua(@RequestParam SchoolCycle cycle, @RequestParam(required = false) String serie) {
        return useCase.importerMatieresMepua(TenantContext.get(), cycle, serie);
    }

    @Operation(summary = "List the teacher codes already assigned for a subject (e.g. CH1, CH2), across all classes — used by the timetable grid to assign/reassign a teacher by code")
    @GetMapping("/subjects/{subjectId}/teacher-codes")
    public List<ClasseMatiere> teacherCodesForSubject(@PathVariable Long subjectId) {
        return classeUseCase.findTeacherCodesForSubject(subjectId);
    }
}
