package gn.lacaeducation.dpe.adapters.in.web;

import gn.lacaeducation.common.DpeScopeProvider;
import gn.lacaeducation.dpe.adapters.in.web.dto.DpeAlertResponse;
import gn.lacaeducation.dpe.application.port.in.DpeAlertUseCase;
import gn.lacaeducation.dpe.domain.DpeAlertStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Alertes DPE/DCE (features/dpe.md §13). Périmètre résolu par {@link DpeScopeResolver}, indexé par
 * préfecture (pas par ensemble de tenants) puisque {@code dpe_alerts} est rattachée à une préfecture.
 */
@Tag(name = "DPE alerts", description = "Alertes de supervision (incohérences d'effectifs, écoles inactives…)")
@RestController
@RequestMapping("/api/v1/dpe/alerts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','DPE_ADMIN')")
public class DpeAlertController {

    private final DpeAlertUseCase useCase;
    private final DpeScopeResolver scopeResolver;
    private final DpeScopeProvider dpeScopeProvider;

    public record UpdateStatusRequest(DpeAlertStatus statut) {}

    @Operation(summary = "Alertes de la préfecture/commune (statut optionnel : NOUVELLE/EN_COURS/RESOLUE)")
    @GetMapping
    public List<DpeAlertResponse> list(@RequestParam(required = false) Long prefectureId,
                                        @RequestParam(required = false) DpeAlertStatus statut,
                                        Authentication auth) {
        Long resolved = scopeResolver.resolvePrefectureId(prefectureId, auth);
        if (resolved == null) {
            return List.of();
        }
        return useCase.findForPrefecture(resolved, statut).stream().map(DpeAlertResponse::from).toList();
    }

    @Operation(summary = "Change le statut d'une alerte (EN_COURS ou RESOLUE ; RESOLUE peut être rouverte en EN_COURS)")
    @PatchMapping("/{id}/statut")
    public DpeAlertResponse updateStatus(@PathVariable Long id, @RequestBody UpdateStatusRequest req,
                                          @RequestParam(required = false) Long prefectureId,
                                          Authentication auth, @AuthenticationPrincipal Jwt jwt) {
        Long resolved = scopeResolver.resolvePrefectureId(prefectureId, auth);
        return DpeAlertResponse.from(useCase.updateStatus(resolved, id, req.statut(), userIdOf(jwt)));
    }

    @Operation(summary = "Déclenche une réévaluation immédiate des alertes (au lieu d'attendre le job nocturne)")
    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void refresh(@RequestParam(required = false) Long prefectureId, Authentication auth) {
        Long resolved = scopeResolver.resolvePrefectureId(prefectureId, auth);
        if (resolved == null) {
            return;
        }
        useCase.evaluateForPrefecture(resolved, dpeScopeProvider.tenantIdsForPrefecture(resolved));
    }

    private static Long userIdOf(Jwt jwt) {
        try { return jwt == null ? null : Long.parseLong(jwt.getSubject()); } catch (RuntimeException e) { return null; }
    }
}
