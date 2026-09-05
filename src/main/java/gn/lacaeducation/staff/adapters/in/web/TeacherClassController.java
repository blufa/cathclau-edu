package gn.lacaeducation.staff.adapters.in.web;

import gn.lacaeducation.staff.application.service.TeacherClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Teacher Classes", description = "Classes enseignées par un enseignant (checklist sur sa fiche)")
@RestController
@RequestMapping("/api/v1/staff/teachers")
@RequiredArgsConstructor
public class TeacherClassController {

    private final TeacherClassService service;

    public record AssignClassesRequest(List<Long> classIds) {}

    @Operation(summary = "List the class ids a teacher is assigned to")
    @GetMapping("/{teacherId}/assigned-classes")
    public List<Long> list(@PathVariable Long teacherId) {
        return service.findClassIds(teacherId);
    }

    @Operation(summary = "Replace the set of classes a teacher is assigned to")
    @PreAuthorize("hasAuthority('classe:manage')")
    @PutMapping("/{teacherId}/assigned-classes")
    public List<Long> set(@PathVariable Long teacherId, @RequestBody AssignClassesRequest r) {
        service.setClasses(teacherId, r.classIds());
        return service.findClassIds(teacherId);
    }
}
