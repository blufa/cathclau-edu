package gn.lacaeducation.academics.application.service;

import gn.lacaeducation.academics.application.port.in.GererSchoolEventUseCase;
import gn.lacaeducation.academics.application.port.out.EmploiDuTempsRepository;
import gn.lacaeducation.academics.application.port.out.SchoolEventRepository;
import gn.lacaeducation.academics.domain.EmploiDuTemps;
import gn.lacaeducation.academics.domain.SchoolEvent;
import gn.lacaeducation.academics.domain.valueobject.JourSemaine;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
public class SchoolEventService implements GererSchoolEventUseCase {

    private static final Map<DayOfWeek, JourSemaine> DAY_OF_WEEK = Map.of(
            DayOfWeek.MONDAY, JourSemaine.LUNDI, DayOfWeek.TUESDAY, JourSemaine.MARDI,
            DayOfWeek.WEDNESDAY, JourSemaine.MERCREDI, DayOfWeek.THURSDAY, JourSemaine.JEUDI,
            DayOfWeek.FRIDAY, JourSemaine.VENDREDI, DayOfWeek.SATURDAY, JourSemaine.SAMEDI);

    private final SchoolEventRepository repo;
    private final EmploiDuTempsRepository emploiDuTempsRepo;

    @Override
    public SchoolEvent create(CreateSchoolEventCommand c) {
        if (c.label() == null || c.label().isBlank()) {
            throw new BusinessRuleException("Le libellé de l'événement est requis.");
        }
        if (c.startDate() == null || c.endDate() == null) {
            throw new BusinessRuleException("Les dates de début et de fin sont requises.");
        }
        if (c.endDate().isBefore(c.startDate())) {
            throw new BusinessRuleException("La date de fin doit être postérieure ou égale à la date de début.");
        }
        if (c.startTime() != null && c.endTime() != null && !c.startTime().isBefore(c.endTime())) {
            throw new BusinessRuleException("L'heure de fin doit être postérieure à l'heure de début.");
        }
        return repo.save(SchoolEvent.builder()
                .tenantId(c.tenantId()).label(c.label()).startDate(c.startDate()).endDate(c.endDate())
                .startTime(c.startTime()).endTime(c.endTime()).reason(c.reason())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchoolEvent> findByTenant(Long tenantId) {
        return repo.findByTenantId(tenantId);
    }

    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TouchedTeacher> findTouchedTeachers(Long schoolEventId) {
        SchoolEvent event = repo.findById(schoolEventId)
                .orElseThrow(() -> new ResourceNotFoundException("Événement école", schoolEventId));
        Set<Long> credited = Set.copyOf(repo.findCreditedTeacherIds(schoolEventId));
        LocalTime eventStart = event.getStartTime() != null ? event.getStartTime() : LocalTime.MIN;
        LocalTime eventEnd = event.getEndTime() != null ? event.getEndTime() : LocalTime.MAX;
        Set<Long> touched = new java.util.LinkedHashSet<>();
        for (EmploiDuTemps slot : emploiDuTempsRepo.search(null, null, null, null)) {
            if (slot.getTeacherId() == null || slot.getDay() == null
                    || slot.getStartTime() == null || slot.getEndTime() == null) continue;
            if (!slotOverlapsEvent(slot, event, eventStart, eventEnd)) continue;
            touched.add(slot.getTeacherId());
        }
        return touched.stream().map(id -> new TouchedTeacher(id, credited.contains(id))).toList();
    }

    private boolean slotOverlapsEvent(EmploiDuTemps slot, SchoolEvent event, LocalTime eventStart, LocalTime eventEnd) {
        if (!slot.getStartTime().isBefore(eventEnd) || !eventStart.isBefore(slot.getEndTime())) return false;
        for (LocalDate d = event.getStartDate(); !d.isAfter(event.getEndDate()); d = d.plusDays(1)) {
            if (DAY_OF_WEEK.get(d.getDayOfWeek()) == slot.getDay()) return true;
        }
        return false;
    }

    @Override
    public void setCreditedTeachers(Long schoolEventId, List<Long> teacherIds) {
        if (repo.findById(schoolEventId).isEmpty()) {
            throw new ResourceNotFoundException("Événement école", schoolEventId);
        }
        repo.replaceCreditedTeachers(schoolEventId, teacherIds == null ? List.of() : teacherIds);
    }
}
