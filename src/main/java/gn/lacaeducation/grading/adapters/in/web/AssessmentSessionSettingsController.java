package gn.lacaeducation.grading.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.grading.application.port.in.AssessmentSessionSettingsUseCase;
import gn.lacaeducation.grading.application.port.in.AssessmentSessionSettingsUseCase.UpdateSettingsCommand;
import gn.lacaeducation.grading.domain.AssessmentSessionSettings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Assessment Session Settings",
        description = "Durées de séance (heures travaillées enseignant) par type d'évaluation, par établissement")
@RestController
@RequestMapping("/api/v1/grading/assessment-session-settings")
@RequiredArgsConstructor
public class AssessmentSessionSettingsController {

    private final AssessmentSessionSettingsUseCase useCase;

    @Operation(summary = "Durées de séance par type d'évaluation du tenant courant (défauts si non configuré)")
    @GetMapping
    public AssessmentSessionSettings get() {
        return useCase.getForTenant(TenantContext.get());
    }

    @Operation(summary = "Configurer les durées de séance par type d'évaluation pour le tenant courant")
    @PreAuthorize("hasAuthority('parametrage_notes:manage')")
    @PutMapping
    public AssessmentSessionSettings update(@RequestBody UpdateSettingsCommand command) {
        return useCase.update(TenantContext.get(), command);
    }
}
