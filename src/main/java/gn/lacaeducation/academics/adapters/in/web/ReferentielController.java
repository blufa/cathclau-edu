package gn.lacaeducation.academics.adapters.in.web;

import gn.lacaeducation.iam.TenantContext;
import gn.lacaeducation.academics.application.port.in.GererReferentielUseCase;
import gn.lacaeducation.academics.application.port.in.GererReferentielUseCase.CreateRoomCommand;
import gn.lacaeducation.academics.domain.Matiere;
import gn.lacaeducation.academics.domain.Niveau;
import gn.lacaeducation.academics.domain.Salle;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @Operation(summary = "Create a new school level (grade level)")
    @PostMapping("/levels")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    public ResponseEntity<Niveau> createLevel(@RequestParam String label, @RequestParam Integer sortOrder) {
        Niveau n = useCase.createLevel(TenantContext.get(), label, sortOrder);
        return ResponseEntity.created(URI.create("/api/academics/levels/" + n.getId())).body(n);
    }

    @Operation(summary = "List all school levels for the current tenant")
    @GetMapping("/levels")
    public List<Niveau> findLevels() { return useCase.findLevels(TenantContext.get()); }

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
}
