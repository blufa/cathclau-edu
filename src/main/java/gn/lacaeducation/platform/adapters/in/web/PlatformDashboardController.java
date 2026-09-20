package gn.lacaeducation.platform.adapters.in.web;

import gn.lacaeducation.platform.application.port.in.PlatformDashboardUseCase;
import gn.lacaeducation.platform.application.port.in.PlatformDashboardUseCase.PlatformDashboard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Platform Dashboard", description = "Indicateurs éditeur : établissements, MRR/ARR, churn (SUPER_ADMIN)")
@RestController
@RequestMapping("/api/v1/platform/dashboard")
@RequiredArgsConstructor
public class PlatformDashboardController {

    private final PlatformDashboardUseCase dashboardUseCase;

    @Operation(summary = "Indicateurs globaux de la plateforme : nombre d'établissements, MRR/ARR, répartition par plan, churn, revenu mensuel")
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public PlatformDashboard dashboard() {
        return dashboardUseCase.getDashboard();
    }
}
