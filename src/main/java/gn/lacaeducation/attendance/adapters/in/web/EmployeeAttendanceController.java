package gn.lacaeducation.attendance.adapters.in.web;
import gn.lacaeducation.attendance.application.port.in.EmployeeAttendanceUseCase;
import gn.lacaeducation.attendance.application.port.in.EmployeeAttendanceUseCase.RecordEmployeeAttendanceCommand;
import gn.lacaeducation.attendance.application.port.in.EmployeeAttendanceUseCase.EmployeeAttendanceStatsResponse;
import gn.lacaeducation.attendance.domain.EmployeeAttendance;
import gn.lacaeducation.attendance.domain.EmployeeAttendanceStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
@Tag(name = "Employee Attendance", description = "Présence du personnel non-enseignant (administratif, direction, soutien)")
@RestController @RequestMapping("/api/v1/attendance/employees") @RequiredArgsConstructor
public class EmployeeAttendanceController {
    private final EmployeeAttendanceUseCase useCase;

    @Operation(summary = "Record an employee attendance entry")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
    @PostMapping
    public EmployeeAttendance record(@RequestBody RecordEmployeeAttendanceCommand cmd) { return useCase.record(cmd); }

    @Operation(summary = "Marquer un membre du personnel absent aujourd'hui (un scan ne peut jamais produire ce statut)")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
    @PostMapping("/{employeeId}/mark-absent")
    public EmployeeAttendance markAbsent(@PathVariable Long employeeId, @RequestParam(required = false) String reason) {
        return useCase.markAbsent(employeeId, reason);
    }

    @Operation(summary = "Correction manuelle : saisir l'heure d'entrée pour une date donnée (le "
            + "retard est calculé automatiquement vs l'horaire configuré, comme pour un scan)")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
    @PostMapping("/{employeeId}/manual-entry")
    public EmployeeAttendance manualEntry(@PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam LocalTime checkInTime) {
        return useCase.recordManualEntry(employeeId, date, checkInTime);
    }

    @Operation(summary = "Correction manuelle : saisir l'heure de sortie pour une date donnée — utile "
            + "quand le badge a été oublié pour toute la journée")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
    @PostMapping("/{employeeId}/manual-exit")
    public EmployeeAttendance manualExit(@PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam LocalTime checkOutTime) {
        return useCase.recordManualExit(employeeId, date, checkOutTime);
    }

    @Operation(summary = "Update an employee attendance record")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
    @PutMapping("/{id}")
    public EmployeeAttendance update(@PathVariable Long id, @RequestParam EmployeeAttendanceStatus status,
                                     @RequestParam(required = false) String reason) {
        return useCase.update(id, status, reason);
    }

    @Operation(summary = "Get an employee attendance record by ID")
    @GetMapping("/{id}")
    public EmployeeAttendance findById(@PathVariable Long id) { return useCase.findById(id); }

    @Operation(summary = "Get attendance statistics for an employee")
    @GetMapping("/{employeeId}/stats")
    public EmployeeAttendanceStatsResponse getStats(
            @PathVariable Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> to) {
        return useCase.getStats(employeeId, from.orElse(null), to.orElse(null));
    }

    @Operation(summary = "Récapitulatif mensuel d'un membre du personnel (présences, retard/départ anticipé cumulés)")
    @GetMapping("/{employeeId}/monthly-summary")
    public EmployeeAttendanceUseCase.MonthlySummary monthlySummary(
            @PathVariable Long employeeId, @RequestParam int year, @RequestParam int month) {
        return useCase.monthlySummary(employeeId, year, month);
    }

    @Operation(summary = "Get attendance records for an employee over a date range")
    @GetMapping("/employee/{employeeId}")
    public List<EmployeeAttendance> findByEmployee(@PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) { return useCase.findByEmployee(employeeId, from, to); }

    @Operation(summary = "Delete an employee attendance record")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { useCase.delete(id); return ResponseEntity.noContent().build(); }
}
