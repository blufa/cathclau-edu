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
@RestController @RequestMapping("/api/v1/grading/grades") @RequiredArgsConstructor
public class GradeController {
    private final GradeUseCase useCase;
    public record EnterRequest(Long assessmentId, Long studentId, BigDecimal value) {}
    public record UpdateRequest(BigDecimal value) {}
    @Operation(summary = "Enter a grade for a student on an assessment")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','TEACHER')")
    @PostMapping public ResponseEntity<Grade> enter(@RequestBody EnterRequest r) {
        return ResponseEntity.ok(useCase.enter(new GradeUseCase.EnterGradeCommand(r.assessmentId(), r.studentId(), r.value())));
    }
    @Operation(summary = "Update a student's grade value")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','TEACHER')")
    @PutMapping("/{id}") public ResponseEntity<Grade> update(@PathVariable Long id, @RequestBody UpdateRequest r) {
        return ResponseEntity.ok(useCase.update(id, r.value()));
    }
    @Operation(summary = "Validate (lock) a grade so it can no longer be modified")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','TEACHER')")
    @PutMapping("/{id}/validate") public ResponseEntity<Grade> validate(@PathVariable Long id) { return ResponseEntity.ok(useCase.validate(id)); }
    @Operation(summary = "Get a grade by ID")
    @GetMapping("/{id}") public ResponseEntity<Grade> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @Operation(summary = "List all grades for an assessment")
    @GetMapping("/assessment/{assessmentId}") public List<Grade> findByAssessment(@PathVariable Long assessmentId) { return useCase.findByAssessment(assessmentId); }
    @Operation(summary = "List all grades for a student")
    @GetMapping("/student/{studentId}") public List<Grade> findByStudent(@PathVariable Long studentId) { return useCase.findByStudent(studentId); }
    @Operation(summary = "Delete a grade")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','TEACHER')")
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { useCase.delete(id); return ResponseEntity.noContent().build(); }
}
