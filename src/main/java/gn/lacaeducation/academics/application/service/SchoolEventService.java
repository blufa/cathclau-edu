package gn.lacaeducation.academics.application.service;

import gn.lacaeducation.academics.application.port.in.GererSchoolEventUseCase;
import gn.lacaeducation.academics.application.port.out.SchoolEventRepository;
import gn.lacaeducation.academics.domain.SchoolEvent;
import gn.lacaeducation.common.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class SchoolEventService implements GererSchoolEventUseCase {

    private final SchoolEventRepository repo;

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
}
