package gn.lacaeducation.academics.adapters.in.web;

import gn.lacaeducation.academics.application.port.in.PrimaryTimetableSettingsUseCase;
import gn.lacaeducation.academics.domain.PrimaryTimetableSettings;
import gn.lacaeducation.common.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;

/**
 * Grille horaire du Primaire/Maternelle (§3.2) : plage et durée des créneaux, configurables par
 * tenant plutôt que fixées à 08h-18h/60min en dur côté frontend.
 */
@Tag(name = "Primary Timetable Settings", description = "Grille horaire configurable du Primaire/Maternelle")
@RestController
@RequestMapping("/api/v1/academics/primary-timetable-settings")
@RequiredArgsConstructor
public class PrimaryTimetableSettingsController {

    private final PrimaryTimetableSettingsUseCase useCase;

    public record UpdateGridRequest(LocalTime gridStartTime, LocalTime gridEndTime, Integer bandMinutes) {}

    @Operation(summary = "Réglage du tenant courant (08h-18h par bandes de 60min si non configuré)")
    @GetMapping
    public PrimaryTimetableSettings get() {
        return useCase.getSettings(TenantContext.get());
    }

    @Operation(summary = "Configurer la grille horaire du Primaire/Maternelle (heure de début, de fin, durée des créneaux)")
    @PreAuthorize("hasAuthority('emploi:manage')")
    @PutMapping
    public PrimaryTimetableSettings update(@RequestBody UpdateGridRequest r) {
        return useCase.updateGrid(TenantContext.get(), r.gridStartTime(), r.gridEndTime(), r.bandMinutes());
    }
}
