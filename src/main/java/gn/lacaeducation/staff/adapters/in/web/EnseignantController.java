package gn.lacaeducation.staff.adapters.in.web;

import gn.lacaeducation.academics.ClassPublicInfo;
import gn.lacaeducation.academics.ClassSubjectPublicInfo;
import gn.lacaeducation.academics.ScheduleSlotPublicInfo;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.staff.application.port.in.TeacherUseCase;
import gn.lacaeducation.staff.application.port.in.TeacherUseCase.CreateTeacherCommand;
import gn.lacaeducation.staff.domain.Teacher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@Tag(name = "Teachers", description = "Manage teaching staff members within the school")
@RestController @RequestMapping("/api/v1/staff/teachers") @RequiredArgsConstructor
public class EnseignantController {

    private final TeacherUseCase teacherUseCase;

    @Operation(summary = "Create a new teacher")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @PostMapping
    public ResponseEntity<Teacher> create(@RequestBody CreateTeacherCommand cmd) {
        Teacher t = teacherUseCase.create(new CreateTeacherCommand(
                TenantContext.get(), cmd.userId(), cmd.staffNumber(), cmd.firstName(), cmd.lastName(),
                cmd.phone(), cmd.email(), cmd.specialization(), cmd.hireDate()));
        return ResponseEntity.created(URI.create("/api/staff/teachers/" + t.getId())).body(t);
    }

    @Operation(summary = "Get a teacher by ID")
    @GetMapping("/{id}")
    public ResponseEntity<Teacher> findById(@PathVariable Long id) {
        return teacherUseCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "List all teachers for the current tenant")
    @GetMapping
    public List<Teacher> findAll() { return teacherUseCase.findByTenantId(TenantContext.get()); }

    @Operation(summary = "Update an existing teacher")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @PutMapping("/{id}")
    public Teacher update(@PathVariable Long id, @RequestBody TeacherUseCase.UpdateTeacherCommand cmd) {
        return teacherUseCase.update(id, cmd);
    }

    @Operation(summary = "Delete a teacher (forbidden if linked to evaluations — deactivate instead)")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        teacherUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Deactivate a teacher")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @PatchMapping("/{id}/deactivate")
    public Teacher deactivate(@PathVariable Long id) { return teacherUseCase.setActive(id, false); }

    @Operation(summary = "Reactivate a teacher")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @PatchMapping("/{id}/activate")
    public Teacher activate(@PathVariable Long id) { return teacherUseCase.setActive(id, true); }

    @Operation(summary = "Classes for which the teacher is the head teacher")
    @GetMapping("/{id}/classes")
    public List<ClassPublicInfo> classes(@PathVariable Long id) { return teacherUseCase.findClasses(id); }

    @Operation(summary = "Subject assignments (ClasseMatiere) of the teacher")
    @GetMapping("/{id}/subjects")
    public List<ClassSubjectPublicInfo> subjects(@PathVariable Long id) { return teacherUseCase.findSubjects(id); }

    @Operation(summary = "Consolidated timetable of the teacher")
    @GetMapping("/{id}/schedule")
    public List<ScheduleSlotPublicInfo> schedule(@PathVariable Long id) { return teacherUseCase.findSchedule(id); }
}
