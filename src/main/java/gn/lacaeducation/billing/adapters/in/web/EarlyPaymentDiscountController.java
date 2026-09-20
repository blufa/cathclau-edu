package gn.lacaeducation.billing.adapters.in.web;

import gn.lacaeducation.billing.application.port.in.EarlyPaymentDiscountUseCase;
import gn.lacaeducation.billing.application.port.in.EarlyPaymentDiscountUseCase.RuleCommand;
import gn.lacaeducation.billing.domain.EarlyPaymentDiscountRule;
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
 * Règles de remise pour paiement annuel complet (§billing) : quand un parent règle en un seul
 * paiement toutes les tranches restantes d'un frais, une remise en pourcentage s'applique.
 * Configuration réservée à la direction / comptabilité.
 */
@Tag(name = "Early Payment Discounts", description = "Remises paramétrables pour paiement annuel complet")
@RestController
@RequestMapping("/api/v1/billing/early-payment-discount-rules")
@RequiredArgsConstructor
public class EarlyPaymentDiscountController {

    private final EarlyPaymentDiscountUseCase useCase;

    @Operation(summary = "Créer une règle de remise pour paiement annuel complet")
    @PreAuthorize("hasAuthority('frais:manage')")
    @PostMapping
    public ResponseEntity<EarlyPaymentDiscountRule> create(@RequestBody RuleCommand cmd) {
        EarlyPaymentDiscountRule r = useCase.create(TenantContext.get(), cmd);
        return ResponseEntity.created(URI.create("/api/v1/billing/early-payment-discount-rules/" + r.getId())).body(r);
    }

    @Operation(summary = "Lister les règles de remise pour paiement annuel complet du tenant")
    @PreAuthorize("hasAuthority('frais:manage')")
    @GetMapping
    public List<EarlyPaymentDiscountRule> list() {
        return useCase.findByTenant(TenantContext.get());
    }

    @Operation(summary = "Modifier une règle")
    @PreAuthorize("hasAuthority('frais:manage')")
    @PutMapping("/{id}")
    public EarlyPaymentDiscountRule update(@PathVariable Long id, @RequestBody RuleCommand cmd) {
        return useCase.update(id, cmd);
    }

    @Operation(summary = "Supprimer une règle")
    @PreAuthorize("hasAuthority('frais:manage')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        useCase.delete(id);
    }
}
