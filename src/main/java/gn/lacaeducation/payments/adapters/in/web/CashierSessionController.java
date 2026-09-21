package gn.lacaeducation.payments.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.payments.application.service.CashierSessionService;
import gn.lacaeducation.payments.domain.CashierSession;
import gn.lacaeducation.payments.domain.CashierSessionStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Rapprochement de caisse : ouverture/clôture d'une session de guichet par comptable (§angles morts
 * comptables) — comparaison du total système au comptant physique. */
@Tag(name = "Cashier Sessions", description = "Ouverture/clôture de caisse, rapprochement comptant physique vs système")
@RestController
@RequestMapping("/api/v1/payments/cashier-sessions")
@RequiredArgsConstructor
public class CashierSessionController {

    private final CashierSessionService service;

    public record CloseRequest(BigDecimal physicalCount, String notes) {}

    @Operation(summary = "Ouvrir ma caisse (refusé si une session est déjà ouverte pour moi)")
    @PreAuthorize("hasAuthority('paiement:create')")
    @PostMapping
    public CashierSession open(@AuthenticationPrincipal Jwt jwt) {
        return service.open(TenantContext.get(), currentUserId(jwt));
    }

    @Operation(summary = "Clôturer une session de caisse : indique le comptant physique, calcule l'écart")
    @PreAuthorize("hasAuthority('paiement:create')")
    @PatchMapping("/{id}/close")
    public CashierSession close(@PathVariable Long id, @RequestBody CloseRequest r) {
        return service.close(id, r.physicalCount(), r.notes());
    }

    @Operation(summary = "Ma session de caisse actuellement ouverte, avec le total encaissé en temps réel")
    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> current(@AuthenticationPrincipal Jwt jwt) {
        return service.currentOpen(currentUserId(jwt))
                .map(s -> ResponseEntity.ok(Map.<String, Object>of("session", s, "runningTotal", service.runningTotal(s))))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Détail d'une session de caisse")
    @PreAuthorize("hasAuthority('paiement:read')")
    @GetMapping("/{id}")
    public CashierSession get(@PathVariable Long id) {
        return service.get(id);
    }

    @Operation(summary = "Historique des sessions de caisse du tenant — filtrable par comptable et/ou statut")
    @PreAuthorize("hasAuthority('paiement:read')")
    @GetMapping
    public List<CashierSession> history(@RequestParam(required = false) Long cashierId,
                                        @RequestParam(required = false) CashierSessionStatus status) {
        return service.history(TenantContext.get(), cashierId, status);
    }

    private Long currentUserId(Jwt jwt) {
        try { return jwt == null ? null : Long.parseLong(jwt.getSubject()); } catch (RuntimeException e) { return null; }
    }
}
