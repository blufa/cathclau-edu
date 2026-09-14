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

@Tag(name = "Grades", description = "Enter, update, validate, and query student grades for assessments")
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

    @Operation(summary = "Enter a grade for a student on an assessment")
    @PreAuthorize("hasAuthority('note:create')")
    @PostMapping("/grades")
    public ResponseEntity<Grade> enter(@RequestBody EnterRequest r, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(useCase.enter(
                new GradeUseCase.EnterGradeCommand(r.assessmentId(), r.studentId(), r.value()), currentUserId(jwt)));
    }

    @Operation(summary = "Update a student's grade value (only while period is open and grade not validated). "
            + "An optional reason explains the correction and is kept in the grade's history.")
    @PreAuthorize("hasAuthority('note:update')")
    @PutMapping("/grades/{id}")
    public ResponseEntity<Grade> update(@PathVariable Long id, @RequestBody UpdateRequest r,
                                        @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(useCase.update(id, r.value(), currentUserId(jwt), r.reason()));
    }

    @Operation(summary = "Validate (lock) a single grade so it can no longer be modified")
    @PreAuthorize("hasAuthority('note:validate')")
    @PutMapping("/grades/{id}/validate")
    public ResponseEntity<Grade> validate(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(useCase.validate(id, currentUserId(jwt)));
    }

    @Operation(summary = "Unlock a validated grade so it can be corrected (authorized roles only)")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @PutMapping("/grades/{id}/unlock")
    public ResponseEntity<Grade> unlock(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(useCase.unlock(id, currentUserId(jwt)));
    }

    @Operation(summary = "Get a grade by ID")
    @GetMapping("/grades/{id}")
    public ResponseEntity<Grade> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get the full modification history of a grade (who changed what, when and why)")
    @PreAuthorize("hasAuthority('note:read')")
    @GetMapping("/grades/{id}/history")
    public List<GradeHistory> history(@PathVariable Long id) {
        return useCase.findHistory(id);
    }

    @Operation(summary = "List all grades for an assessment")
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
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','RESPONSABLE_PEDAGOGIQUE','TEACHER','SECRETARY')")
    @GetMapping("/classes/{classId}/subjects/{subjectId}/periods/{periodId}/releve")
    public GradeUseCase.SubjectGradeReport subjectReleve(@PathVariable Long classId, @PathVariable Long subjectId, @PathVariable Long periodId) {
        return useCase.subjectGradeReport(classId, subjectId, periodId);
    }

    @Operation(summary = "Relevé de notes d'une matière (PDF) — ?month=1-12 limite au relevé de ce mois")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','RESPONSABLE_PEDAGOGIQUE','TEACHER','SECRETARY')")
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

    @Operation(summary = "List all grades for a student (optionally filtered by periodId)")
    @GetMapping("/grades/student/{studentId}")
    public List<Grade> findByStudent(@PathVariable Long studentId,
                                     @RequestParam(required = false) Long periodId) {
        if (periodId != null) {
            return useCase.findByStudentAndPeriod(studentId, periodId);
        }
        return useCase.findByStudent(studentId);
    }

    @Operation(summary = "Delete a grade")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','TEACHER')")
    @DeleteMapping("/grades/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        useCase.delete(id, currentUserId(jwt));
        return ResponseEntity.noContent().build();
    }

    // ── Student grades endpoint at /api/v1/students/{id}/grades ──────────────

    @Operation(summary = "List all grades for a student (optionally filtered by periodId and/or subjectId)")
    @GetMapping("/students/{studentId}/grades")
    public List<Grade> findGradesForStudent(@PathVariable Long studentId,
                                            @RequestParam(required = false) Long periodId) {
        if (periodId != null) {
            return useCase.findByStudentAndPeriod(studentId, periodId);
        }
        return useCase.findByStudent(studentId);
    }

    // ── Bulk grade endpoints on assessments ───────────────────────────────────

    @Operation(summary = "Bulk enter grades for a whole class on an assessment. "
            + "Expects a list of {studentId, value} pairs.")
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

    @Operation(summary = "Validate (lock) all grades for an assessment in one operation. "
            + "Recomputes the class averages, ranks and report cards for the assessment's period.")
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
