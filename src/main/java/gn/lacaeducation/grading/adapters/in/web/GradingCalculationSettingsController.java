package gn.lacaeducation.grading.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.grading.application.port.in.GradingCalculationSettingsUseCase;
import gn.lacaeducation.grading.application.port.in.GradingCalculationSettingsUseCase.UpdateCommand;
import gn.lacaeducation.grading.domain.GradingCalculationSettings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Grading Calculation Settings",
        description = "Moteur de calcul des moyennes : méthode, décimales et arrondi par établissement (§2/§5)")
@RestController
@RequestMapping("/api/v1/grading/calculation-settings")
@RequiredArgsConstructor
public class GradingCalculationSettingsController {

    private final GradingCalculationSettingsUseCase useCase;

    @Operation(summary = "Réglage du moteur de calcul du tenant courant (défauts si non configuré)")
    @GetMapping
    public GradingCalculationSettings get() {
        return useCase.getForTenant(TenantContext.get());
    }

    @Operation(summary = "Configurer la méthode de calcul, les décimales et l'arrondi des moyennes")
    @PreAuthorize("hasAuthority('parametrage_notes:manage')")
    @PutMapping
    public GradingCalculationSettings update(@RequestBody UpdateCommand command) {
        return useCase.update(TenantContext.get(), command);
    }
}
