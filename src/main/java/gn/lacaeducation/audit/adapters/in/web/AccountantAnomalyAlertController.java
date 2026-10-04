package gn.lacaeducation.audit.adapters.in.web;

import gn.lacaeducation.audit.adapters.in.web.dto.AccountantAnomalyAlertResponse;
import gn.lacaeducation.audit.application.port.in.AccountantAnomalyAlertUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Détecteur B3 (§retour fondateurs, contrôle anti-fraude comptable) — consultation uniquement :
 * résolution automatique et silencieuse par {@code AccountantAnomalyAlertScheduler}, aucun endpoint
 * de changement de statut manuel dans cette itération.
 */
@Tag(name = "Accountant anomaly alerts", description = "Écarts statistiques entre comptables (détecteur B3)")
@RestController
@RequiredArgsConstructor
public class AccountantAnomalyAlertController {

    private final AccountantAnomalyAlertUseCase useCase;

    @Operation(summary = "Alertes d'écart statistique entre comptables de l'école courante")
    @GetMapping("/api/v1/accountant-anomaly-alerts")
    @PreAuthorize("hasAuthority('audit:read')")
    public List<AccountantAnomalyAlertResponse> forCurrentTenant() {
        return useCase.findForCurrentTenant().stream().map(AccountantAnomalyAlertResponse::from).toList();
    }
}
