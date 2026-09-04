package gn.lacaeducation.students.adapters.in.web;

import gn.lacaeducation.students.application.port.in.EnrollmentUseCase;
import gn.lacaeducation.students.application.port.in.EnrollmentUseCase.EnrollCommand;
import gn.lacaeducation.students.domain.Enrollment;
import gn.lacaeducation.students.domain.EnrollmentStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Tag(name = "Enrollments", description = "Enrollments and re-enrollments with validation workflow")
@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentUseCase useCase;

    @Operation(summary = "Enroll a student in a class for an academic year")
    @PostMapping
    @PreAuthorize("hasAuthority('inscription:create')")
    public ResponseEntity<Enrollment> enroll(@Valid @RequestBody EnrollCommand cmd) {
        Enrollment e = useCase.enroll(cmd);
        return ResponseEntity.created(URI.create("/api/v1/enrollments/" + e.getId())).body(e);
    }

    @Operation(summary = "Re-enroll a student (next academic year)")
    @PostMapping("/re-enrollment")
    @PreAuthorize("hasAuthority('inscription:create')")
    public ResponseEntity<Enrollment> reenroll(@Valid @RequestBody EnrollCommand cmd) {
        Enrollment e = useCase.reenroll(cmd);
        return ResponseEntity.created(URI.create("/api/v1/enrollments/" + e.getId())).body(e);
    }

    @Operation(summary = "Modify a pending enrollment (class / academic year / type)")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('inscription:create')")
    public Enrollment update(@PathVariable Long id, @Valid @RequestBody EnrollCommand cmd) {
        return useCase.update(id, cmd);
    }

    @Operation(summary = "Search enrollments by student, class, academic year and/or status")
    @GetMapping
    @PreAuthorize("hasAuthority('inscription:read')")
    public List<Enrollment> search(@RequestParam(required = false) Long studentId,
                                   @RequestParam(required = false) Long classId,
                                   @RequestParam(required = false) Long academicYearId,
                                   @RequestParam(required = false) EnrollmentStatus status) {
        return useCase.search(studentId, classId, academicYearId, status);
    }

    @Operation(summary = "Get an enrollment by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('inscription:read')")
    public Enrollment findById(@PathVariable Long id) {
        return useCase.findById(id);
    }

    @Operation(summary = "Modifier le choix d'accès à la plateforme pour cet élève (opt-in du parent, modifiable en cours d'année)")
    @PatchMapping("/{id}/platform-access")
    @PreAuthorize("hasAuthority('inscription:create')")
    public Enrollment updatePlatformAccess(@PathVariable Long id, @RequestBody PlatformAccessRequest r) {
        return useCase.updatePlatformAccessOptIn(id, Boolean.TRUE.equals(r.optIn()));
    }

    public record PlatformAccessRequest(Boolean optIn) {}

    @Operation(summary = "Validate a pending enrollment (checks capacity and one-validated-per-year)")
    @PatchMapping("/{id}/validate")
    @PreAuthorize("hasAuthority('inscription:validate')")
    public Enrollment validate(@PathVariable Long id) {
        return useCase.validate(id);
    }

    @Operation(summary = "Cancel an enrollment")
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    public Enrollment cancel(@PathVariable Long id) {
        return useCase.cancel(id);
    }

    @Operation(summary = "Delete an enrollment")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        useCase.delete(id);
    }
}
