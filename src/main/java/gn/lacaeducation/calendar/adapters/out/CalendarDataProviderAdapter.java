package gn.lacaeducation.calendar.adapters.out;

import gn.lacaeducation.calendar.CalendarDataProvider;
import gn.lacaeducation.calendar.application.port.out.AcademicEventRepository;
import gn.lacaeducation.calendar.domain.AcademicEvent;
import gn.lacaeducation.calendar.domain.AcademicEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
class CalendarDataProviderAdapter implements CalendarDataProvider {

    private final AcademicEventRepository repository;

    @Override
    public List<LocalDate> holidayDates(Long tenantId, LocalDate from, LocalDate to) {
        List<LocalDate> dates = new ArrayList<>();
        for (AcademicEvent e : repository.findByTenantId(tenantId)) {
            if (e.getEventType() != AcademicEventType.HOLIDAY && e.getEventType() != AcademicEventType.VACATION) continue;
            if (e.getStartDate() == null) continue;
            LocalDate start = e.getStartDate();
            LocalDate end = e.getEndDate() != null ? e.getEndDate() : start;
            LocalDate rangeStart = start.isBefore(from) ? from : start;
            LocalDate rangeEnd = end.isAfter(to) ? to : end;
            for (LocalDate d = rangeStart; !d.isAfter(rangeEnd); d = d.plusDays(1)) {
                dates.add(d);
            }
        }
        return dates;
    }
}
