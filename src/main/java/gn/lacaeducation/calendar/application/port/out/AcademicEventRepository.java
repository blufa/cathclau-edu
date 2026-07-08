package gn.lacaeducation.calendar.application.port.out;

import gn.lacaeducation.calendar.domain.AcademicEvent;

import java.util.List;
import java.util.Optional;

public interface AcademicEventRepository {
    AcademicEvent save(AcademicEvent e);
    Optional<AcademicEvent> findById(Long id);
    List<AcademicEvent> findByTenantId(Long tenantId);
    List<AcademicEvent> findByTenantIdAndAcademicYearId(Long tenantId, Long academicYearId);
    void deleteById(Long id);
}
