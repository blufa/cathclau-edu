package gn.lacaeducation.grading.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.grading.application.port.in.GradingFormulaUseCase;
import gn.lacaeducation.grading.application.port.in.GradingFormulaUseCase.FormulaCommand;
import gn.lacaeducation.grading.domain.GradingFormula;
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
 * Formules de calcul de moyenne paramétrables (§ Refonte du moteur de calcul des notes) : remplace
 * la logique de calcul codée en dur par des étapes configurables (agrégation par mois/type
 * d'évaluation, puis combinaison pondérée), exécutées par {@code GradingFormulaEngine}.
 */
@Tag(name = "Grading Formulas", description = "Moteur de calcul des moyennes paramétrable")
@RestController
@RequestMapping("/api/v1/grading/formulas")
@RequiredArgsConstructor
public class GradingFormulaController {

    private final GradingFormulaUseCase useCase;

    @Operation(summary = "Créer une formule de calcul de moyenne")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','RESPONSABLE_PEDAGOGIQUE')")
    @PostMapping
    public ResponseEntity<GradingFormula> create(@RequestBody FormulaCommand cmd) {
        GradingFormula f = useCase.create(TenantContext.get(), cmd);
        return ResponseEntity.created(URI.create("/api/v1/grading/formulas/" + f.getId())).body(f);
    }

    @Operation(summary = "Lister les formules de calcul du tenant")
    @GetMapping
    public List<GradingFormula> list() {
        return useCase.findByTenant(TenantContext.get());
    }

    @Operation(summary = "Modifier une formule de calcul")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','RESPONSABLE_PEDAGOGIQUE')")
    @PutMapping("/{id}")
    public GradingFormula update(@PathVariable Long id, @RequestBody FormulaCommand cmd) {
        return useCase.update(id, cmd);
    }

    @Operation(summary = "Supprimer une formule de calcul")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        useCase.delete(id);
    }
}
