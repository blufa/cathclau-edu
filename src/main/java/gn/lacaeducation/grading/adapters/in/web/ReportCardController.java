package gn.lacaeducation.grading.adapters.in.web;
import gn.lacaeducation.grading.application.port.in.ReportCardUseCase;
import gn.lacaeducation.grading.domain.ReportCard;
import gn.lacaeducation.grading.domain.ReportCardLine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/grading/report-cards") @RequiredArgsConstructor
public class ReportCardController {
    private final ReportCardUseCase useCase;
    public record GenerateRequest(Long studentId, Long classId, Long periodId) {}
    @PostMapping public ResponseEntity<ReportCard> generate(@RequestBody GenerateRequest r) {
        return ResponseEntity.ok(useCase.generate(new ReportCardUseCase.GenerateReportCardCommand(r.studentId(), r.classId(), r.periodId())));
    }
    @PutMapping("/{id}/publish") public ResponseEntity<ReportCard> publish(@PathVariable Long id) { return ResponseEntity.ok(useCase.publish(id)); }
    @GetMapping("/{id}") public ResponseEntity<ReportCard> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/class/{classId}/period/{periodId}") public List<ReportCard> findByClass(@PathVariable Long classId, @PathVariable Long periodId) { return useCase.findByClass(classId, periodId); }
    @GetMapping("/student/{studentId}/period/{periodId}") public ResponseEntity<ReportCard> findByStudent(@PathVariable Long studentId, @PathVariable Long periodId) {
        return useCase.findByStudent(studentId, periodId).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/{id}/lines") public List<ReportCardLine> findLines(@PathVariable Long id) { return useCase.findLines(id); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { useCase.delete(id); return ResponseEntity.noContent().build(); }
}
