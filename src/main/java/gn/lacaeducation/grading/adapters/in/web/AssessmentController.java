package gn.lacaeducation.grading.adapters.in.web;
import gn.lacaeducation.grading.application.port.in.AssessmentUseCase;
import gn.lacaeducation.grading.domain.Assessment;
import gn.lacaeducation.grading.domain.AssessmentType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
@Tag(name = "Assessments", description = "Gérer les évaluations (interrogations, devoirs, examens) des classes et périodes")
@RestController @RequestMapping("/api/v1/grading/assessments") @RequiredArgsConstructor
public class AssessmentController {
    private final AssessmentUseCase useCase;
    private final AssessmentOwnershipGuard ownershipGuard;
    private Long currentUserId(Jwt jwt) {
        if (jwt == null) return null;
        try { return Long.parseLong(jwt.getSubject()); } catch (RuntimeException e) { return null; }
    }
    public record CreateRequest(Long tenantId, Long classId, Long subjectId, Long teacherId, Long periodId,
                                AssessmentType type, Long assessmentTypeId, String label, LocalDate date,
                                BigDecimal coefficient, BigDecimal maxScore) {}
    @Operation(summary = "Créer une évaluation")
    @PreAuthorize("hasAuthority('evaluation:create')")
    @PostMapping public ResponseEntity<Assessment> create(@RequestBody CreateRequest r,
                                                           @AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        ownershipGuard.requireAssignedClassSubject(r.classId(), r.subjectId(), currentUserId(jwt), authentication);
        return ResponseEntity.ok(useCase.create(new AssessmentUseCase.CreateAssessmentCommand(
                r.tenantId(), r.classId(), r.subjectId(), r.teacherId(), r.periodId(),
                r.type(), r.assessmentTypeId(), r.label(), r.date(), r.coefficient(), r.maxScore())));
    }
    @Operation(summary = "Modifier une évaluation (interdit si la période est clôturée)")
    @PreAuthorize("hasAuthority('evaluation:update')")
    @PutMapping("/{id}") public ResponseEntity<Assessment> update(@PathVariable Long id, @RequestBody CreateRequest r,
                                                                   @AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        ownershipGuard.requireOwnerOrAdmin(id, currentUserId(jwt), authentication);
        ownershipGuard.requireAssignedClassSubject(r.classId(), r.subjectId(), currentUserId(jwt), authentication);
        return ResponseEntity.ok(useCase.update(id, new AssessmentUseCase.CreateAssessmentCommand(
                r.tenantId(), r.classId(), r.subjectId(), r.teacherId(), r.periodId(),
                r.type(), r.assessmentTypeId(), r.label(), r.date(), r.coefficient(), r.maxScore())));
    }
    @Operation(summary = "Consulter une évaluation par son identifiant")
    @GetMapping("/{id}") public ResponseEntity<Assessment> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @Operation(summary = "Lister toutes les évaluations d'une classe")
    @GetMapping("/class/{classId}") public List<Assessment> findByClass(@PathVariable Long classId) { return useCase.findByClass(classId); }
    @Operation(summary = "Lister toutes les évaluations d'une période")
    @GetMapping("/period/{periodId}") public List<Assessment> findByPeriod(@PathVariable Long periodId) { return useCase.findByPeriod(periodId); }


    @Operation(summary = "Supprimer une évaluation et ses notes déjà saisies le cas échéant "
            + "(réservé SCHOOL_ADMIN/DIRECTOR, même pour son créateur)")
    @PreAuthorize("hasAuthority('evaluation:delete')")
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id,
                                                                Authentication authentication) {
        ownershipGuard.requireAdmin(authentication);
        useCase.delete(id); return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Lister les groupes d'interrogations créées en double (même classe/matière/"
            + "période/date) avant l'introduction de la double saisie Orale/Écrite — candidats à une "
            + "fusion manuelle via /duplicates/merge, jamais modifié automatiquement")
    @PreAuthorize("hasAuthority('evaluation:read')")
    @GetMapping("/duplicates")
    public List<AssessmentUseCase.DuplicateGroup> findDuplicateQuizGroups() {
        return useCase.findDuplicateQuizGroups();
    }

    public record MergeDuplicatesRequest(Long keepAssessmentId, Long removeAssessmentId) {}

    @Operation(summary = "Fusionner deux interrogations en double : la note de keepAssessmentId devient "
            + "la case A, celle de removeAssessmentId la case B (moyenne recalculée), puis "
            + "removeAssessmentId et ses notes sont supprimées. Réservé SCHOOL_ADMIN/DIRECTOR.")
    @PreAuthorize("hasAuthority('evaluation:delete')")
    @PostMapping("/duplicates/merge")
    public ResponseEntity<Assessment> mergeDuplicates(@RequestBody MergeDuplicatesRequest r, Authentication authentication) {
        ownershipGuard.requireAdmin(authentication);
        return ResponseEntity.ok(useCase.mergeDuplicates(r.keepAssessmentId(), r.removeAssessmentId()));
    }

    @Operation(summary = "Fiche de notes vierge (PDF) à imprimer et donner à l'enseignant : liste des élèves de la classe")
    @PreAuthorize("hasAuthority('evaluation:read')")
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
    @PreAuthorize("hasAuthority('evaluation:read')")
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> assessmentGradeSheetPdf(@PathVariable Long id) {
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"evaluation-" + id + ".pdf\"")
                .body(useCase.assessmentGradeSheetPdf(id));
    }
}
