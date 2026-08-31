package gn.lacaeducation.academics.adapters.in.web;

import gn.lacaeducation.academics.application.port.in.GererScheduleExceptionUseCase;
import gn.lacaeducation.academics.application.port.in.GererScheduleExceptionUseCase.EffectiveScheduleSlot;
import gn.lacaeducation.academics.application.port.in.GererScheduleExceptionUseCase.MoveScheduleForWeekCommand;
import gn.lacaeducation.academics.domain.ScheduleException;
import gn.lacaeducation.academics.domain.valueobject.JourSemaine;
import gn.lacaeducation.common.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Déplacement/annulation ponctuelle d'un créneau récurrent pour une semaine précise (§5.6) — vient
 * compléter {@link EmploiDuTempsController} sans y toucher (endpoints existants inchangés).
 */
@Tag(name = "Timetable exceptions", description = "Déplacement/annulation ponctuelle d'un créneau pour une semaine précise")
@RestController
@RequestMapping("/api/v1/academics/schedules")
@RequiredArgsConstructor
public class ScheduleExceptionController {

    private final GererScheduleExceptionUseCase useCase;

    public record MoveForWeekRequest(LocalDate weekStartDate, JourSemaine newDay, LocalTime newStartTime,
                                     LocalTime newEndTime, Long newRoomId, Long newTeacherId, String reason) {}

    public record CancelForWeekRequest(LocalDate weekStartDate, String reason) {}

    @Operation(summary = "Déplace un créneau récurrent pour une semaine précise uniquement (409 si conflit)")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @PostMapping("/{scheduleId}/move-for-week")
    public ScheduleException moveForWeek(@PathVariable Long scheduleId, @RequestBody MoveForWeekRequest req) {
        return useCase.moveForWeek(new MoveScheduleForWeekCommand(
                TenantContext.get(), scheduleId, req.weekStartDate(), req.newDay(),
                req.newStartTime(), req.newEndTime(), req.newRoomId(), req.newTeacherId(), req.reason()));
    }

    @Operation(summary = "Annule un créneau récurrent pour une semaine précise (le cours n'a pas lieu, sans déplacement)")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @PostMapping("/{scheduleId}/cancel-for-week")
    public ScheduleException cancelForWeek(@PathVariable Long scheduleId, @RequestBody CancelForWeekRequest req) {
        return useCase.cancelForWeek(scheduleId, req.weekStartDate(), req.reason());
    }

    @Operation(summary = "Supprime l'exception d'une semaine : le créneau redevient récurrent (position d'origine)")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @DeleteMapping("/{scheduleId}/week-exception")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelException(@PathVariable Long scheduleId, @RequestParam LocalDate weekStartDate) {
        useCase.cancelException(scheduleId, weekStartDate);
    }

    @Operation(summary = "Emploi du temps effectif d'une classe pour une semaine (récurrent + déplacements/annulations ponctuels)")
    @GetMapping("/by-class/{classId}/effective")
    public List<EffectiveScheduleSlot> effectiveByClass(@PathVariable Long classId, @RequestParam LocalDate weekStartDate) {
        return useCase.effectiveScheduleForClass(classId, weekStartDate);
    }

    @Operation(summary = "Emploi du temps effectif d'un enseignant pour une semaine")
    @GetMapping("/by-teacher/{teacherId}/effective")
    public List<EffectiveScheduleSlot> effectiveByTeacher(@PathVariable Long teacherId, @RequestParam LocalDate weekStartDate) {
        return useCase.effectiveScheduleForTeacher(teacherId, weekStartDate);
    }
}
