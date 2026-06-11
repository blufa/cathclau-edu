package gn.lacaeducation.grading.adapters.in.web;
import gn.lacaeducation.grading.application.port.in.ReportCardUseCase;
import gn.lacaeducation.grading.domain.ReportCard;
import gn.lacaeducation.grading.domain.ReportCardLine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@Tag(name = "Report Cards", description = "Generate, publish, and query student report cards per period")
@RestController @RequestMapping("/api/v1/grading/report-cards") @RequiredArgsConstructor
public class ReportCardController {
    private final ReportCardUseCase useCase;
    public record GenerateRequest(Long studentId, Long classId, Long periodId) {}
    @Operation(summary = "Generate a report card for a student for a given period")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
    @PostMapping public ResponseEntity<ReportCard> generate(@RequestBody GenerateRequest r) {
        return ResponseEntity.ok(useCase.generate(new ReportCardUseCase.GenerateReportCardCommand(r.studentId(), r.classId(), r.periodId())));
    }
    @Operation(summary = "Publish a report card to make it visible to parents and students")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @PutMapping("/{id}/publish") public ResponseEntity<ReportCard> publish(@PathVariable Long id) { return ResponseEntity.ok(useCase.publish(id)); }
    @Operation(summary = "Get a report card by ID")
    @GetMapping("/{id}") public ResponseEntity<ReportCard> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @Operation(summary = "List all report cards for a class in a given period")
    @GetMapping("/class/{classId}/period/{periodId}") public List<ReportCard> findByClass(@PathVariable Long classId, @PathVariable Long periodId) { return useCase.findByClass(classId, periodId); }
    @Operation(summary = "Get the report card for a student in a given period")
    @GetMapping("/student/{studentId}/period/{periodId}") public ResponseEntity<ReportCard> findByStudent(@PathVariable Long studentId, @PathVariable Long periodId) {
        return useCase.findByStudent(studentId, periodId).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @Operation(summary = "Get the subject-by-subject detail lines of a report card")
    @GetMapping("/{id}/lines") public List<ReportCardLine> findLines(@PathVariable Long id) { return useCase.findLines(id); }
    @Operation(summary = "Delete a report card")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { useCase.delete(id); return ResponseEntity.noContent().build(); }
}
