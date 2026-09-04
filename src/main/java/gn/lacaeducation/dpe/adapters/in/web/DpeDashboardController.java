package gn.lacaeducation.dpe.adapters.in.web;

import gn.lacaeducation.dpe.application.port.in.DpeDashboardUseCase;
import gn.lacaeducation.dpe.application.port.in.DpeDashboardUseCase.DashboardOverview;
import gn.lacaeducation.dpe.application.port.in.DpeDashboardUseCase.SchoolSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Tableau de bord DPE/DCE (DPE_ARCHITECTURE_ANALYSIS.md §13, DPE_STATISTICS_SPECIFICATION.md §A-B).
 * Périmètre résolu par {@link DpeScopeResolver} (partagé avec {@link DpeAlertController}), jamais
 * dans le service — voir sa Javadoc pour la règle DPE_ADMIN vs SUPER_ADMIN.
 */
@Tag(name = "DPE dashboard", description = "Supervision en lecture seule des écoles du périmètre DPE/DCE")
@RestController
@RequestMapping("/api/v1/dpe")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','DPE_ADMIN')")
public class DpeDashboardController {

    private final DpeDashboardUseCase useCase;
    private final DpeScopeResolver scopeResolver;

    @Operation(summary = "Vue générale de la préfecture/commune")
    @GetMapping("/dashboard")
    public DashboardOverview dashboard(@RequestParam(required = false) Long prefectureId, Authentication auth) {
        return useCase.overview(scopeResolver.resolveTenantScope(prefectureId, auth));
    }

    @Operation(summary = "Écoles du périmètre avec leurs effectifs de base")
    @GetMapping("/schools")
    public List<SchoolSummary> schools(@RequestParam(required = false) Long prefectureId, Authentication auth) {
        return useCase.schools(scopeResolver.resolveTenantScope(prefectureId, auth));
    }

    @Operation(summary = "Détail d'une école du périmètre (403 si hors périmètre)")
    @GetMapping("/schools/{tenantId}")
    public SchoolSummary school(@PathVariable Long tenantId,
                                 @RequestParam(required = false) Long prefectureId, Authentication auth) {
        return useCase.schoolDetail(scopeResolver.resolveTenantScope(prefectureId, auth), tenantId);
    }
}
