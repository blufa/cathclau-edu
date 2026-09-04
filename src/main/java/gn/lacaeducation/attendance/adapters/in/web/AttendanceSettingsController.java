package gn.lacaeducation.attendance.adapters.in.web;

import gn.lacaeducation.attendance.application.port.in.AttendanceSettingsUseCase;
import gn.lacaeducation.attendance.domain.AttendanceSettings;
import gn.lacaeducation.attendance.domain.RecessWindow;
import gn.lacaeducation.common.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;

/**
 * Réglages du calcul des heures enseignant réalisées : marge de transition entre deux séances
 * consécutives dans des salles différentes, et fenêtres de récréation (temps défini par l'école,
 * non imputable à l'enseignant sur son heure d'arrivée).
 */
@Tag(name = "Attendance Settings", description = "Marge de transition et fenêtres de récréation pour le calcul des heures enseignant")
@RestController
@RequestMapping("/api/v1/attendance/settings")
@RequiredArgsConstructor
public class AttendanceSettingsController {

    private final AttendanceSettingsUseCase useCase;

    public record UpdateMarginRequest(Integer transitionMarginMinutes) {}
    public record RecessWindowRequest(String label, LocalTime startTime, LocalTime endTime) {}

    @Operation(summary = "Réglage du tenant courant (marge de transition, défaut 5 min si non configuré)")
    @GetMapping
    public AttendanceSettings get() {
        return useCase.getSettings(TenantContext.get());
    }

    @Operation(summary = "Configurer la marge de transition entre deux séances consécutives")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @PutMapping
    public AttendanceSettings update(@RequestBody UpdateMarginRequest r) {
        return useCase.updateTransitionMargin(TenantContext.get(), r.transitionMarginMinutes());
    }

    @Operation(summary = "Lister les fenêtres de récréation du tenant courant")
    @GetMapping("/recess-windows")
    public List<RecessWindow> listRecessWindows() {
        return useCase.listRecessWindows(TenantContext.get());
    }

    @Operation(summary = "Ajouter une fenêtre de récréation (ex. 10h00-10h15)")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @PostMapping("/recess-windows")
    @ResponseStatus(HttpStatus.CREATED)
    public RecessWindow addRecessWindow(@RequestBody RecessWindowRequest r) {
        return useCase.addRecessWindow(TenantContext.get(), r.label(), r.startTime(), r.endTime());
    }

    @Operation(summary = "Supprimer une fenêtre de récréation")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @DeleteMapping("/recess-windows/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRecessWindow(@PathVariable Long id) {
        useCase.deleteRecessWindow(TenantContext.get(), id);
    }
}
