package gn.lacaeducation.academics.adapters.in.web;

import gn.lacaeducation.academics.application.port.in.GererEmploiDuTempsUseCase;
import gn.lacaeducation.academics.application.port.in.GererEmploiDuTempsUseCase.CreateScheduleCommand;
import gn.lacaeducation.academics.domain.EmploiDuTemps;
import gn.lacaeducation.academics.domain.valueobject.JourSemaine;
import gn.lacaeducation.common.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@Tag(name = "Timetable", description = "Manage class schedules (timetable) with conflict detection")
@RestController
@RequestMapping("/api/v1/academics/schedules")
@RequiredArgsConstructor
public class EmploiDuTempsController {

    private final GererEmploiDuTempsUseCase useCase;

    @Operation(summary = "Create a timetable slot (rejects room/teacher/class conflicts with 409)")
    @PostMapping
    @PreAuthorize("hasAuthority('emploi:manage')")
    public ResponseEntity<EmploiDuTemps> create(@Valid @RequestBody CreateScheduleCommand cmd) {
        EmploiDuTemps e = useCase.create(new CreateScheduleCommand(
                TenantContext.get(), cmd.schoolClassId(), cmd.subjectId(), cmd.teacherId(),
                cmd.roomId(), cmd.day(), cmd.startTime(), cmd.endTime()));
        return ResponseEntity.created(URI.create("/api/v1/academics/schedules/" + e.getId())).body(e);
    }

    @Operation(summary = "Search timetable slots by class, teacher, room and/or day")
    @GetMapping
    public List<EmploiDuTemps> search(@RequestParam(required = false) Long classId,
                                      @RequestParam(required = false) Long teacherId,
                                      @RequestParam(required = false) Long roomId,
                                      @RequestParam(required = false) JourSemaine day) {
        return useCase.search(classId, teacherId, roomId, day);
    }

    @Operation(summary = "Get a timetable slot by ID")
    @GetMapping("/{id}")
    public ResponseEntity<EmploiDuTemps> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Update a timetable slot (re-checks conflicts)")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('emploi:manage')")
    public EmploiDuTemps update(@PathVariable Long id, @Valid @RequestBody CreateScheduleCommand cmd) {
        return useCase.update(id, new CreateScheduleCommand(
                TenantContext.get(), cmd.schoolClassId(), cmd.subjectId(), cmd.teacherId(),
                cmd.roomId(), cmd.day(), cmd.startTime(), cmd.endTime()));
    }

    @Operation(summary = "Delete a timetable slot")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('emploi:manage')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { useCase.delete(id); }

    @Operation(summary = "Get the timetable of a class")
    @GetMapping("/by-class/{classId}")
    public List<EmploiDuTemps> byClass(@PathVariable Long classId) {
        return useCase.findByClass(classId);
    }

    @Operation(summary = "Get the timetable of a teacher")
    @GetMapping("/by-teacher/{teacherId}")
    public List<EmploiDuTemps> byTeacher(@PathVariable Long teacherId) {
        return useCase.findByTeacher(teacherId);
    }
}
