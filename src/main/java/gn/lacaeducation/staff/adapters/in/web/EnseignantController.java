package gn.lacaeducation.staff.adapters.in.web;

import gn.lacaeducation.academics.ClassPublicInfo;
import gn.lacaeducation.academics.ClassSubjectPublicInfo;
import gn.lacaeducation.academics.ScheduleSlotPublicInfo;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.staff.application.port.in.TeacherUseCase;
import gn.lacaeducation.staff.application.port.in.TeacherUseCase.CreateTeacherCommand;
import gn.lacaeducation.staff.domain.Teacher;
import gn.lacaeducation.staff.domain.TeacherMonthlyWorkload;
import gn.lacaeducation.staff.domain.TeacherWorkload;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@Tag(name = "Teachers", description = "Gérer le personnel enseignant de l'établissement")
@RestController @RequestMapping("/api/v1/staff/teachers") @RequiredArgsConstructor
public class EnseignantController {

    private final TeacherUseCase teacherUseCase;

    @Operation(summary = "Créer un nouvel enseignant")
    @PreAuthorize("hasAuthority('enseignant:create')")
    @PostMapping
    public ResponseEntity<Teacher> create(@RequestBody CreateTeacherCommand cmd) {
        Teacher t = teacherUseCase.create(new CreateTeacherCommand(
                TenantContext.get(), cmd.userId(), cmd.staffNumber(), cmd.firstName(), cmd.lastName(),
                cmd.phone(), cmd.email(), cmd.specialization(), cmd.hireDate(),
                cmd.employmentStatus(), cmd.diploma(), cmd.remunerationType(),
                cmd.remunerationRate(), cmd.teachingAuthorizationNumber(), cmd.cnssNumber(),
                cmd.bankAccount(), cmd.idempotencyKey()));
        return ResponseEntity.created(URI.create("/api/staff/teachers/" + t.getId())).body(t);
    }

    @Operation(summary = "Consulter la fiche de l'enseignant connecté")
    @GetMapping("/me")
    public ResponseEntity<Teacher> me(@AuthenticationPrincipal Jwt jwt) {
        Long userId;
        try { userId = Long.parseLong(jwt.getSubject()); } catch (RuntimeException e) { userId = null; }
        return teacherUseCase.findByUserId(userId).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Mise à jour self-service du profil de l'enseignant connecté (téléphone, e-mail)")
    @PreAuthorize("hasRole('TEACHER')")
    @PutMapping("/me")
    public Teacher updateMyProfile(@AuthenticationPrincipal Jwt jwt,
                                   @RequestBody TeacherUseCase.UpdateOwnProfileCommand cmd) {
        return teacherUseCase.updateOwnProfile(currentUserId(jwt), cmd);
    }

    @Operation(summary = "Mise à jour de la photo de profil de l'enseignant connecté")
    @PreAuthorize("hasRole('TEACHER')")
    @PatchMapping("/me/photo")
    public Teacher updateMyPhoto(@AuthenticationPrincipal Jwt jwt, @RequestParam String photoUrl) {
        return teacherUseCase.updateOwnPhoto(currentUserId(jwt), photoUrl);
    }

    private Long currentUserId(Jwt jwt) {
        try { return Long.parseLong(jwt.getSubject()); } catch (RuntimeException e) { return null; }
    }

    @Operation(summary = "Définir la photo d'un enseignant (admin, badge)")
    @PreAuthorize("hasAuthority('enseignant:update')")
    @PatchMapping("/{id}/photo")
    public Teacher updatePhoto(@PathVariable Long id, @RequestParam String photoUrl) {
        return teacherUseCase.updatePhoto(id, photoUrl);
    }

    @Operation(summary = "Marquer (ou retirer) un enseignant comme pouvant couvrir plusieurs classes en même temps (ex. EPS)")
    @PreAuthorize("hasAuthority('enseignant:update')")
    @PatchMapping("/{id}/allows-concurrent-classes")
    public Teacher updateAllowsConcurrentClasses(@PathVariable Long id, @RequestParam boolean allowsConcurrentClasses) {
        return teacherUseCase.updateAllowsConcurrentClasses(id, allowsConcurrentClasses);
    }

    @Operation(summary = "Consulter un enseignant par son ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('enseignant:read')")
    public ResponseEntity<Teacher> findById(@PathVariable Long id) {
        return teacherUseCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Lister tous les enseignants de l'établissement")
    @GetMapping
    @PreAuthorize("hasAuthority('enseignant:read')")
    public List<Teacher> findAll() { return teacherUseCase.findByTenantId(TenantContext.get()); }

    @Operation(summary = "Modifier un enseignant existant")
    @PreAuthorize("hasAuthority('enseignant:update')")
    @PutMapping("/{id}")
    public Teacher update(@PathVariable Long id, @RequestBody TeacherUseCase.UpdateTeacherCommand cmd) {
        return teacherUseCase.update(id, cmd);
    }

    @Operation(summary = "Supprimer un enseignant (interdit s'il est lié à des évaluations — désactiver à la place)")
    @PreAuthorize("hasAuthority('enseignant:delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        teacherUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Désactiver un enseignant")
    @PreAuthorize("hasAuthority('enseignant:update')")
    @PatchMapping("/{id}/deactivate")
    public Teacher deactivate(@PathVariable Long id) { return teacherUseCase.setActive(id, false); }

    @Operation(summary = "Réactiver un enseignant")
    @PreAuthorize("hasAuthority('enseignant:update')")
    @PatchMapping("/{id}/activate")
    public Teacher activate(@PathVariable Long id) { return teacherUseCase.setActive(id, true); }

    @Operation(summary = "Classes dont l'enseignant est le professeur principal")
    @GetMapping("/{id}/classes")
    public List<ClassPublicInfo> classes(@PathVariable Long id) { return teacherUseCase.findClasses(id); }

    @Operation(summary = "Affectations de matières (ClasseMatiere) de l'enseignant")
    @GetMapping("/{id}/subjects")
    public List<ClassSubjectPublicInfo> subjects(@PathVariable Long id) { return teacherUseCase.findSubjects(id); }

    @Operation(summary = "Emploi du temps consolidé de l'enseignant")
    @GetMapping("/{id}/schedule")
    public List<ScheduleSlotPublicInfo> schedule(@PathVariable Long id) { return teacherUseCase.findSchedule(id); }

    @Operation(summary = "Charge horaire hebdomadaire de l'enseignant (total heures + répartition par jour)")
    @GetMapping("/{id}/workload")
    public TeacherWorkload workload(@PathVariable Long id) { return teacherUseCase.computeWorkload(id); }

    @Operation(summary = "Charge horaire mensuelle de l'enseignant, projetée depuis son emploi du temps hebdomadaire (§2)")
    @GetMapping("/{id}/workload/monthly")
    public TeacherMonthlyWorkload monthlyWorkload(@PathVariable Long id,
            @RequestParam int year, @RequestParam int month) {
        return teacherUseCase.computeMonthlyWorkload(id, year, month);
    }
}
