package gn.lacaeducation.grading.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.grading.application.port.in.DecisionRuleUseCase;
import gn.lacaeducation.grading.application.port.in.DecisionRuleUseCase.RuleCommand;
import gn.lacaeducation.grading.domain.DecisionRule;
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
 * Règles de décision paramétrables (§17 — rule engine) : conditions de passage/redoublement définies
 * sans code, activables et versionnées. Configuration réservée à la direction / responsable pédagogique.
 */
@Tag(name = "Decision Rules", description = "Moteur de règles de décision paramétrable (§17)")
@RestController
@RequestMapping("/api/v1/grading/decision-rules")
@RequiredArgsConstructor
public class DecisionRuleController {

    private final DecisionRuleUseCase useCase;

    @Operation(summary = "Créer une règle de décision (SI condition ALORS décision)")
    @PreAuthorize("hasAuthority('parametrage_notes:manage')")
    @PostMapping
    public ResponseEntity<DecisionRule> create(@RequestBody RuleCommand cmd) {
        DecisionRule r = useCase.create(TenantContext.get(), cmd);
        return ResponseEntity.created(URI.create("/api/v1/grading/decision-rules/" + r.getId())).body(r);
    }

    @Operation(summary = "Lister les règles de décision du tenant")
    @GetMapping
    public List<DecisionRule> list() {
        return useCase.findByTenant(TenantContext.get());
    }

    @Operation(summary = "Modifier une règle de décision")
    @PreAuthorize("hasAuthority('parametrage_notes:manage')")
    @PutMapping("/{id}")
    public DecisionRule update(@PathVariable Long id, @RequestBody RuleCommand cmd) {
        return useCase.update(id, cmd);
    }

    @Operation(summary = "Supprimer une règle de décision")
    @PreAuthorize("hasAuthority('parametrage_notes:manage')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        useCase.delete(id);
    }
}
