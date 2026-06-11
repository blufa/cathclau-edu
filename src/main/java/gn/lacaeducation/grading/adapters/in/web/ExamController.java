package gn.lacaeducation.grading.adapters.in.web;
import gn.lacaeducation.grading.application.port.in.ExamUseCase;
import gn.lacaeducation.grading.domain.Exam;
import gn.lacaeducation.grading.domain.ExamType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
@RestController @RequestMapping("/api/grading/exams") @RequiredArgsConstructor
public class ExamController {
    private final ExamUseCase useCase;
    public record CreateRequest(Long tenantId, Long classId, Long periodId, ExamType type, String label, LocalDate scheduledDate) {}
    @PostMapping public ResponseEntity<Exam> create(@RequestBody CreateRequest r) {
        return ResponseEntity.ok(useCase.create(new ExamUseCase.CreateExamCommand(r.tenantId(), r.classId(), r.periodId(), r.type(), r.label(), r.scheduledDate())));
    }
    @GetMapping("/{id}") public ResponseEntity<Exam> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/class/{classId}") public List<Exam> findByClass(@PathVariable Long classId) { return useCase.findByClass(classId); }
    @GetMapping("/period/{periodId}") public List<Exam> findByPeriod(@PathVariable Long periodId) { return useCase.findByPeriod(periodId); }
    @PutMapping("/{id}/start") public ResponseEntity<Exam> start(@PathVariable Long id) { return ResponseEntity.ok(useCase.start(id)); }
    @PutMapping("/{id}/complete") public ResponseEntity<Exam> complete(@PathVariable Long id) { return ResponseEntity.ok(useCase.complete(id)); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { useCase.delete(id); return ResponseEntity.noContent().build(); }
}
