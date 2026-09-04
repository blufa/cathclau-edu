package gn.lacaeducation.lessons.adapters.in.web;

import gn.lacaeducation.common.PortalAccess;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.lessons.application.service.LessonService;
import gn.lacaeducation.lessons.application.service.LessonService.CreateLessonCommand;
import gn.lacaeducation.lessons.domain.LessonEntry;
import gn.lacaeducation.lessons.domain.LessonStatus;
import gn.lacaeducation.students.ParentAccessProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Lessons", description = "Cahier de textes / progression pédagogique par classe & matière")
@RestController
@RequestMapping("/api/v1/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService service;
    private final ParentAccessProvider parentAccessProvider;

    public record LessonRequest(Long classId, Long subjectId, Long teacherId, LocalDate lessonDate,
                                String title, String content, String homework, Integer durationMinutes,
                                LessonStatus status) {}

    /** Durée réellement effectuée (optionnelle) — corrige la durée planifiée au moment de la réalisation. */
    public record MarkDoneRequest(Integer durationMinutes) {}

    @Operation(summary = "Lister les séances (filtres classe / matière)")
    @GetMapping
    public List<LessonEntry> list(@RequestParam(required = false) Long classId,
                                  @RequestParam(required = false) Long subjectId) {
        return service.find(TenantContext.get(), classId, subjectId);
    }

    @Operation(summary = "Cahier de textes de la classe d'un élève (portail élève / parent)")
    @GetMapping("/student/{studentId}")
    public List<LessonEntry> byStudent(@PathVariable Long studentId, @AuthenticationPrincipal Jwt jwt) {
        if (jwt != null) {
            Long userId;
            try { userId = Long.parseLong(jwt.getSubject()); } catch (RuntimeException e) { userId = null; }
            PortalAccess.assertParentCanAccessStudent(jwt.getClaimAsStringList("roles"), userId, studentId,
                    parentAccessProvider::isParentOfStudent);
        }
        return service.findByStudent(studentId);
    }

    @Operation(summary = "Détail d'une séance")
    @GetMapping("/{id}")
    public LessonEntry findById(@PathVariable Long id) { return service.findById(id); }

    @Operation(summary = "Créer une séance (planifiée ou réalisée)")
    @PreAuthorize("hasAuthority('cahier:manage')")
    @PostMapping
    public LessonEntry create(@RequestBody LessonRequest r) { return service.create(toCommand(r)); }

    @Operation(summary = "Modifier une séance")
    @PreAuthorize("hasAuthority('cahier:manage')")
    @PutMapping("/{id}")
    public LessonEntry update(@PathVariable Long id, @RequestBody LessonRequest r) { return service.update(id, toCommand(r)); }

    @Operation(summary = "Marquer la séance comme réalisée (durée réalisée optionnelle, sinon la durée planifiée est conservée)")
    @PreAuthorize("hasAuthority('cahier:manage')")
    @PatchMapping("/{id}/done")
    public LessonEntry markDone(@PathVariable Long id, @RequestBody(required = false) MarkDoneRequest r) {
        return service.markDone(id, r != null ? r.durationMinutes() : null);
    }

    @Operation(summary = "Remettre la séance en planifié")
    @PreAuthorize("hasAuthority('cahier:manage')")
    @PatchMapping("/{id}/plan")
    public LessonEntry markPlanned(@PathVariable Long id) { return service.markPlanned(id); }

    @Operation(summary = "Supprimer une séance")
    @PreAuthorize("hasAuthority('cahier:manage')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.noContent().build(); }

    private CreateLessonCommand toCommand(LessonRequest r) {
        return new CreateLessonCommand(TenantContext.get(), r.classId(), r.subjectId(), r.teacherId(),
                r.lessonDate(), r.title(), r.content(), r.homework(), r.durationMinutes(), r.status());
    }
}
