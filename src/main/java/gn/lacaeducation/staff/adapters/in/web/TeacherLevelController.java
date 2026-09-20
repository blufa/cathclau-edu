package gn.lacaeducation.staff.adapters.in.web;

import gn.lacaeducation.staff.application.service.TeacherLevelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Teacher Levels", description = "Niveaux (cycles) où un enseignant est habilité à enseigner")
@RestController
@RequestMapping("/api/v1/staff/teachers")
@RequiredArgsConstructor
public class TeacherLevelController {

    private final TeacherLevelService service;

    public record AssignLevelsRequest(List<Long> levelIds) {}

    @Operation(summary = "Lister les identifiants de niveaux affectés à un enseignant")
    @GetMapping("/{teacherId}/assigned-levels")
    public List<Long> list(@PathVariable Long teacherId) {
        return service.findLevelIds(teacherId);
    }

    @Operation(summary = "Lister les identifiants d'enseignants affectés à un niveau donné")
    @GetMapping("/by-level/{levelId}")
    public List<Long> byLevel(@PathVariable Long levelId) {
        return service.findTeacherIdsByLevel(levelId);
    }

    @Operation(summary = "Remplacer l'ensemble des niveaux affectés à un enseignant")
    @PreAuthorize("hasAuthority('niveau:manage')")
    @PutMapping("/{teacherId}/assigned-levels")
    public List<Long> set(@PathVariable Long teacherId, @RequestBody AssignLevelsRequest r) {
        service.setLevels(teacherId, r.levelIds());
        return service.findLevelIds(teacherId);
    }
}
