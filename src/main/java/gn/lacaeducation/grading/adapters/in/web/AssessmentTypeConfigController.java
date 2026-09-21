package gn.lacaeducation.grading.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.grading.application.port.in.AssessmentTypeConfigUseCase;
import gn.lacaeducation.grading.application.port.in.AssessmentTypeConfigUseCase.TypeCommand;
import gn.lacaeducation.grading.domain.AssessmentTypeConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * Types d'évaluation configurables par établissement (§3) : chaque école crée ses propres types avec
 * coefficient par défaut, poids et couleur. Configuration réservée à la direction / responsable pédagogique.
 */
@Tag(name = "Assessment Types", description = "Types d'évaluation configurables (§3)")
@RestController
@RequestMapping("/api/v1/grading/assessment-types")
@RequiredArgsConstructor
public class AssessmentTypeConfigController {

    private final AssessmentTypeConfigUseCase useCase;

    @Operation(summary = "Créer un type d'évaluation")
    @PreAuthorize("hasAuthority('parametrage_notes:manage')")
    @PostMapping
    public ResponseEntity<AssessmentTypeConfig> create(@RequestBody TypeCommand cmd) {
        AssessmentTypeConfig t = useCase.create(TenantContext.get(), cmd);
        return ResponseEntity.created(URI.create("/api/v1/grading/assessment-types/" + t.getId())).body(t);
    }

    @Operation(summary = "Lister les types d'évaluation du tenant")
    @GetMapping
    public List<AssessmentTypeConfig> list() {
        return useCase.findByTenant(TenantContext.get());
    }

    @Operation(summary = "Détail d'un type d'évaluation")
    @GetMapping("/{id}")
    public ResponseEntity<AssessmentTypeConfig> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Modifier un type d'évaluation")
    @PreAuthorize("hasAuthority('parametrage_notes:manage')")
    @PutMapping("/{id}")
    public AssessmentTypeConfig update(@PathVariable Long id, @RequestBody TypeCommand cmd) {
        return useCase.update(id, cmd);
    }

    @Operation(summary = "Supprimer un type d'évaluation")
    @PreAuthorize("hasAuthority('parametrage_notes:manage')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        useCase.delete(id);
    }
}
