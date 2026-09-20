package gn.lacaeducation.staff.adapters.in.web;

import gn.lacaeducation.staff.application.service.TeacherSubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Teacher Subjects", description = "Matières enseignées par un enseignant (liste sur sa fiche)")
@RestController
@RequestMapping("/api/v1/staff/teachers")
@RequiredArgsConstructor
public class TeacherSubjectController {

    private final TeacherSubjectService service;

    public record AssignSubjectsRequest(List<Long> subjectIds) {}

    @Operation(summary = "Lister les identifiants de matières affectées à un enseignant")
    @GetMapping("/{teacherId}/assigned-subjects")
    public List<Long> list(@PathVariable Long teacherId) {
        return service.findSubjectIds(teacherId);
    }

    @Operation(summary = "Lister les identifiants d'enseignants affectés à une matière donnée")
    @GetMapping("/by-subject/{subjectId}")
    public List<Long> bySubject(@PathVariable Long subjectId) {
        return service.findTeacherIdsBySubject(subjectId);
    }

    @Operation(summary = "Remplacer l'ensemble des matières affectées à un enseignant")
    @PreAuthorize("hasAuthority('matiere:manage')")
    @PutMapping("/{teacherId}/assigned-subjects")
    public List<Long> set(@PathVariable Long teacherId, @RequestBody AssignSubjectsRequest r) {
        service.setSubjects(teacherId, r.subjectIds());
        return service.findSubjectIds(teacherId);
    }
}
