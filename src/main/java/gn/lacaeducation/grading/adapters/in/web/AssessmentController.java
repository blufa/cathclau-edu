package gn.lacaeducation.grading.adapters.in.web;
import gn.lacaeducation.grading.application.port.in.AssessmentUseCase;
import gn.lacaeducation.grading.domain.Assessment;
import gn.lacaeducation.grading.domain.AssessmentType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
@RestController @RequestMapping("/api/grading/assessments") @RequiredArgsConstructor
public class AssessmentController {
    private final AssessmentUseCase useCase;
    public record CreateRequest(Long tenantId, Long classId, Long subjectId, Long teacherId, Long periodId,
                                AssessmentType type, String label, LocalDate date, BigDecimal coefficient, BigDecimal maxScore) {}
    @PostMapping public ResponseEntity<Assessment> create(@RequestBody CreateRequest r) {
        return ResponseEntity.ok(useCase.create(new AssessmentUseCase.CreateAssessmentCommand(
                r.tenantId(), r.classId(), r.subjectId(), r.teacherId(), r.periodId(),
                r.type(), r.label(), r.date(), r.coefficient(), r.maxScore())));
    }
    @GetMapping("/{id}") public ResponseEntity<Assessment> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/class/{classId}") public List<Assessment> findByClass(@PathVariable Long classId) { return useCase.findByClass(classId); }
    @GetMapping("/period/{periodId}") public List<Assessment> findByPeriod(@PathVariable Long periodId) { return useCase.findByPeriod(periodId); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { useCase.delete(id); return ResponseEntity.noContent().build(); }
}
