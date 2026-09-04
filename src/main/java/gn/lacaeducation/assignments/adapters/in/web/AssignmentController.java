package gn.lacaeducation.assignments.adapters.in.web;

import gn.lacaeducation.assignments.application.service.AssignmentService;
import gn.lacaeducation.assignments.application.service.AssignmentService.CreateAssignmentCommand;
import gn.lacaeducation.assignments.domain.Assignment;
import gn.lacaeducation.assignments.domain.AssignmentStatus;
import gn.lacaeducation.assignments.domain.AssignmentSubmission;
import gn.lacaeducation.common.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "Assignments", description = "Devoirs à rendre et rendus des élèves (inspiré OpenEduCat)")
@RestController
@RequestMapping("/api/v1/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService service;

    public record AssignmentRequest(Long classId, Long subjectId, Long teacherId, String title,
                                    String instructions, String attachmentUrl, LocalDate dueDate, BigDecimal maxScore) {}
    public record SubmitRequest(Long studentId, String fileUrl) {}
    public record GradeRequest(BigDecimal grade, String feedback) {}

    @Operation(summary = "Lister les devoirs du tenant (filtres classe / statut)")
    @GetMapping
    public List<Assignment> list(@RequestParam(required = false) Long classId,
                                 @RequestParam(required = false) AssignmentStatus status) {
        return service.find(TenantContext.get(), classId, status);
    }

    @Operation(summary = "Détail d'un devoir")
    @GetMapping("/{id}")
    public Assignment findById(@PathVariable Long id) { return service.findById(id); }

    @Operation(summary = "Créer un devoir")
    @PreAuthorize("hasAuthority('devoir:manage')")
    @PostMapping
    public Assignment create(@RequestBody AssignmentRequest r) {
        return service.create(toCommand(r));
    }

    @Operation(summary = "Modifier un devoir")
    @PreAuthorize("hasAuthority('devoir:manage')")
    @PutMapping("/{id}")
    public Assignment update(@PathVariable Long id, @RequestBody AssignmentRequest r) {
        return service.update(id, toCommand(r));
    }

    @Operation(summary = "Clôturer un devoir (plus de rendu accepté)")
    @PreAuthorize("hasAuthority('devoir:manage')")
    @PatchMapping("/{id}/close")
    public Assignment close(@PathVariable Long id) { return service.close(id); }

    @Operation(summary = "Rouvrir un devoir")
    @PreAuthorize("hasAuthority('devoir:manage')")
    @PatchMapping("/{id}/reopen")
    public Assignment reopen(@PathVariable Long id) { return service.reopen(id); }

    @Operation(summary = "Supprimer un devoir")
    @PreAuthorize("hasAuthority('devoir:manage')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.noContent().build(); }

    // ── Rendus ────────────────────────────────────────────────────────────────

    @Operation(summary = "Lister les rendus d'un devoir")
    @GetMapping("/{id}/submissions")
    public List<AssignmentSubmission> submissions(@PathVariable Long id) { return service.submissions(id); }

    @Operation(summary = "Enregistrer / mettre à jour le rendu d'un élève")
    @PostMapping("/{id}/submissions")
    public AssignmentSubmission submit(@PathVariable Long id, @RequestBody SubmitRequest r) {
        return service.submit(TenantContext.get(), id, r.studentId(), r.fileUrl());
    }

    @Operation(summary = "Noter le rendu d'un élève")
    @PreAuthorize("hasAuthority('devoir:manage')")
    @PatchMapping("/submissions/{submissionId}/grade")
    public AssignmentSubmission grade(@PathVariable Long submissionId, @RequestBody GradeRequest r) {
        return service.grade(submissionId, r.grade(), r.feedback());
    }

    private CreateAssignmentCommand toCommand(AssignmentRequest r) {
        return new CreateAssignmentCommand(TenantContext.get(), r.classId(), r.subjectId(), r.teacherId(),
                r.title(), r.instructions(), r.attachmentUrl(), r.dueDate(), r.maxScore());
    }
}
