package gn.lacaeducation.grading.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.grading.application.port.in.EliminatoryRuleUseCase;
import gn.lacaeducation.grading.application.port.in.EliminatoryRuleUseCase.RuleCommand;
import gn.lacaeducation.grading.domain.EliminatoryRule;
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
 * Notes éliminatoires par matière (§7), paramétrables : seuil sous lequel l'élève est ajourné.
 * Configuration réservée à la direction / responsable pédagogique.
 */
@Tag(name = "Eliminatory Rules", description = "Notes éliminatoires par matière (§7)")
@RestController
@RequestMapping("/api/v1/grading/eliminatory-rules")
@RequiredArgsConstructor
public class EliminatoryRuleController {

    private final EliminatoryRuleUseCase useCase;

    @Operation(summary = "Créer une règle de note éliminatoire pour une matière")
    @PreAuthorize("hasAuthority('parametrage_notes:manage')")
    @PostMapping
    public ResponseEntity<EliminatoryRule> create(@RequestBody RuleCommand cmd) {
        EliminatoryRule r = useCase.create(TenantContext.get(), cmd);
        return ResponseEntity.created(URI.create("/api/v1/grading/eliminatory-rules/" + r.getId())).body(r);
    }

    @Operation(summary = "Lister les règles éliminatoires du tenant")
    @GetMapping
    public List<EliminatoryRule> list() {
        return useCase.findByTenant(TenantContext.get());
    }

    @Operation(summary = "Modifier une règle éliminatoire")
    @PreAuthorize("hasAuthority('parametrage_notes:manage')")
    @PutMapping("/{id}")
    public EliminatoryRule update(@PathVariable Long id, @RequestBody RuleCommand cmd) {
        return useCase.update(id, cmd);
    }

    @Operation(summary = "Supprimer une règle éliminatoire")
    @PreAuthorize("hasAuthority('parametrage_notes:manage')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        useCase.delete(id);
    }
}
