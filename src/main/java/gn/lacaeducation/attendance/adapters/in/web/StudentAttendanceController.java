package gn.lacaeducation.attendance.adapters.in.web;
import gn.lacaeducation.attendance.application.port.in.StudentAttendanceUseCase;
import gn.lacaeducation.attendance.application.port.in.StudentAttendanceUseCase.RecordAttendanceCommand;
import gn.lacaeducation.attendance.application.port.in.StudentAttendanceUseCase.BulkAttendanceCommand;
import gn.lacaeducation.attendance.application.port.in.StudentAttendanceUseCase.AttendanceEntry;
import gn.lacaeducation.attendance.application.port.in.StudentAttendanceUseCase.AttendanceStatsResponse;
import gn.lacaeducation.attendance.domain.AttendanceStatus;
import gn.lacaeducation.attendance.domain.ChildPickup;
import gn.lacaeducation.attendance.domain.StudentAttendance;
import gn.lacaeducation.attendance.domain.StudentCheckin;
import gn.lacaeducation.common.PortalAccess;
import gn.lacaeducation.students.ParentAccessProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
@Tag(name = "Student Attendance", description = "Record and query student attendance per class and date")
@RestController @RequestMapping("/api/v1/attendance/students") @RequiredArgsConstructor
public class StudentAttendanceController {
    private final StudentAttendanceUseCase useCase;
    private final ParentAccessProvider parentAccessProvider;

    private void assertParentScope(Jwt jwt, Long studentId) {
        if (jwt == null) return;
        Long userId;
        try { userId = Long.parseLong(jwt.getSubject()); } catch (RuntimeException e) { userId = null; }
        PortalAccess.assertParentCanAccessStudent(jwt.getClaimAsStringList("roles"), userId, studentId,
                parentAccessProvider::isParentOfStudent);
    }

    public record BulkAttendanceRequest(
            @NotNull Long classId,
            @NotNull LocalDate date,
            @NotNull List<AttendanceEntryDto> attendances) {}

    public record AttendanceEntryDto(
            @NotNull Long studentId,
            @NotNull AttendanceStatus status,
            String reason) {}

    @Operation(summary = "Record a student attendance entry")
    @PreAuthorize("hasAuthority('presence:create')")
    @PostMapping
    public StudentAttendance record(@RequestBody RecordAttendanceCommand cmd) { return useCase.record(cmd); }

    @Operation(summary = "Marquer un élève absent aujourd'hui (un scan ne peut jamais produire ce statut)")
    @PreAuthorize("hasAuthority('presence:create')")
    @PostMapping("/{studentId}/mark-absent")
    public StudentAttendance markAbsent(@PathVariable Long studentId, @RequestParam(required = false) String reason) {
        return useCase.markAbsent(studentId, reason);
    }

    @Operation(summary = "Correction manuelle : saisir l'heure d'entrée d'un élève pour une date donnée "
            + "(le retard est calculé automatiquement vs l'emploi du temps, comme pour un scan)")
    @PreAuthorize("hasAuthority('presence:create')")
    @PostMapping("/{studentId}/manual-entry")
    public StudentCheckin manualEntry(@PathVariable Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam LocalTime checkInTime) {
        return useCase.recordManualEntry(studentId, date, checkInTime);
    }

    @Operation(summary = "Correction manuelle : saisir l'heure de sortie d'un élève pour une date "
            + "donnée (le départ anticipé est calculé automatiquement vs l'emploi du temps, comme pour "
            + "un scan) — utile quand le badge a été oublié pour toute la séance")
    @PreAuthorize("hasAuthority('presence:create')")
    @PostMapping("/{studentId}/manual-exit")
    public StudentCheckin manualExit(@PathVariable Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam LocalTime checkOutTime) {
        return useCase.recordManualExit(studentId, date, checkOutTime);
    }

    @Operation(summary = "Pointages (par séance) d'un élève à une date donnée — heure d'arrivée/sortie, "
            + "retard et départ anticipé par créneau (ex. pour le dashboard parent)")
    @GetMapping("/{studentId}/checkins")
    public List<StudentCheckin> checkins(@PathVariable Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal Jwt jwt) {
        assertParentScope(jwt, studentId);
        return useCase.findCheckins(studentId, date);
    }

    @Operation(summary = "Badge de retrait (maternelle/primaire) : enregistrer qui est venu récupérer "
            + "l'enfant — un parent déjà lié (pickerParentId) ou une personne saisie librement "
            + "(pickerName + pickerRelation)")
    @PreAuthorize("hasAuthority('presence:create')")
    @PostMapping("/{studentId}/pickup")
    public ChildPickup recordPickup(@PathVariable Long studentId,
            @RequestParam(required = false) Long pickerParentId,
            @RequestParam(required = false) String pickerName,
            @RequestParam(required = false) String pickerRelation) {
        return useCase.recordPickup(studentId, pickerParentId, pickerName, pickerRelation);
    }

    @Operation(summary = "Retraits d'un élève à une date donnée (historique/audit)")
    @GetMapping("/{studentId}/pickups")
    public List<ChildPickup> findPickups(@PathVariable Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return useCase.findPickups(studentId, date);
    }

    @Operation(summary = "Record attendance for an entire class at once")
    @PreAuthorize("hasAuthority('presence:create')")
    @PostMapping("/bulk")
    public ResponseEntity<List<StudentAttendance>> recordBulk(@Valid @RequestBody BulkAttendanceRequest req) {
        List<AttendanceEntry> entries = req.attendances().stream()
                .map(e -> new AttendanceEntry(e.studentId(), e.status(), e.reason()))
                .toList();
        List<StudentAttendance> result = useCase.recordBulk(new BulkAttendanceCommand(req.classId(), req.date(), entries));
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Get attendance statistics for a student")
    @GetMapping("/{studentId}/stats")
    public AttendanceStatsResponse getStats(
            @PathVariable Long studentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> to,
            @AuthenticationPrincipal Jwt jwt) {
        assertParentScope(jwt, studentId);
        return useCase.getStats(studentId, from.orElse(null), to.orElse(null));
    }

    @Operation(summary = "Get attendance records for a class on a given date")
    @GetMapping("/class/{classId}")
    public List<StudentAttendance> findByClass(@PathVariable Long classId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) { return useCase.findByClass(classId, date); }

    @Operation(summary = "Pointages (par séance, tous élèves) d'une classe à une date donnée — heure "
            + "de sortie/départ anticipé par élève et par créneau, pour le tableau de suivi du personnel")
    @GetMapping("/class/{classId}/checkins")
    public List<StudentCheckin> checkinsByClass(@PathVariable Long classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return useCase.findCheckinsByClass(classId, date);
    }

    @Operation(summary = "Get attendance records for a student over a date range")
    @GetMapping("/student/{studentId}")
    public List<StudentAttendance> findByStudent(@PathVariable Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal Jwt jwt) { assertParentScope(jwt, studentId); return useCase.findByStudent(studentId, from, to); }

    @Operation(summary = "Liste des absents d'une classe pour une date (PDF)")
    @PreAuthorize("hasAuthority('presence:read') and !hasRole('PARENT') and !hasRole('STUDENT')")
    @GetMapping("/class/{classId}/absentees/download")
    public ResponseEntity<byte[]> absenteesDownload(@PathVariable Long classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"absents-classe-" + classId + "-" + date + ".pdf\"")
                .body(useCase.absenteeListPdf(classId, date));
    }

    @Operation(summary = "Rapport des retards d'une classe sur une période (PDF)")
    @PreAuthorize("hasAuthority('presence:read') and !hasRole('PARENT') and !hasRole('STUDENT')")
    @GetMapping("/class/{classId}/lateness/download")
    public ResponseEntity<byte[]> latenessDownload(@PathVariable Long classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"retards-classe-" + classId + ".pdf\"")
                .body(useCase.latenessReportPdf(classId, from, to));
    }

    @Operation(summary = "Update the status of an attendance record")
    @PreAuthorize("hasAuthority('presence:update')")
    @PutMapping("/{id}")
    public StudentAttendance update(@PathVariable Long id, @RequestParam AttendanceStatus status, @RequestParam(required=false) String reason) { return useCase.update(id, status, reason); }

    public record JustifyRequest(Boolean justified, String reason, String justificationDoc) {}

    @Operation(summary = "Justifier (ou non) une absence : justifié + motif + pièce")
    @PreAuthorize("hasAuthority('presence:update')")
    @PatchMapping("/{id}/justify")
    public StudentAttendance justify(@PathVariable Long id, @RequestBody JustifyRequest r) {
        return useCase.justify(id, r.justified(), r.reason(), r.justificationDoc());
    }

    @Operation(summary = "Alertes : élèves d'une classe avec ≥ seuil absences non justifiées sur la période")
    @PreAuthorize("hasAuthority('presence:read') and !hasRole('PARENT') and !hasRole('STUDENT')")
    @GetMapping("/class/{classId}/unjustified-alerts")
    public List<StudentAttendanceUseCase.UnjustifiedAlert> unjustifiedAlerts(
            @PathVariable Long classId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "2") int threshold) {
        return useCase.unjustifiedAlerts(classId, from, to, threshold);
    }

    // Redéclenche réellement l'envoi (SMS + email) plutôt que de seulement cocher la case — voir
    // StudentAttendanceUseCase#notifyParentNow. Rôles alignés sur `canWrite` côté frontend (le bouton
    // est visible aux mêmes rôles que la saisie de présence, dont TEACHER) ; sans ça, un enseignant
    // voyait le bouton mais recevait un 403 silencieux en cliquant.
    @Operation(summary = "Resend the absence/lateness notification to the parent and mark it as handled")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY','TEACHER')")
    @PutMapping("/{id}/notify-parent")
    public ResponseEntity<Void> notifyParent(@PathVariable Long id) { useCase.notifyParentNow(id); return ResponseEntity.ok().build(); }

    @Operation(summary = "Delete an attendance record")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { useCase.delete(id); return ResponseEntity.noContent().build(); }
}
