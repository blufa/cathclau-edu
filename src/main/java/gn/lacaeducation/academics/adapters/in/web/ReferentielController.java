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

@Tag(name = "Academic Referential", description = "Gérer les données de référence : salles, niveaux et matières")
@RestController
@RequestMapping("/api/v1/academics")
@RequiredArgsConstructor
public class ReferentielController {

    private final GererReferentielUseCase useCase;
    private final GererClasseUseCase classeUseCase;

    @Operation(summary = "Créer une nouvelle salle")
    @PostMapping("/rooms")
    @PreAuthorize("hasAuthority('salle:manage')")
    public ResponseEntity<Salle> createRoom(@RequestBody CreateRoomCommand cmd) {
        Salle s = useCase.createRoom(new CreateRoomCommand(
                TenantContext.get(), cmd.label(), cmd.building(), cmd.capacity(), cmd.type()
        ));
        return ResponseEntity.created(URI.create("/api/academics/rooms/" + s.getId())).body(s);
    }

    @Operation(summary = "Lister toutes les salles de l'établissement")
    @GetMapping("/rooms")
    public List<Salle> findRooms() { return useCase.findRooms(TenantContext.get()); }

    @Operation(summary = "Modifier une salle")
    @PutMapping("/rooms/{id}")
    @PreAuthorize("hasAuthority('salle:manage')")
    public Salle updateRoom(@PathVariable Long id, @RequestBody CreateRoomCommand cmd) {
        return useCase.updateRoom(id, new CreateRoomCommand(
                TenantContext.get(), cmd.label(), cmd.building(), cmd.capacity(), cmd.type()));
    }

    @Operation(summary = "Définir la disponibilité d'une salle")
    @PatchMapping("/rooms/{id}/availability")
    @PreAuthorize("hasAuthority('salle:manage')")
    public Salle setRoomAvailability(@PathVariable Long id, @RequestParam boolean available) {
        return useCase.setRoomAvailability(id, available);
    }

    @Operation(summary = "Supprimer une salle")
    @DeleteMapping("/rooms/{id}")
    @PreAuthorize("hasAuthority('salle:manage')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRoom(@PathVariable Long id) { useCase.deleteRoom(id); }

    @Operation(summary = "Créer un nouveau niveau scolaire")
    @PostMapping("/levels")
    @PreAuthorize("hasAuthority('niveau:manage')")
    public ResponseEntity<Niveau> createLevel(@RequestParam String label, @RequestParam Integer sortOrder,
            @RequestParam(required = false) SchoolCycle cycle) {
        Niveau n = useCase.createLevel(TenantContext.get(), label, sortOrder, cycle);
        return ResponseEntity.created(URI.create("/api/academics/levels/" + n.getId())).body(n);
    }

    @Operation(summary = "Lister tous les niveaux scolaires de l'établissement")
    @GetMapping("/levels")
    public List<Niveau> findLevels() { return useCase.findLevels(TenantContext.get()); }

    @Operation(summary = "Modifier un niveau scolaire")
    @PutMapping("/levels/{id}")
    @PreAuthorize("hasAuthority('niveau:manage')")
    public Niveau updateLevel(@PathVariable Long id, @RequestParam String label, @RequestParam Integer sortOrder,
            @RequestParam(required = false) SchoolCycle cycle) {
        return useCase.updateLevel(id, label, sortOrder, cycle);
    }

    @Operation(summary = "Supprimer un niveau scolaire")
    @DeleteMapping("/levels/{id}")
    @PreAuthorize("hasAuthority('niveau:manage')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLevel(@PathVariable Long id) { useCase.deleteLevel(id); }

    @Operation(summary = "Créer une nouvelle matière")
    @PostMapping("/subjects")
    @PreAuthorize("hasAuthority('matiere:manage')")
    public ResponseEntity<Matiere> createSubject(
            @RequestParam String label,
            @RequestParam BigDecimal coefficient,
            @RequestParam(required = false) String syllabus) {
        Matiere m = useCase.createSubject(TenantContext.get(), label, coefficient, syllabus);
        return ResponseEntity.created(URI.create("/api/academics/subjects/" + m.getId())).body(m);
    }

    @Operation(summary = "Lister toutes les matières de l'établissement")
    @GetMapping("/subjects")
    public List<Matiere> findSubjects() { return useCase.findSubjects(TenantContext.get()); }

    @Operation(summary = "Modifier une matière")
    @PutMapping("/subjects/{id}")
    @PreAuthorize("hasAuthority('matiere:manage')")
    public Matiere updateSubject(@PathVariable Long id,
                                 @RequestParam String label,
                                 @RequestParam BigDecimal coefficient,
                                 @RequestParam(required = false) String syllabus) {
        return useCase.updateSubject(id, label, coefficient, syllabus);
    }

    @Operation(summary = "Supprimer une matière")
    @DeleteMapping("/subjects/{id}")
    @PreAuthorize("hasAuthority('matiere:manage')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSubject(@PathVariable Long id) { useCase.deleteSubject(id); }

    @Operation(summary = "Consulter le catalogue officiel MEPUA des matières/coefficients pour un cycle (série requise pour le LYCEE : LITTERAIRE ou SCIENTIFIQUE)")
    @GetMapping("/subjects/catalogue-mepua")
    public List<MatiereModele> catalogueMepua(@RequestParam SchoolCycle cycle, @RequestParam(required = false) String serie) {
        return useCase.catalogueMepua(cycle, serie);
    }

    @Operation(summary = "Importer les matières/coefficients MEPUA d'un cycle (et série) comme matières de l'établissement, en ignorant celles déjà existantes")
    @PostMapping("/subjects/importer-mepua")
    @PreAuthorize("hasAuthority('matiere:manage')")
    public List<Matiere> importerMepua(@RequestParam SchoolCycle cycle, @RequestParam(required = false) String serie) {
        return useCase.importerMatieresMepua(TenantContext.get(), cycle, serie);
    }

    @Operation(summary = "Lister les codes enseignants déjà affectés pour une matière (ex. CH1, CH2), toutes classes confondues — utilisé par la grille d'emploi du temps pour affecter/réaffecter un enseignant par code")
    @GetMapping("/subjects/{subjectId}/teacher-codes")
    public List<ClasseMatiere> teacherCodesForSubject(@PathVariable Long subjectId) {
        return classeUseCase.findTeacherCodesForSubject(subjectId);
    }
}
