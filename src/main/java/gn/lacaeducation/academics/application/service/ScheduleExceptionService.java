package gn.lacaeducation.academics.application.service;

import gn.lacaeducation.academics.application.port.in.GererScheduleExceptionUseCase;
import gn.lacaeducation.academics.application.port.out.EmploiDuTempsRepository;
import gn.lacaeducation.academics.application.port.out.ScheduleExceptionRepository;
import gn.lacaeducation.academics.domain.EmploiDuTemps;
import gn.lacaeducation.academics.domain.ScheduleException;
import gn.lacaeducation.academics.domain.valueobject.JourSemaine;
import gn.lacaeducation.academics.domain.valueobject.ScheduleExceptionType;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ConflictException;
import gn.lacaeducation.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ScheduleExceptionService implements GererScheduleExceptionUseCase {

    private final EmploiDuTempsRepository edtRepository;
    private final ScheduleExceptionRepository exceptionRepository;

    @Override
    public ScheduleException moveForWeek(MoveScheduleForWeekCommand cmd) {
        EmploiDuTemps original = loadOriginal(cmd.originalScheduleId());
        requireMonday(cmd.weekStartDate());
        validateTimeRange(cmd.newStartTime(), cmd.newEndTime());

        Long effectiveRoomId = cmd.newRoomId() != null ? cmd.newRoomId() : original.getRoomId();
        Long effectiveTeacherId = cmd.newTeacherId() != null ? cmd.newTeacherId() : original.getTeacherId();

        Optional<ScheduleException> existing = exceptionRepository.findByOriginalScheduleIdAndWeek(
                cmd.originalScheduleId(), cmd.weekStartDate());
        Long excludeExceptionId = existing.map(ScheduleException::getId).orElse(null);

        checkConflictsForWeek(effectiveRoomId, effectiveTeacherId, original.getSchoolClassId(),
                cmd.weekStartDate(), cmd.newDay(), cmd.newStartTime(), cmd.newEndTime(),
                cmd.originalScheduleId(), excludeExceptionId);

        ScheduleException toSave = existing.orElseGet(() -> ScheduleException.builder()
                .tenantId(cmd.tenantId())
                .originalScheduleId(cmd.originalScheduleId())
                .weekStartDate(cmd.weekStartDate())
                .build());
        toSave.setType(ScheduleExceptionType.MOVED);
        toSave.setNewDay(cmd.newDay());
        toSave.setNewStartTime(cmd.newStartTime());
        toSave.setNewEndTime(cmd.newEndTime());
        toSave.setNewRoomId(cmd.newRoomId());
        toSave.setNewTeacherId(cmd.newTeacherId());
        toSave.setReason(cmd.reason());
        return exceptionRepository.save(toSave);
    }

    @Override
    public ScheduleException cancelForWeek(Long originalScheduleId, LocalDate weekStartDate, String reason) {
        EmploiDuTemps original = loadOriginal(originalScheduleId);
        requireMonday(weekStartDate);
        Optional<ScheduleException> existing = exceptionRepository.findByOriginalScheduleIdAndWeek(originalScheduleId, weekStartDate);
        ScheduleException toSave = existing.orElseGet(() -> ScheduleException.builder()
                .tenantId(original.getTenantId())
                .originalScheduleId(originalScheduleId)
                .weekStartDate(weekStartDate)
                .build());
        toSave.setType(ScheduleExceptionType.CANCELLED);
        toSave.setNewDay(null);
        toSave.setNewStartTime(null);
        toSave.setNewEndTime(null);
        toSave.setNewRoomId(null);
        toSave.setNewTeacherId(null);
        toSave.setReason(reason);
        return exceptionRepository.save(toSave);
    }

    @Override
    public void cancelException(Long originalScheduleId, LocalDate weekStartDate) {
        exceptionRepository.deleteByOriginalScheduleIdAndWeek(originalScheduleId, weekStartDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EffectiveScheduleSlot> effectiveScheduleForClass(Long classId, LocalDate weekStartDate) {
        requireMonday(weekStartDate);
        List<EmploiDuTemps> recurring = edtRepository.findByClassId(classId);
        if (recurring.isEmpty()) return List.of();
        Map<Long, ScheduleException> byScheduleId = exceptionRepository.findByWeek(weekStartDate).stream()
                .filter(e -> recurring.stream().anyMatch(r -> r.getId().equals(e.getOriginalScheduleId())))
                .collect(Collectors.toMap(ScheduleException::getOriginalScheduleId, e -> e));
        return recurring.stream().map(r -> toEffectiveSlot(r, byScheduleId.get(r.getId()))).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EffectiveScheduleSlot> effectiveScheduleForTeacher(Long teacherId, LocalDate weekStartDate) {
        requireMonday(weekStartDate);
        List<EmploiDuTemps> ownRecurring = edtRepository.findByTeacherId(teacherId);
        List<ScheduleException> weekExceptions = exceptionRepository.findByWeek(weekStartDate);
        Map<Long, ScheduleException> ownExceptions = weekExceptions.stream()
                .filter(e -> ownRecurring.stream().anyMatch(r -> r.getId().equals(e.getOriginalScheduleId())))
                .collect(Collectors.toMap(ScheduleException::getOriginalScheduleId, e -> e));

        List<EffectiveScheduleSlot> result = new ArrayList<>();
        for (EmploiDuTemps r : ownRecurring) {
            ScheduleException ex = ownExceptions.get(r.getId());
            boolean movedToAnotherTeacher = ex != null && ex.getType() == ScheduleExceptionType.MOVED
                    && ex.getNewTeacherId() != null && !ex.getNewTeacherId().equals(teacherId);
            if (movedToAnotherTeacher) continue; // ne compte plus chez lui cette semaine
            result.add(toEffectiveSlot(r, ex));
        }
        // Créneaux d'autres classes déplacés VERS cet enseignant cette semaine.
        for (ScheduleException ex : weekExceptions) {
            if (ex.getType() != ScheduleExceptionType.MOVED) continue;
            if (!teacherId.equals(ex.getNewTeacherId())) continue;
            boolean alreadyHis = ownRecurring.stream().anyMatch(r -> r.getId().equals(ex.getOriginalScheduleId()));
            if (alreadyHis) continue;
            edtRepository.findById(ex.getOriginalScheduleId()).ifPresent(original -> result.add(toEffectiveSlot(original, ex)));
        }
        return result;
    }

    // ── Détection de conflit pour une semaine précise ─────────────────────────
    private void checkConflictsForWeek(Long roomId, Long teacherId, Long classId, LocalDate week,
                                       JourSemaine day, LocalTime start, LocalTime end,
                                       Long excludeScheduleId, Long excludeExceptionId) {
        List<ScheduleException> weekExceptions = exceptionRepository.findByWeek(week);
        Set<Long> movedAwayIds = weekExceptions.stream()
                .filter(e -> excludeExceptionId == null || !e.getId().equals(excludeExceptionId))
                .map(ScheduleException::getOriginalScheduleId)
                .collect(Collectors.toSet());
        movedAwayIds.add(excludeScheduleId); // ne sera pas à sa position récurrente cette semaine

        if (roomId != null) {
            boolean conflict = edtRepository.search(null, null, roomId, day).stream()
                    .filter(s -> !movedAwayIds.contains(s.getId()))
                    .anyMatch(s -> overlaps(s.getStartTime(), s.getEndTime(), start, end));
            if (conflict) {
                throw new ConflictException("Cette salle est déjà occupée le " + dayLabel(day)
                        + " de " + hhmm(start) + " à " + hhmm(end) + " cette semaine-là.");
            }
        }
        if (teacherId != null) {
            boolean conflict = edtRepository.search(null, teacherId, null, day).stream()
                    .filter(s -> !movedAwayIds.contains(s.getId()))
                    .anyMatch(s -> overlaps(s.getStartTime(), s.getEndTime(), start, end));
            if (conflict) {
                throw new ConflictException("Cet enseignant a déjà un cours le " + dayLabel(day)
                        + " de " + hhmm(start) + " à " + hhmm(end) + " cette semaine-là.");
            }
        }
        if (classId != null) {
            boolean conflict = edtRepository.search(classId, null, null, day).stream()
                    .filter(s -> !movedAwayIds.contains(s.getId()))
                    .anyMatch(s -> overlaps(s.getStartTime(), s.getEndTime(), start, end));
            if (conflict) {
                throw new ConflictException("Cette classe a déjà un cours le " + dayLabel(day)
                        + " de " + hhmm(start) + " à " + hhmm(end) + " cette semaine-là.");
            }
        }

        for (ScheduleException e : weekExceptions) {
            if (e.getType() != ScheduleExceptionType.MOVED) continue;
            if (excludeExceptionId != null && e.getId().equals(excludeExceptionId)) continue;
            if (!day.equals(e.getNewDay()) || !overlaps(e.getNewStartTime(), e.getNewEndTime(), start, end)) continue;
            EmploiDuTemps exOriginal = edtRepository.findById(e.getOriginalScheduleId()).orElse(null);
            if (exOriginal == null) continue;
            Long exRoomId = e.getNewRoomId() != null ? e.getNewRoomId() : exOriginal.getRoomId();
            Long exTeacherId = e.getNewTeacherId() != null ? e.getNewTeacherId() : exOriginal.getTeacherId();
            boolean overlapConflict = (roomId != null && roomId.equals(exRoomId))
                    || (teacherId != null && teacherId.equals(exTeacherId))
                    || (classId != null && classId.equals(exOriginal.getSchoolClassId()));
            if (overlapConflict) {
                throw new ConflictException("Un autre créneau est déjà déplacé le " + dayLabel(day)
                        + " de " + hhmm(start) + " à " + hhmm(end) + " cette semaine-là (salle/enseignant/classe déjà occupé).");
            }
        }
    }

    private static boolean overlaps(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
        return aStart.isBefore(bEnd) && aEnd.isAfter(bStart);
    }

    private EffectiveScheduleSlot toEffectiveSlot(EmploiDuTemps recurring, ScheduleException exception) {
        if (exception == null) {
            return new EffectiveScheduleSlot(recurring.getId(), recurring.getSchoolClassId(), recurring.getSubjectId(),
                    recurring.getTeacherId(), recurring.getRoomId(), recurring.getDay(), recurring.getStartTime(), recurring.getEndTime(),
                    false, false, null, null, null, recurring.getLabel());
        }
        if (exception.getType() == ScheduleExceptionType.CANCELLED) {
            return new EffectiveScheduleSlot(recurring.getId(), recurring.getSchoolClassId(), recurring.getSubjectId(),
                    recurring.getTeacherId(), recurring.getRoomId(), recurring.getDay(), recurring.getStartTime(), recurring.getEndTime(),
                    false, true, recurring.getDay(), recurring.getStartTime(), recurring.getEndTime(), recurring.getLabel());
        }
        Long effRoom = exception.getNewRoomId() != null ? exception.getNewRoomId() : recurring.getRoomId();
        Long effTeacher = exception.getNewTeacherId() != null ? exception.getNewTeacherId() : recurring.getTeacherId();
        return new EffectiveScheduleSlot(recurring.getId(), recurring.getSchoolClassId(), recurring.getSubjectId(),
                effTeacher, effRoom, exception.getNewDay(), exception.getNewStartTime(), exception.getNewEndTime(),
                true, false, recurring.getDay(), recurring.getStartTime(), recurring.getEndTime(), recurring.getLabel());
    }

    private EmploiDuTemps loadOriginal(Long id) {
        return edtRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Schedule", id));
    }

    private static void requireMonday(LocalDate weekStartDate) {
        if (weekStartDate == null || weekStartDate.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new BusinessRuleException("weekStartDate doit être un lundi (début de semaine).");
        }
    }

    private static void validateTimeRange(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new BusinessRuleException("Invalid time range: start time must be before end time.");
        }
    }

    private static String hhmm(LocalTime t) {
        return t == null ? "-" : String.format("%02d:%02d", t.getHour(), t.getMinute());
    }

    private static String dayLabel(JourSemaine day) {
        if (day == null) return "-";
        return switch (day) {
            case LUNDI -> "lundi";
            case MARDI -> "mardi";
            case MERCREDI -> "mercredi";
            case JEUDI -> "jeudi";
            case VENDREDI -> "vendredi";
            case SAMEDI -> "samedi";
        };
    }
}
