package gn.lacaeducation.grading.adapters.in.web;
import gn.lacaeducation.grading.application.port.in.AssessmentUseCase;
import gn.lacaeducation.grading.domain.Assessment;
import gn.lacaeducation.grading.domain.AssessmentType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
@Tag(name = "Assessments", description = "Manage assessments (quizzes, tests, exams) for classes and periods")
@RestController @RequestMapping("/api/v1/grading/assessments") @RequiredArgsConstructor
public class AssessmentController {
    private final AssessmentUseCase useCase;
    public record CreateRequest(Long tenantId, Long classId, Long subjectId, Long teacherId, Long periodId,
                                AssessmentType type, Long assessmentTypeId, String label, LocalDate date,
                                BigDecimal coefficient, BigDecimal maxScore) {}
    @Operation(summary = "Create a new assessment")
    @PreAuthorize("hasAuthority('evaluation:create')")
    @PostMapping public ResponseEntity<Assessment> create(@RequestBody CreateRequest r) {
        return ResponseEntity.ok(useCase.create(new AssessmentUseCase.CreateAssessmentCommand(
                r.tenantId(), r.classId(), r.subjectId(), r.teacherId(), r.periodId(),
                r.type(), r.assessmentTypeId(), r.label(), r.date(), r.coefficient(), r.maxScore())));
    }
    @Operation(summary = "Update an assessment (forbidden if the period is closed)")
    @PreAuthorize("hasAuthority('evaluation:update')")
    @PutMapping("/{id}") public ResponseEntity<Assessment> update(@PathVariable Long id, @RequestBody CreateRequest r) {
        return ResponseEntity.ok(useCase.update(id, new AssessmentUseCase.CreateAssessmentCommand(
                r.tenantId(), r.classId(), r.subjectId(), r.teacherId(), r.periodId(),
                r.type(), r.assessmentTypeId(), r.label(), r.date(), r.coefficient(), r.maxScore())));
    }
    @Operation(summary = "Get an assessment by ID")
    @GetMapping("/{id}") public ResponseEntity<Assessment> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @Operation(summary = "List all assessments for a class")
    @GetMapping("/class/{classId}") public List<Assessment> findByClass(@PathVariable Long classId) { return useCase.findByClass(classId); }
    @Operation(summary = "List all assessments for a period")
    @GetMapping("/period/{periodId}") public List<Assessment> findByPeriod(@PathVariable Long periodId) { return useCase.findByPeriod(periodId); }


    @Operation(summary = "Delete an assessment")
    @PreAuthorize("hasAuthority('evaluation:delete')")
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) {
        useCase.delete(id); return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Fiche de notes vierge (PDF) à imprimer et donner à l'enseignant : liste des élèves de la classe")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','RESPONSABLE_PEDAGOGIQUE','TEACHER','SECRETARY')")
    @GetMapping("/plan/pdf")
    public ResponseEntity<byte[]> planPdf(@RequestParam Long classId, @RequestParam Long subjectId,
                                          @RequestParam Long academicYearId) {
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"fiche-notes-classe-" + classId + "-matiere-" + subjectId + ".pdf\"")
                .body(useCase.planPdf(classId, subjectId, academicYearId));
    }

    @Operation(summary = "PDF téléchargeable d'une évaluation précise (notes déjà saisies incluses)")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','RESPONSABLE_PEDAGOGIQUE','TEACHER','SECRETARY')")
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> assessmentGradeSheetPdf(@PathVariable Long id) {
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"evaluation-" + id + ".pdf\"")
                .body(useCase.assessmentGradeSheetPdf(id));
    }
}
