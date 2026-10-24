package gn.lacaeducation.grading.adapters.in.web;
import gn.lacaeducation.grading.application.port.in.ExamUseCase;
import gn.lacaeducation.grading.domain.Assessment;
import gn.lacaeducation.grading.domain.Exam;
import gn.lacaeducation.grading.domain.ExamSupervision;
import gn.lacaeducation.grading.domain.ExamType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
@Tag(name = "Exams", description = "Planifier et gérer les examens officiels (blanc, semestriel, annuel, national)")
@RestController @RequestMapping("/api/v1/grading/exams") @RequiredArgsConstructor
public class ExamController {
    private final ExamUseCase useCase;
    public record CreateRequest(Long tenantId, Long classId, Long periodId, ExamType type, String label, LocalDate scheduledDate) {}
    @Operation(summary = "Planifier un examen")
    @PreAuthorize("hasAuthority('examen:manage')")
    @PostMapping public ResponseEntity<Exam> create(@RequestBody CreateRequest r) {
        return ResponseEntity.ok(useCase.create(new ExamUseCase.CreateExamCommand(r.tenantId(), r.classId(), r.periodId(), r.type(), r.label(), r.scheduledDate())));
    }
    @Operation(summary = "Consulter un examen par son identifiant")
    @GetMapping("/{id}") public ResponseEntity<Exam> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @Operation(summary = "Lister tous les examens d'une classe")
    @GetMapping("/class/{classId}") public List<Exam> findByClass(@PathVariable Long classId) { return useCase.findByClass(classId); }
    @Operation(summary = "Lister tous les examens d'une période")
    @GetMapping("/period/{periodId}") public List<Exam> findByPeriod(@PathVariable Long periodId) { return useCase.findByPeriod(periodId); }
    @Operation(summary = "Marquer un examen comme démarré")
    @PreAuthorize("hasAuthority('examen:manage')")
    @PutMapping("/{id}/start") public ResponseEntity<Exam> start(@PathVariable Long id) { return ResponseEntity.ok(useCase.start(id)); }
    @Operation(summary = "Marquer un examen comme terminé")
    @PreAuthorize("hasAuthority('examen:manage')")
    @PutMapping("/{id}/complete") public ResponseEntity<Exam> complete(@PathVariable Long id) { return ResponseEntity.ok(useCase.complete(id)); }
    @Operation(summary = "Obtenir l'URL du PDF de convocation à l'examen (déclenche la génération asynchrone si absent)")
    @GetMapping("/{id}/convocation") public ResponseEntity<Map<String, String>> convocation(@PathVariable Long id) {
        return useCase.getConvocation(id)
                .map(url -> ResponseEntity.ok(Map.of("url", url)))
                .orElseGet(() -> { useCase.generateConvocationAsync(id); return ResponseEntity.accepted().body(Map.of("status", "GENERATING")); });
    }
    @Operation(summary = "Flux des octets du PDF de convocation (aperçu direct) — indépendant de Cloudinary")
    @GetMapping("/{id}/convocation/download") public ResponseEntity<byte[]> convocationDownload(@PathVariable Long id) {
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"convocation-" + id + ".pdf\"")
                .body(useCase.convocationBytes(id));
    }
    @Operation(summary = "Supprimer un examen")
    @PreAuthorize("hasAuthority('examen:manage')")
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { useCase.delete(id); return ResponseEntity.noContent().build(); }

    // ── Surveillance (composition : surveillants par salle) ──────────────────────
    public record SupervisionRequest(Long roomId, Long teacherId) {}

    @Operation(summary = "Affecter un enseignant surveillant à une salle pour l'examen (conflit refusé en 422)")
    @PreAuthorize("hasAuthority('examen:manage')")
    @PostMapping("/{id}/supervisions")
    public ResponseEntity<ExamSupervision> assignSupervisor(@PathVariable Long id, @RequestBody SupervisionRequest r) {
        return ResponseEntity.ok(useCase.assignSupervisor(id, r.roomId(), r.teacherId()));
    }

    @Operation(summary = "Lister les surveillants d'un examen")
    @GetMapping("/{id}/supervisions")
    public List<ExamSupervision> supervisions(@PathVariable Long id) { return useCase.supervisions(id); }

    @Operation(summary = "Retirer un surveillant")
    @PreAuthorize("hasAuthority('examen:manage')")
    @DeleteMapping("/{id}/supervisions/{supervisionId}")
    public ResponseEntity<Void> removeSupervision(@PathVariable Long id, @PathVariable Long supervisionId) {
        useCase.removeSupervision(supervisionId);
        return ResponseEntity.noContent().build();
    }

    // ── Saisie des notes par matière (la composition se note depuis cet onglet, jamais via "Nouvelle
    // évaluation" — §retour utilisateur) ──────────────────────────────────────────
    @Operation(summary = "Lister les évaluations (une par matière déjà notée) de cette composition")
    @PreAuthorize("hasAuthority('evaluation:create')")
    @GetMapping("/{id}/assessments")
    public List<Assessment> assessmentsByExam(@PathVariable Long id) { return useCase.assessmentsByExam(id); }

    @Operation(summary = "Retrouve (ou crée) l'évaluation de cette matière pour cette composition, prête pour la saisie des notes")
    @PreAuthorize("hasAuthority('evaluation:create')")
    @PostMapping("/{id}/subjects/{subjectId}/grade-entry")
    public ResponseEntity<Assessment> gradeEntryAssessment(@PathVariable Long id, @PathVariable Long subjectId) {
        return ResponseEntity.ok(useCase.gradeEntryAssessment(id, subjectId));
    }
}
