package gn.lacaeducation.staff.adapters.in.web;

import gn.lacaeducation.staff.application.service.TeacherAvailabilityService;
import gn.lacaeducation.staff.domain.TeacherAvailability;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;

@Tag(name = "Teacher Availability", description = "Plages de disponibilité des enseignants (emploi du temps)")
@RestController
@RequestMapping("/api/v1/staff/teachers")
@RequiredArgsConstructor
public class TeacherAvailabilityController {

    private final TeacherAvailabilityService service;

    public record AvailabilityRequest(String day, LocalTime startTime, LocalTime endTime,
                                       Long schoolClassId, Long subjectId) {}

    @Operation(summary = "List a teacher's availability slots")
    @GetMapping("/{teacherId}/availability")
    public List<TeacherAvailability> list(@PathVariable Long teacherId) {
        return service.findByTeacher(teacherId);
    }

    @Operation(summary = "Add an availability slot for a teacher")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','TEACHER')")
    @PostMapping("/{teacherId}/availability")
    public TeacherAvailability add(@PathVariable Long teacherId, @RequestBody AvailabilityRequest r) {
        return service.add(teacherId, r.day(), r.startTime(), r.endTime(), r.schoolClassId(), r.subjectId());
    }

    @Operation(summary = "Delete an availability slot")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','TEACHER')")
    @DeleteMapping("/availability/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
