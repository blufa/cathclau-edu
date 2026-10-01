package gn.lacaeducation.billing.adapters.in.web;

import gn.lacaeducation.billing.application.port.in.FamilyDiscountUseCase;
import gn.lacaeducation.billing.application.port.in.FamilyDiscountUseCase.ApplicableDiscount;
import gn.lacaeducation.billing.application.port.in.FamilyDiscountUseCase.RuleCommand;
import gn.lacaeducation.billing.domain.FamilyDiscountRule;
import gn.lacaeducation.billing.domain.FeeType;
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
 * Règles de réduction familiale paramétrables (§5) et calcul de la réduction applicable à un élève.
 * Configuration réservée à la direction / comptabilité.
 */
@Tag(name = "Family Discounts", description = "Règles de réduction familiale paramétrables (§5)")
@RestController
@RequestMapping("/api/v1/billing/family-discount-rules")
@RequiredArgsConstructor
public class FamilyDiscountController {

    private final FamilyDiscountUseCase useCase;

    @Operation(summary = "Créer une règle de réduction familiale")
    @PreAuthorize("hasAuthority('regle_facturation:create')")
    @PostMapping
    public ResponseEntity<FamilyDiscountRule> create(@RequestBody RuleCommand cmd) {
        FamilyDiscountRule r = useCase.create(TenantContext.get(), cmd);
        return ResponseEntity.created(URI.create("/api/v1/billing/family-discount-rules/" + r.getId())).body(r);
    }

    @Operation(summary = "Lister les règles de réduction familiale du tenant")
    @PreAuthorize("hasAuthority('regle_facturation:read')")
    @GetMapping
    public List<FamilyDiscountRule> list() {
        return useCase.findByTenant(TenantContext.get());
    }

    @Operation(summary = "Modifier une règle")
    @PreAuthorize("hasAuthority('regle_facturation:update')")
    @PutMapping("/{id}")
    public FamilyDiscountRule update(@PathVariable Long id, @RequestBody RuleCommand cmd) {
        return useCase.update(id, cmd);
    }

    @Operation(summary = "Supprimer une règle")
    @PreAuthorize("hasAuthority('regle_facturation:delete')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        useCase.delete(id);
    }

    @Operation(summary = "Réduction familiale applicable à un élève pour un frais donné")
    @PreAuthorize("hasAuthority('regle_facturation:read')")
    @GetMapping("/applicable")
    public ApplicableDiscount applicable(@RequestParam Long studentId, @RequestParam FeeType feeType) {
        return useCase.computeApplicable(TenantContext.get(), studentId, feeType);
    }

    @Operation(summary = "Parmi une liste d'élèves, ceux qui sont le cadet exonéré d'une fratrie nombreuse (pour badge dans la liste des élèves)")
    @PreAuthorize("hasAuthority('regle_facturation:read')")
    @GetMapping("/eligible-youngest")
    public List<Long> eligibleYoungest(@RequestParam List<Long> studentIds) {
        return useCase.eligibleYoungest(TenantContext.get(), studentIds);
    }
}
