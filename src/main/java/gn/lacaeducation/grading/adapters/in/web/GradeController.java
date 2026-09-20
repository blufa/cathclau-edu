package gn.lacaeducation.grading.adapters.in.web;

import gn.lacaeducation.grading.application.port.in.GradeUseCase;
import gn.lacaeducation.grading.domain.Grade;
import gn.lacaeducation.grading.domain.GradeHistory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "Grades", description = "Saisir, modifier, valider et consulter les notes des élèves pour les évaluations")
@RestController
@RequestMapping("/api/v1/grading")
@RequiredArgsConstructor
public class GradeController {

    private final GradeUseCase useCase;

    public record EnterRequest(Long assessmentId, Long studentId, BigDecimal value) {}
    public record UpdateRequest(BigDecimal value, String reason) {}
    public record BulkEntry(Long studentId, BigDecimal value) {}
    public record BulkGradeRequest(List<BulkEntry> grades) {}

    // ── Single grade entry ────────────────────────────────────────────────────

    @Operation(summary = "Saisir la note d'un élève pour une évaluation")
    @PreAuthorize("hasAuthority('note:create')")
    @PostMapping("/grades")
    public ResponseEntity<Grade> enter(@RequestBody EnterRequest r, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(useCase.enter(
                new GradeUseCase.EnterGradeCommand(r.assessmentId(), r.studentId(), r.value()), currentUserId(jwt)));
    }

    @Operation(summary = "Modifier la note d'un élève (uniquement si la période est ouverte et la note non validée). "
            + "Un motif optionnel explique la correction et est conservé dans l'historique de la note.")
    @PreAuthorize("hasAuthority('note:update')")
    @PutMapping("/grades/{id}")
    public ResponseEntity<Grade> update(@PathVariable Long id, @RequestBody UpdateRequest r,
                                        @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(useCase.update(id, r.value(), currentUserId(jwt), r.reason()));
    }

    @Operation(summary = "Valider (verrouiller) une note pour qu'elle ne puisse plus être modifiée")
    @PreAuthorize("hasAuthority('note:validate')")
    @PutMapping("/grades/{id}/validate")
    public ResponseEntity<Grade> validate(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(useCase.validate(id, currentUserId(jwt)));
    }

    @Operation(summary = "Déverrouiller une note validée pour permettre sa correction (rôles autorisés uniquement)")
    @PreAuthorize("hasAuthority('note:unlock')")
    @PutMapping("/grades/{id}/unlock")
    public ResponseEntity<Grade> unlock(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(useCase.unlock(id, currentUserId(jwt)));
    }

    @Operation(summary = "Consulter une note par son identifiant")
    @GetMapping("/grades/{id}")
    public ResponseEntity<Grade> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Consulter l'historique complet des modifications d'une note (qui, quoi, quand et pourquoi)")
    @PreAuthorize("hasAuthority('note:read')")
    @GetMapping("/grades/{id}/history")
    public List<GradeHistory> history(@PathVariable Long id) {
        return useCase.findHistory(id);
    }

    @Operation(summary = "Lister toutes les notes d'une évaluation")
    @GetMapping("/grades/assessment/{assessmentId}")
    public List<Grade> findByAssessment(@PathVariable Long assessmentId) {
        return useCase.findByAssessment(assessmentId);
    }

    @Operation(summary = "Feuille de notes d'une évaluation : liste des élèves de la classe avec leur note saisie")
    @PreAuthorize("hasAuthority('note:read')")
    @GetMapping("/assessments/{assessmentId}/grade-sheet")
    public List<GradeUseCase.GradeSheetRow> gradeSheet(@PathVariable Long assessmentId) {
        return useCase.gradeSheet(assessmentId);
    }

    @Operation(summary = "Relevé de notes d'une matière (classe/période) : chaque évaluation + moyenne calculée")
    @PreAuthorize("hasAuthority('note:read')")
    @GetMapping("/classes/{classId}/subjects/{subjectId}/periods/{periodId}/releve")
    public GradeUseCase.SubjectGradeReport subjectReleve(@PathVariable Long classId, @PathVariable Long subjectId, @PathVariable Long periodId) {
        return useCase.subjectGradeReport(classId, subjectId, periodId);
    }

    @Operation(summary = "Relevé de notes d'une matière (PDF) — ?month=1-12 limite au relevé de ce mois")
    @PreAuthorize("hasAuthority('note:read')")
    @GetMapping("/classes/{classId}/subjects/{subjectId}/periods/{periodId}/releve/download")
    public org.springframework.http.ResponseEntity<byte[]> subjectReleveDownload(@PathVariable Long classId, @PathVariable Long subjectId,
            @PathVariable Long periodId, @RequestParam(required = false) Integer month) {
        String suffix = month != null ? "-mois-" + month : "";
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"releve-classe-" + classId + "-matiere-" + subjectId + "-periode-" + periodId + suffix + ".pdf\"")
                .body(useCase.subjectGradeReportPdf(classId, subjectId, periodId, month));
    }

    @Operation(summary = "Lister toutes les notes d'un élève (filtrage optionnel par periodId)")
    @GetMapping("/grades/student/{studentId}")
    public List<Grade> findByStudent(@PathVariable Long studentId,
                                     @RequestParam(required = false) Long periodId) {
        if (periodId != null) {
            return useCase.findByStudentAndPeriod(studentId, periodId);
        }
        return useCase.findByStudent(studentId);
    }

    @Operation(summary = "Supprimer une note")
    @PreAuthorize("hasAuthority('note:update')")
    @DeleteMapping("/grades/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        useCase.delete(id, currentUserId(jwt));
        return ResponseEntity.noContent().build();
    }

    // ── Student grades endpoint at /api/v1/students/{id}/grades ──────────────

    @Operation(summary = "Lister toutes les notes d'un élève (filtrage optionnel par periodId et/ou subjectId)")
    @GetMapping("/students/{studentId}/grades")
    public List<Grade> findGradesForStudent(@PathVariable Long studentId,
                                            @RequestParam(required = false) Long periodId) {
        if (periodId != null) {
            return useCase.findByStudentAndPeriod(studentId, periodId);
        }
        return useCase.findByStudent(studentId);
    }

    // ── Bulk grade endpoints on assessments ───────────────────────────────────

    @Operation(summary = "Saisir en masse les notes d'une classe entière pour une évaluation. "
            + "Attend une liste de paires {studentId, value}.")
    @PreAuthorize("hasAuthority('note:create')")
    @PostMapping("/assessments/{assessmentId}/grades/bulk")
    public ResponseEntity<List<Grade>> enterBulk(@PathVariable Long assessmentId,
                                                  @RequestBody BulkGradeRequest request,
                                                  @AuthenticationPrincipal Jwt jwt,
                                                  org.springframework.security.core.Authentication authentication) {
        List<GradeUseCase.BulkGradeEntry> entries = request.grades().stream()
                .map(e -> new GradeUseCase.BulkGradeEntry(e.studentId(), e.value()))
                .toList();
        // Only ADMIN/DIRECTOR may overwrite an already-entered grade; teachers can only enter new ones.
        boolean allowOverwrite = authentication != null && authentication.getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_SCHOOL_ADMIN") || a.equals("ROLE_DIRECTOR"));
        return ResponseEntity.ok(useCase.enterBulk(assessmentId, entries, allowOverwrite, currentUserId(jwt)));
    }

    @Operation(summary = "Valider (verrouiller) toutes les notes d'une évaluation en une seule opération. "
            + "Recalcule les moyennes de classe, les rangs et les bulletins de la période de l'évaluation.")
    @PreAuthorize("hasAuthority('note:validate')")
    @PatchMapping("/assessments/{assessmentId}/grades/validate")
    public ResponseEntity<List<Grade>> validateAll(@PathVariable Long assessmentId,
                                                    @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(useCase.validateAllForAssessment(assessmentId, currentUserId(jwt)));
    }

    private Long currentUserId(Jwt jwt) {
        if (jwt == null) return null;
        try { return Long.parseLong(jwt.getSubject()); } catch (RuntimeException e) { return null; }
    }
}
