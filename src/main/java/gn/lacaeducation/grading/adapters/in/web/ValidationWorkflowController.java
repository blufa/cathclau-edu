package gn.lacaeducation.grading.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.grading.application.port.in.ValidationWorkflowUseCase;
import gn.lacaeducation.grading.domain.ValidationStep;
import gn.lacaeducation.grading.domain.ValidationWorkflow;
import gn.lacaeducation.grading.domain.WorkflowStage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Workflow de validation académique des bulletins (§10) : soumission par le professeur, validations
 * successives (responsable pédagogique → direction des études → proviseur), publication puis
 * verrouillage. Chaque étape est horodatée et signée par le niveau autorisé.
 */
@Tag(name = "Validation Workflow", description = "Workflow multi-niveaux de validation des bulletins (§10)")
@RestController
@RequestMapping("/api/v1/grading/validation-workflows")
@RequiredArgsConstructor
public class ValidationWorkflowController {

    private final ValidationWorkflowUseCase useCase;

    public record RejectRequest(WorkflowStage targetStage, String comment) {}
    public record CommentRequest(String comment) {}

    @Operation(summary = "Récupérer (ou initialiser) le workflow d'une classe/période")
    @PreAuthorize("hasAuthority('bulletin:read')")
    @GetMapping
    public ValidationWorkflow get(@RequestParam Long classId, @RequestParam Long periodId) {
        return useCase.getOrCreate(TenantContext.get(), classId, periodId);
    }

    @Operation(summary = "Faire avancer le workflow à l'étape suivante (signé par le niveau autorisé)")
    @PreAuthorize("hasAuthority('bulletin:generate')")
    @PostMapping("/{id}/advance")
    public ValidationWorkflow advance(@PathVariable Long id, @RequestBody(required = false) CommentRequest req,
                                      @AuthenticationPrincipal Jwt jwt) {
        return useCase.advance(id, userId(jwt), roles(jwt), req != null ? req.comment() : null);
    }

    @Operation(summary = "Rejeter et renvoyer le workflow à une étape antérieure")
    @PreAuthorize("hasAuthority('bulletin:publish')")
    @PostMapping("/{id}/reject")
    public ValidationWorkflow reject(@PathVariable Long id, @RequestBody RejectRequest req,
                                     @AuthenticationPrincipal Jwt jwt) {
        return useCase.reject(id, req.targetStage(), userId(jwt), roles(jwt), req.comment());
    }

    @Operation(summary = "Historique horodaté et signé des étapes du workflow")
    @GetMapping("/{id}/history")
    public List<ValidationStep> history(@PathVariable Long id) {
        return useCase.history(id);
    }

    private Long userId(Jwt jwt) {
        if (jwt == null) return null;
        try { return Long.parseLong(jwt.getSubject()); } catch (RuntimeException e) { return null; }
    }

    private List<String> roles(Jwt jwt) {
        return jwt == null ? List.of() : jwt.getClaimAsStringList("roles");
    }
}
