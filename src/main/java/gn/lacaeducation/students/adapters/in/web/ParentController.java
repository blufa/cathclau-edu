package gn.lacaeducation.students.adapters.in.web;

import gn.lacaeducation.common.PageResponse;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.students.application.port.in.ParentUseCase;
import gn.lacaeducation.students.domain.Parent;
import gn.lacaeducation.students.domain.Relationship;
import gn.lacaeducation.students.domain.StudentParent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Tag(name = "Parents", description = "Parent records and student-parent relationships")
@RestController
@RequestMapping("/api/v1/parents")
@RequiredArgsConstructor
public class ParentController {

    private final ParentUseCase parentUseCase;

    @Operation(summary = "Register a new parent")
    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','SECRETARY','DIRECTOR')")
    public ResponseEntity<Parent> create(@Valid @RequestBody ParentUseCase.CreateParentCommand cmd) {
        Parent p = parentUseCase.create(new ParentUseCase.CreateParentCommand(
                TenantContext.get(), cmd.userId(), cmd.firstName(), cmd.lastName(),
                cmd.phone(), cmd.email(), cmd.occupation(), cmd.address(), cmd.idempotencyKey()));
        return ResponseEntity.created(URI.create("/api/v1/parents/" + p.getId())).body(p);
    }

    @Operation(summary = "List all parents in current tenant (paginated)")
    @GetMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
    public PageResponse<Parent> findAll(@PageableDefault(size = 20) Pageable pageable) {
        List<Parent> list = parentUseCase.findByTenantId(TenantContext.get());
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());
        return PageResponse.of(new PageImpl<>(list.subList(start, end), pageable, list.size()));
    }

    @Operation(summary = "Get the parent record of the currently authenticated user (parent portal)")
    @GetMapping("/me")
    public ResponseEntity<Parent> me(@AuthenticationPrincipal Jwt jwt) {
        return parentUseCase.findByUserId(currentUserId(jwt)).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get the children (students) of the currently authenticated parent")
    @GetMapping("/me/children")
    public List<gn.lacaeducation.students.domain.Student> myChildren(@AuthenticationPrincipal Jwt jwt) {
        return parentUseCase.findByUserId(currentUserId(jwt))
                .map(p -> parentUseCase.findStudentsByParent(p.getId()))
                .orElseGet(List::of);
    }

    @Operation(summary = "Mise à jour self-service du profil du parent connecté (tél, email, adresse, profession)")
    @PreAuthorize("hasRole('PARENT')")
    @PutMapping("/me")
    public Parent updateMyProfile(@AuthenticationPrincipal Jwt jwt,
                                  @Valid @RequestBody ParentUseCase.UpdateProfileCommand cmd) {
        return parentUseCase.updateOwnProfile(currentUserId(jwt), cmd);
    }

    @Operation(summary = "Mise à jour de la photo de profil du parent connecté")
    @PreAuthorize("hasRole('PARENT')")
    @PatchMapping("/me/photo")
    public Parent updateMyPhoto(@AuthenticationPrincipal Jwt jwt, @RequestParam String photoUrl) {
        return parentUseCase.updateOwnPhoto(currentUserId(jwt), photoUrl);
    }

    @Operation(summary = "Get parent by ID")
    @GetMapping("/{id}")
    public ResponseEntity<Parent> findById(@PathVariable Long id) {
        return parentUseCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    private Long currentUserId(Jwt jwt) {
        try { return Long.parseLong(jwt.getSubject()); } catch (RuntimeException e) { return null; }
    }

    @Operation(summary = "Update parent information")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','SECRETARY','DIRECTOR')")
    public Parent update(@PathVariable Long id, @Valid @RequestBody ParentUseCase.UpdateParentCommand cmd) {
        return parentUseCase.update(id, cmd);
    }

    @Operation(summary = "Delete a parent")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { parentUseCase.delete(id); }

    @Operation(summary = "Link a parent to a student with relationship type")
    @PostMapping("/{parentId}/students/{studentId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','SECRETARY','DIRECTOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public StudentParent linkStudent(@PathVariable Long parentId, @PathVariable Long studentId,
                                      @RequestParam Relationship relationship) {
        return parentUseCase.linkStudent(studentId, parentId, relationship);
    }

    @Operation(summary = "Désigner le responsable principal (« responsable 1 ») de l'élève")
    @PatchMapping("/{parentId}/students/{studentId}/primary")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','SECRETARY','DIRECTOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setPrimary(@PathVariable Long parentId, @PathVariable Long studentId) {
        parentUseCase.setPrimaryGuardian(studentId, parentId);
    }

    @Operation(summary = "Remove a student-parent link")
    @DeleteMapping("/links/{linkId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlinkStudent(@PathVariable Long linkId) { parentUseCase.unlinkStudent(linkId); }

    @Operation(summary = "Get parents of a student")
    @GetMapping("/by-student/{studentId}")
    public List<Parent> findByStudent(@PathVariable Long studentId) { return parentUseCase.findByStudent(studentId); }

    @Operation(summary = "Get children (students) of a parent")
    @GetMapping("/{id}/students")
    public List<gn.lacaeducation.students.domain.Student> findChildren(@PathVariable Long id) {
        return parentUseCase.findStudentsByParent(id);
    }
}
