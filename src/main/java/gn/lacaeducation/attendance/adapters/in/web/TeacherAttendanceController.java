package gn.lacaeducation.attendance.adapters.in.web;
import gn.lacaeducation.attendance.application.port.in.TeacherAttendanceUseCase;
import gn.lacaeducation.attendance.application.port.in.TeacherAttendanceUseCase.RecordTeacherAttendanceCommand;
import gn.lacaeducation.attendance.domain.TeacherAttendance;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
@RestController @RequestMapping("/api/attendance/teachers") @RequiredArgsConstructor
public class TeacherAttendanceController {
    private final TeacherAttendanceUseCase useCase;
    @PostMapping
    public TeacherAttendance record(@RequestBody RecordTeacherAttendanceCommand cmd) { return useCase.record(cmd); }
    @GetMapping("/teacher/{teacherId}")
    public List<TeacherAttendance> findByTeacher(@PathVariable Long teacherId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) { return useCase.findByTeacher(teacherId, from, to); }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { useCase.delete(id); return ResponseEntity.noContent().build(); }
}
