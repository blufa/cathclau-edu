package gn.lacaeducation.attendance.adapters.in.web;
import gn.lacaeducation.attendance.application.port.in.TeacherAttendanceUseCase;
import gn.lacaeducation.attendance.application.port.in.TeacherAttendanceUseCase.RecordTeacherAttendanceCommand;
import gn.lacaeducation.attendance.domain.TeacherAttendance;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
@Tag(name = "Teacher Attendance", description = "Record and query teacher attendance")
@RestController @RequestMapping("/api/v1/attendance/teachers") @RequiredArgsConstructor
public class TeacherAttendanceController {
    private final TeacherAttendanceUseCase useCase;
    @Operation(summary = "Record a teacher attendance entry")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
    @PostMapping
    public TeacherAttendance record(@RequestBody RecordTeacherAttendanceCommand cmd) { return useCase.record(cmd); }
    @Operation(summary = "Get attendance records for a teacher over a date range")
    @GetMapping("/teacher/{teacherId}")
    public List<TeacherAttendance> findByTeacher(@PathVariable Long teacherId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) { return useCase.findByTeacher(teacherId, from, to); }
    @Operation(summary = "Delete a teacher attendance record")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { useCase.delete(id); return ResponseEntity.noContent().build(); }
}
