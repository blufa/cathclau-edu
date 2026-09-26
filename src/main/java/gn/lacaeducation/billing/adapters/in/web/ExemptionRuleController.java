package gn.lacaeducation.billing.adapters.in.web;

import gn.lacaeducation.billing.application.port.in.ExemptionRuleUseCase;
import gn.lacaeducation.billing.application.port.in.ExemptionRuleUseCase.RuleCommand;
import gn.lacaeducation.billing.domain.ExemptionRule;
import gn.lacaeducation.common.TenantContext;
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
 * Catalogue des types d'exonération pré-configurés (§retour comptable, refonte inscription) — voir
 * {@link gn.lacaeducation.billing.domain.ExemptionRule}. Configuré une fois par la direction/
 * comptabilité, puis choisi dans une liste déroulante à l'inscription au lieu d'être ressaisi à
 * chaque élève.
 */
@Tag(name = "Exemption Rules", description = "Catalogue des types d'exonération pré-configurés")
@RestController
@RequestMapping("/api/v1/billing/exemption-rules")
@RequiredArgsConstructor
public class ExemptionRuleController {

    private final ExemptionRuleUseCase useCase;

    @Operation(summary = "Créer un type d'exonération")
    @PreAuthorize("hasAuthority('frais:manage')")
    @PostMapping
    public ResponseEntity<ExemptionRule> create(@RequestBody RuleCommand cmd) {
        ExemptionRule r = useCase.create(TenantContext.get(), cmd);
        return ResponseEntity.created(URI.create("/api/v1/billing/exemption-rules/" + r.getId())).body(r);
    }

    @Operation(summary = "Lister les types d'exonération du tenant")
    @PreAuthorize("hasAuthority('frais:manage')")
    @GetMapping
    public List<ExemptionRule> list() {
        return useCase.findByTenant(TenantContext.get());
    }

    @Operation(summary = "Modifier un type d'exonération")
    @PreAuthorize("hasAuthority('frais:manage')")
    @PutMapping("/{id}")
    public ExemptionRule update(@PathVariable Long id, @RequestBody RuleCommand cmd) {
        return useCase.update(id, cmd);
    }

    @Operation(summary = "Supprimer un type d'exonération")
    @PreAuthorize("hasAuthority('frais:manage')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        useCase.delete(id);
    }
}
