package gn.lacaeducation.grading.adapters.in.web;

import gn.lacaeducation.grading.application.port.in.GradeUseCase;
import gn.lacaeducation.grading.domain.Grade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    public record UpdateRequest(BigDecimal value) {}
    public record BulkEntry(Long studentId, BigDecimal value) {}
    public record BulkGradeRequest(List<BulkEntry> grades) {}

    // ── Single grade entry ────────────────────────────────────────────────────

    @Operation(summary = "Enter a grade for a student on an assessment")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','TEACHER')")
    @PostMapping("/grades")
    public ResponseEntity<Grade> enter(@RequestBody EnterRequest r) {
        return ResponseEntity.ok(useCase.enter(
                new GradeUseCase.EnterGradeCommand(r.assessmentId(), r.studentId(), r.value())));
    }

    @Operation(summary = "Update a student's grade value (only while period is open and grade not validated)")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','TEACHER')")
    @PutMapping("/grades/{id}")
    public ResponseEntity<Grade> update(@PathVariable Long id, @RequestBody UpdateRequest r) {
        return ResponseEntity.ok(useCase.update(id, r.value()));
    }

    @Operation(summary = "Validate (lock) a single grade so it can no longer be modified")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','TEACHER')")
    @PutMapping("/grades/{id}/validate")
    public ResponseEntity<Grade> validate(@PathVariable Long id) {
        return ResponseEntity.ok(useCase.validate(id));
    }

    @Operation(summary = "Unlock a validated grade so it can be corrected (authorized roles only)")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @PutMapping("/grades/{id}/unlock")
    public ResponseEntity<Grade> unlock(@PathVariable Long id) {
        return ResponseEntity.ok(useCase.unlock(id));
    }

    @Operation(summary = "Get a grade by ID")
    @GetMapping("/grades/{id}")
    public ResponseEntity<Grade> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "List all grades for an assessment")
    @GetMapping("/grades/assessment/{assessmentId}")
    public List<Grade> findByAssessment(@PathVariable Long assessmentId) {
        return useCase.findByAssessment(assessmentId);
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
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        useCase.delete(id);
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
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','TEACHER')")
    @PostMapping("/assessments/{assessmentId}/grades/bulk")
    public ResponseEntity<List<Grade>> enterBulk(@PathVariable Long assessmentId,
                                                  @RequestBody BulkGradeRequest request) {
        List<GradeUseCase.BulkGradeEntry> entries = request.grades().stream()
                .map(e -> new GradeUseCase.BulkGradeEntry(e.studentId(), e.value()))
                .toList();
        return ResponseEntity.ok(useCase.enterBulk(assessmentId, entries));
    }

    @Operation(summary = "Validate (lock) all grades for an assessment in one operation")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','TEACHER')")
    @PatchMapping("/assessments/{assessmentId}/grades/validate")
    public ResponseEntity<List<Grade>> validateAll(@PathVariable Long assessmentId) {
        return ResponseEntity.ok(useCase.validateAllForAssessment(assessmentId));
    }
}
