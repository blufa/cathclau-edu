package gn.lacaeducation.attendance.adapters.in.web;
import gn.lacaeducation.attendance.application.port.in.StudentAttendanceUseCase;
import gn.lacaeducation.attendance.application.port.in.StudentAttendanceUseCase.RecordAttendanceCommand;
import gn.lacaeducation.attendance.domain.AttendanceStatus;
import gn.lacaeducation.attendance.domain.StudentAttendance;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
@RestController @RequestMapping("/api/attendance/students") @RequiredArgsConstructor
public class StudentAttendanceController {
    private final StudentAttendanceUseCase useCase;
    @PostMapping
    public StudentAttendance record(@RequestBody RecordAttendanceCommand cmd) { return useCase.record(cmd); }
    @GetMapping("/class/{classId}")
    public List<StudentAttendance> findByClass(@PathVariable Long classId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) { return useCase.findByClass(classId, date); }
    @GetMapping("/student/{studentId}")
    public List<StudentAttendance> findByStudent(@PathVariable Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) { return useCase.findByStudent(studentId, from, to); }
    @PutMapping("/{id}")
    public StudentAttendance update(@PathVariable Long id, @RequestParam AttendanceStatus status, @RequestParam(required=false) String reason) { return useCase.update(id, status, reason); }
    @PutMapping("/{id}/notify-parent")
    public ResponseEntity<Void> notifyParent(@PathVariable Long id) { useCase.markParentNotified(id); return ResponseEntity.ok().build(); }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { useCase.delete(id); return ResponseEntity.noContent().build(); }
}
