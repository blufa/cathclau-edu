package gn.lacaeducation.grading.adapters.in.web;

import gn.lacaeducation.grading.application.port.in.GradeSimulationUseCase;
import gn.lacaeducation.grading.application.port.in.GradeSimulationUseCase.SimulationReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simulateur de bulletins (§9) : calcul à blanc des moyennes/rangs + détection d'anomalies pour une
 * classe et une période, avant publication. Lecture seule.
 */
@Tag(name = "Grade Simulation", description = "Simulation des bulletins avant publication (§9)")
@RestController
@RequestMapping("/api/v1/grading/simulation")
@RequiredArgsConstructor
public class GradeSimulationController {

    private final GradeSimulationUseCase useCase;

    @Operation(summary = "Simuler les bulletins d'une classe/période : moyennes, rangs et anomalies (sans publier)")
    @PreAuthorize("hasAuthority('bulletin:read')")
    @GetMapping
    public SimulationReport simulate(@RequestParam Long classId, @RequestParam Long periodId) {
        return useCase.simulate(classId, periodId);
    }
}
