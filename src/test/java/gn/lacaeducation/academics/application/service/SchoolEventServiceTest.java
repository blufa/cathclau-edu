package gn.lacaeducation.academics.application.service;

import gn.lacaeducation.academics.application.port.in.GererSchoolEventUseCase.CreateSchoolEventCommand;
import gn.lacaeducation.academics.application.port.out.SchoolEventRepository;
import gn.lacaeducation.academics.domain.SchoolEvent;
import gn.lacaeducation.common.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolEventServiceTest {

    @Mock SchoolEventRepository repo;
    @InjectMocks SchoolEventService service;

    @Test
    void create_valid_savesEvent() {
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        SchoolEvent e = service.create(new CreateSchoolEventCommand(1L, "Kermesse", LocalDate.of(2026, 5, 10),
                LocalDate.of(2026, 5, 10), LocalTime.of(14, 0), LocalTime.of(18, 0), "Kermesse annuelle"));

        assertThat(e.getLabel()).isEqualTo("Kermesse");
        assertThat(e.getStartTime()).isEqualTo(LocalTime.of(14, 0));
    }

    @Test
    void create_blankLabel_throws() {
        assertThatThrownBy(() -> service.create(new CreateSchoolEventCommand(1L, "  ",
                LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 10), null, null, null)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void create_endDateBeforeStartDate_throws() {
        assertThatThrownBy(() -> service.create(new CreateSchoolEventCommand(1L, "Kermesse",
                LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 9), null, null, null)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void create_endTimeBeforeStartTime_throws() {
        assertThatThrownBy(() -> service.create(new CreateSchoolEventCommand(1L, "Kermesse",
                LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 10), LocalTime.of(18, 0), LocalTime.of(14, 0), null)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void create_noTimes_wholeDayEvent_isValid() {
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        SchoolEvent e = service.create(new CreateSchoolEventCommand(1L, "Journée sportive",
                LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 10), null, null, null));

        assertThat(e.getStartTime()).isNull();
        assertThat(e.getEndTime()).isNull();
    }
}
