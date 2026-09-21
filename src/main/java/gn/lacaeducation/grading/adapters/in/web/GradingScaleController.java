package gn.lacaeducation.grading.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.grading.application.port.in.GradingScaleUseCase;
import gn.lacaeducation.grading.application.port.in.GradingScaleUseCase.UpdateScaleCommand;
import gn.lacaeducation.grading.domain.CycleGroup;
import gn.lacaeducation.grading.domain.GradingScale;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Grading Scale", description = "Barème de mentions/décisions par tenant, un par groupe de cycle (primaire/secondaire)")
@RestController
@RequestMapping("/api/v1/grading/scale")
@RequiredArgsConstructor
public class GradingScaleController {

    private final GradingScaleUseCase useCase;

    @Operation(summary = "Consulter le barème de mentions du tenant courant pour un groupe de cycle (défauts si non configuré)")
    @GetMapping
    public GradingScale get(@RequestParam CycleGroup cycleGroup) {
        return useCase.getForTenant(TenantContext.get(), cycleGroup);
    }

    @Operation(summary = "Définir le barème de mentions du tenant courant pour un groupe de cycle (tranches de mentions + seuil de réussite)")
    @PreAuthorize("hasAuthority('parametrage_notes:manage')")
    @PutMapping
    public GradingScale update(@RequestParam CycleGroup cycleGroup, @RequestBody UpdateScaleCommand command) {
        return useCase.update(TenantContext.get(), cycleGroup, command);
    }
}
