package gn.lacaeducation.attendance.adapters.in.web;
import gn.lacaeducation.attendance.application.port.in.StudentAttendanceUseCase;
import gn.lacaeducation.attendance.application.port.in.StudentAttendanceUseCase.RecordAttendanceCommand;
import gn.lacaeducation.attendance.application.port.in.StudentAttendanceUseCase.BulkAttendanceCommand;
import gn.lacaeducation.attendance.application.port.in.StudentAttendanceUseCase.AttendanceEntry;
import gn.lacaeducation.attendance.application.port.in.StudentAttendanceUseCase.AttendanceStatsResponse;
import gn.lacaeducation.attendance.domain.AttendanceStatus;
import gn.lacaeducation.attendance.domain.StudentAttendance;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
@Tag(name = "Student Attendance", description = "Record and query student attendance per class and date")
@RestController @RequestMapping("/api/v1/attendance/students") @RequiredArgsConstructor
public class StudentAttendanceController {
    private final StudentAttendanceUseCase useCase;

    public record BulkAttendanceRequest(
            @NotNull Long classId,
            @NotNull LocalDate date,
            @NotNull List<AttendanceEntryDto> attendances) {}

    public record AttendanceEntryDto(
            @NotNull Long studentId,
            @NotNull AttendanceStatus status,
            String reason) {}

    @Operation(summary = "Record a student attendance entry")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','TEACHER','SECRETARY')")
    @PostMapping
    public StudentAttendance record(@RequestBody RecordAttendanceCommand cmd) { return useCase.record(cmd); }

    @Operation(summary = "Record attendance for an entire class at once")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','TEACHER','SECRETARY')")
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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> to) {
        return useCase.getStats(studentId, from.orElse(null), to.orElse(null));
    }

    @Operation(summary = "Get attendance records for a class on a given date")
    @GetMapping("/class/{classId}")
    public List<StudentAttendance> findByClass(@PathVariable Long classId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) { return useCase.findByClass(classId, date); }

    @Operation(summary = "Get attendance records for a student over a date range")
    @GetMapping("/student/{studentId}")
    public List<StudentAttendance> findByStudent(@PathVariable Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) { return useCase.findByStudent(studentId, from, to); }

    @Operation(summary = "Update the status of an attendance record")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','TEACHER','SECRETARY')")
    @PutMapping("/{id}")
    public StudentAttendance update(@PathVariable Long id, @RequestParam AttendanceStatus status, @RequestParam(required=false) String reason) { return useCase.update(id, status, reason); }

    @Operation(summary = "Mark parent as notified about an absence or late arrival")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
    @PutMapping("/{id}/notify-parent")
    public ResponseEntity<Void> notifyParent(@PathVariable Long id) { useCase.markParentNotified(id); return ResponseEntity.ok().build(); }

    @Operation(summary = "Delete an attendance record")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { useCase.delete(id); return ResponseEntity.noContent().build(); }
}
