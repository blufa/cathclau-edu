package gn.lacaeducation.calendar.application.service;

import gn.lacaeducation.calendar.application.port.out.AcademicEventRepository;
import gn.lacaeducation.calendar.domain.AcademicEvent;
import gn.lacaeducation.calendar.domain.AcademicEventType;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Calendrier académique : jours fériés, vacances et dates importantes (compositions, réunions,
 * échéances…) d'une école privée secondaire, rattachés à une année scolaire (par identifiant — sans
 * dupliquer les années ni les périodes/trimestres existants).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AcademicEventService {

    private final AcademicEventRepository repo;

    public record EventCommand(Long tenantId, Long academicYearId, String title, AcademicEventType eventType,
                               LocalDate startDate, LocalDate endDate, String description) {}

    public AcademicEvent create(EventCommand c) {
        validate(c);
        return repo.save(AcademicEvent.builder()
                .tenantId(c.tenantId()).academicYearId(c.academicYearId()).title(c.title())
                .eventType(c.eventType() != null ? c.eventType() : AcademicEventType.OTHER)
                .startDate(c.startDate()).endDate(c.endDate()).description(c.description())
                .createdAt(LocalDateTime.now()).build());
    }

    public AcademicEvent update(Long id, EventCommand c) {
        validate(c);
        AcademicEvent e = load(id);
        e.setAcademicYearId(c.academicYearId()); e.setTitle(c.title()); e.setEventType(c.eventType());
        e.setStartDate(c.startDate()); e.setEndDate(c.endDate()); e.setDescription(c.description());
        return repo.save(e);
    }

    public void delete(Long id) { repo.deleteById(id); }

    @Transactional(readOnly = true)
    public List<AcademicEvent> find(Long tenantId, Long academicYearId) {
        return academicYearId != null
                ? repo.findByTenantIdAndAcademicYearId(tenantId, academicYearId)
                : repo.findByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public AcademicEvent findById(Long id) { return load(id); }

    private void validate(EventCommand c) {
        if (c.academicYearId() == null) throw new BusinessRuleException("L'année scolaire est requise");
        if (c.title() == null || c.title().isBlank()) throw new BusinessRuleException("Le titre est requis");
        if (c.startDate() == null) throw new BusinessRuleException("La date de début est requise");
        if (c.endDate() != null && c.endDate().isBefore(c.startDate())) {
            throw new BusinessRuleException("La date de fin doit être postérieure à la date de début");
        }
    }

    private AcademicEvent load(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("AcademicEvent", id));
    }
}
