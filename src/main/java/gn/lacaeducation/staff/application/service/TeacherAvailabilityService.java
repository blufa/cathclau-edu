package gn.lacaeducation.staff.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.staff.application.port.out.TeacherAvailabilityRepository;
import gn.lacaeducation.staff.domain.TeacherAvailability;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

/** Gestion des disponibilités d'un enseignant (jour + plage horaire). */
@Service
@Transactional
@RequiredArgsConstructor
public class TeacherAvailabilityService {

    private final TeacherAvailabilityRepository repository;

    @Transactional(readOnly = true)
    public List<TeacherAvailability> findByTeacher(Long teacherId) {
        return repository.findByTeacherId(teacherId);
    }

    public TeacherAvailability add(Long teacherId, String day, LocalTime start, LocalTime end) {
        if (day == null || day.isBlank()) {
            throw new BusinessRuleException("Jour requis");
        }
        if (start == null || end == null || !start.isBefore(end)) {
            throw new BusinessRuleException("Plage horaire invalide : l'heure de début doit précéder l'heure de fin");
        }
        // Conflit : la plage chevauche une disponibilité déjà saisie pour cet enseignant le même jour.
        boolean overlap = repository.findByTeacherId(teacherId).stream()
                .filter(a -> day.equals(a.getDay()))
                .anyMatch(a -> start.isBefore(a.getEndTime()) && a.getStartTime().isBefore(end));
        if (overlap) {
            throw new BusinessRuleException("Cette plage chevauche une disponibilité déjà enregistrée ce jour ("
                    + day + ") : l'heure est déjà prise.");
        }
        return repository.save(TeacherAvailability.builder()
                .tenantId(TenantContext.get())
                .teacherId(teacherId)
                .day(day)
                .startTime(start)
                .endTime(end)
                .build());
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
