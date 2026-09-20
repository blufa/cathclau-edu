package gn.lacaeducation.academics.application.port.out;

import gn.lacaeducation.academics.domain.SchoolEvent;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SchoolEventRepository {
    SchoolEvent save(SchoolEvent event);
    Optional<SchoolEvent> findById(Long id);
    List<SchoolEvent> findByTenantId(Long tenantId);
    /** Événements du tenant dont la période [startDate, endDate] chevauche [from, to]. */
    List<SchoolEvent> findByTenantIdAndDateRangeOverlap(Long tenantId, LocalDate from, LocalDate to);
    void deleteById(Long id);
}
