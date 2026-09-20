package gn.lacaeducation.attendance.adapters.in.web;
import gn.lacaeducation.attendance.application.port.in.TeacherAttendanceUseCase;
import gn.lacaeducation.attendance.application.port.in.TeacherAttendanceUseCase.RecordTeacherAttendanceCommand;
import gn.lacaeducation.attendance.application.port.in.TeacherAttendanceUseCase.TeacherAttendanceStatsResponse;
import gn.lacaeducation.attendance.domain.TeacherAttendance;
import gn.lacaeducation.attendance.domain.TeacherAttendanceStatus;
import gn.lacaeducation.attendance.domain.TeacherCheckin;
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
@Tag(name = "Teacher Attendance", description = "Record and query teacher attendance")
@RestController @RequestMapping("/api/v1/attendance/teachers") @RequiredArgsConstructor
public class TeacherAttendanceController {
    private final TeacherAttendanceUseCase useCase;

    @Operation(summary = "Record a teacher attendance entry")
    @PreAuthorize("hasAuthority('presence:create')")
    @PostMapping
    public TeacherAttendance record(@RequestBody RecordTeacherAttendanceCommand cmd) { return useCase.record(cmd); }

    @Operation(summary = "Marquer un enseignant absent aujourd'hui (un scan ne peut jamais produire ce statut)")
    @PreAuthorize("hasAuthority('presence:create')")
    @PostMapping("/{teacherId}/mark-absent")
    public TeacherAttendance markAbsent(@PathVariable Long teacherId, @RequestParam(required = false) String reason) {
        return useCase.markAbsent(teacherId, reason);
    }

    @Operation(summary = "Correction manuelle : saisir l'heure d'entrée d'un enseignant pour une date "
            + "donnée (le retard est calculé automatiquement vs l'emploi du temps, comme pour un scan)")
    @PreAuthorize("hasAuthority('presence:create')")
    @PostMapping("/{teacherId}/manual-entry")
    public TeacherCheckin manualEntry(@PathVariable Long teacherId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam LocalTime checkInTime) {
        return useCase.recordManualEntry(teacherId, date, checkInTime);
    }

    @Operation(summary = "Correction manuelle : saisir l'heure de sortie d'un enseignant pour une date "
            + "donnée (le départ anticipé est calculé automatiquement vs l'emploi du temps, comme pour un scan) "
            + "— utile quand le badge a été oublié pour toute la séance")
    @PreAuthorize("hasAuthority('presence:create')")
    @PostMapping("/{teacherId}/manual-exit")
    public TeacherCheckin manualExit(@PathVariable Long teacherId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam LocalTime checkOutTime) {
        return useCase.recordManualExit(teacherId, date, checkOutTime);
    }

    @Operation(summary = "Update a teacher attendance record")
    @PreAuthorize("hasAuthority('presence:update')")
    @PutMapping("/{id}")
    public TeacherAttendance update(@PathVariable Long id, @RequestParam TeacherAttendanceStatus status,
                                    @RequestParam(required = false) String reason) {
        return useCase.update(id, status, reason);
    }

    @Operation(summary = "Get a teacher attendance record by ID")
    @GetMapping("/{id}")
    public TeacherAttendance findById(@PathVariable Long id) { return useCase.findById(id); }

    @Operation(summary = "Get attendance statistics for a teacher")
    @GetMapping("/{teacherId}/stats")
    public TeacherAttendanceStatsResponse getStats(
            @PathVariable Long teacherId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> to) {
        return useCase.getStats(teacherId, from.orElse(null), to.orElse(null));
    }

    @Operation(summary = "Récapitulatif mensuel d'un enseignant (présences, retard cumulé, heures prévues/effectives)")
    @GetMapping("/{teacherId}/monthly-summary")
    public TeacherAttendanceUseCase.MonthlySummary monthlySummary(
            @PathVariable Long teacherId, @RequestParam int year, @RequestParam int month) {
        return useCase.monthlySummary(teacherId, year, month);
    }

    @Operation(summary = "Get attendance records for a teacher over a date range")
    @GetMapping("/teacher/{teacherId}")
    public List<TeacherAttendance> findByTeacher(@PathVariable Long teacherId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) { return useCase.findByTeacher(teacherId, from, to); }

    @Operation(summary = "Pointages (par séance) d'un enseignant à une date donnée — heure d'arrivée et "
            + "retard par créneau (ex. pour le cahier de textes : à quelle heure il a pointé pour une séance)")
    @GetMapping("/{teacherId}/checkins")
    public List<TeacherCheckin> checkins(@PathVariable Long teacherId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return useCase.findCheckins(teacherId, date);
    }

    @Operation(summary = "Delete a teacher attendance record")
    @PreAuthorize("hasAuthority('presence:delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { useCase.delete(id); return ResponseEntity.noContent().build(); }

    @Operation(summary = "Détail retard/départ anticipé par séance (matière/classe/jour) sur une période "
            + "— pour le récapitulatif de la fiche enseignant (contestation de salaire)")
    @PreAuthorize("hasAuthority('presence:read') or hasAuthority('paie:manage')")
    @GetMapping("/{teacherId}/lateness-detail")
    public List<TeacherAttendanceUseCase.LatenessDetailRow> latenessDetail(@PathVariable Long teacherId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return useCase.latenessDetail(teacherId, from, to);
    }

    @Operation(summary = "Export PDF du récapitulatif retards/absences d'un enseignant sur une période")
    @PreAuthorize("hasAuthority('presence:read') or hasAuthority('paie:manage')")
    @GetMapping("/{teacherId}/lateness/download")
    public ResponseEntity<byte[]> latenessReportDownload(@PathVariable Long teacherId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"retards-enseignant-" + teacherId + ".pdf\"")
                .body(useCase.latenessReportPdf(teacherId, from, to));
    }
}
