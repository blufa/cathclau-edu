package gn.lacaeducation.grading.adapters.in.web;
import gn.lacaeducation.grading.application.port.in.ExamUseCase;
import gn.lacaeducation.grading.domain.Exam;
import gn.lacaeducation.grading.domain.ExamType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
@Tag(name = "Exams", description = "Schedule and manage formal examinations (mock, semester, annual, national)")
@RestController @RequestMapping("/api/v1/grading/exams") @RequiredArgsConstructor
public class ExamController {
    private final ExamUseCase useCase;
    public record CreateRequest(Long tenantId, Long classId, Long periodId, ExamType type, String label, LocalDate scheduledDate) {}
    @Operation(summary = "Schedule a new exam")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
    @PostMapping public ResponseEntity<Exam> create(@RequestBody CreateRequest r) {
        return ResponseEntity.ok(useCase.create(new ExamUseCase.CreateExamCommand(r.tenantId(), r.classId(), r.periodId(), r.type(), r.label(), r.scheduledDate())));
    }
    @Operation(summary = "Get an exam by ID")
    @GetMapping("/{id}") public ResponseEntity<Exam> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @Operation(summary = "List all exams for a class")
    @GetMapping("/class/{classId}") public List<Exam> findByClass(@PathVariable Long classId) { return useCase.findByClass(classId); }
    @Operation(summary = "List all exams for a period")
    @GetMapping("/period/{periodId}") public List<Exam> findByPeriod(@PathVariable Long periodId) { return useCase.findByPeriod(periodId); }
    @Operation(summary = "Mark an exam as started")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
    @PutMapping("/{id}/start") public ResponseEntity<Exam> start(@PathVariable Long id) { return ResponseEntity.ok(useCase.start(id)); }
    @Operation(summary = "Mark an exam as completed")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
    @PutMapping("/{id}/complete") public ResponseEntity<Exam> complete(@PathVariable Long id) { return ResponseEntity.ok(useCase.complete(id)); }
    @Operation(summary = "Delete an exam")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { useCase.delete(id); return ResponseEntity.noContent().build(); }
}
