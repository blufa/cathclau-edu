package gn.lacaeducation.academics.application.port.in;

import gn.lacaeducation.academics.domain.SchoolEvent;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface GererSchoolEventUseCase {
    record CreateSchoolEventCommand(Long tenantId, String label, LocalDate startDate, LocalDate endDate,
                                    LocalTime startTime, LocalTime endTime, String reason) {}

    SchoolEvent create(CreateSchoolEventCommand command);
    List<SchoolEvent> findByTenant(Long tenantId);
    void delete(Long id);
}
