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
    public List<LocalDate> paidHolidayDates(Long tenantId, LocalDate from, LocalDate to) {
        return datesOfType(tenantId, from, to, AcademicEventType.HOLIDAY);
    }

    @Override
    public List<LocalDate> vacationDates(Long tenantId, LocalDate from, LocalDate to) {
        return datesOfType(tenantId, from, to, AcademicEventType.VACATION);
    }

    private List<LocalDate> datesOfType(Long tenantId, LocalDate from, LocalDate to, AcademicEventType type) {
        List<LocalDate> dates = new ArrayList<>();
        for (AcademicEvent e : repository.findByTenantId(tenantId)) {
            if (e.getEventType() != type) continue;
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
