package gn.lacaeducation.grading.adapters.in.web;
import gn.lacaeducation.grading.application.port.in.GradeUseCase;
import gn.lacaeducation.grading.domain.Grade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
@RestController @RequestMapping("/api/grading/grades") @RequiredArgsConstructor
public class GradeController {
    private final GradeUseCase useCase;
    public record EnterRequest(Long assessmentId, Long studentId, BigDecimal value) {}
    public record UpdateRequest(BigDecimal value) {}
    @PostMapping public ResponseEntity<Grade> enter(@RequestBody EnterRequest r) {
        return ResponseEntity.ok(useCase.enter(new GradeUseCase.EnterGradeCommand(r.assessmentId(), r.studentId(), r.value())));
    }
    @PutMapping("/{id}") public ResponseEntity<Grade> update(@PathVariable Long id, @RequestBody UpdateRequest r) {
        return ResponseEntity.ok(useCase.update(id, r.value()));
    }
    @PutMapping("/{id}/validate") public ResponseEntity<Grade> validate(@PathVariable Long id) { return ResponseEntity.ok(useCase.validate(id)); }
    @GetMapping("/{id}") public ResponseEntity<Grade> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/assessment/{assessmentId}") public List<Grade> findByAssessment(@PathVariable Long assessmentId) { return useCase.findByAssessment(assessmentId); }
    @GetMapping("/student/{studentId}") public List<Grade> findByStudent(@PathVariable Long studentId) { return useCase.findByStudent(studentId); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { useCase.delete(id); return ResponseEntity.noContent().build(); }
}
