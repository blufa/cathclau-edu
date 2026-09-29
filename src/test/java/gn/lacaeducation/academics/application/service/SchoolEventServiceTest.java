package gn.lacaeducation.academics.application.service;

import gn.lacaeducation.academics.application.port.in.GererSchoolEventUseCase.CreateSchoolEventCommand;
import gn.lacaeducation.academics.application.port.in.GererSchoolEventUseCase.TouchedTeacher;
import gn.lacaeducation.academics.application.port.out.EmploiDuTempsRepository;
import gn.lacaeducation.academics.application.port.out.SchoolEventRepository;
import gn.lacaeducation.academics.domain.EmploiDuTemps;
import gn.lacaeducation.academics.domain.SchoolEvent;
import gn.lacaeducation.academics.domain.SchoolEventType;
import gn.lacaeducation.academics.domain.valueobject.JourSemaine;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolEventServiceTest {

    @Mock SchoolEventRepository repo;
    @Mock EmploiDuTempsRepository emploiDuTempsRepo;
    @InjectMocks SchoolEventService service;

    @Test
    void create_valid_savesEvent() {
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        SchoolEvent e = service.create(new CreateSchoolEventCommand(1L, "Kermesse", LocalDate.of(2026, 5, 10),
                LocalDate.of(2026, 5, 10), LocalTime.of(14, 0), LocalTime.of(18, 0), "Kermesse annuelle", null));

        assertThat(e.getLabel()).isEqualTo("Kermesse");
        assertThat(e.getStartTime()).isEqualTo(LocalTime.of(14, 0));
        assertThat(e.getType()).isEqualTo(SchoolEventType.OTHER);
    }

    @Test
    void create_holiday_savesWithHolidayType() {
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        SchoolEvent e = service.create(new CreateSchoolEventCommand(1L, "Fête de l'indépendance",
                LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 2), null, null, null, SchoolEventType.HOLIDAY));

        assertThat(e.getType()).isEqualTo(SchoolEventType.HOLIDAY);
    }

    @Test
    void create_blankLabel_throws() {
        assertThatThrownBy(() -> service.create(new CreateSchoolEventCommand(1L, "  ",
                LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 10), null, null, null, null)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void create_endDateBeforeStartDate_throws() {
        assertThatThrownBy(() -> service.create(new CreateSchoolEventCommand(1L, "Kermesse",
                LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 9), null, null, null, null)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void create_endTimeBeforeStartTime_throws() {
        assertThatThrownBy(() -> service.create(new CreateSchoolEventCommand(1L, "Kermesse",
                LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 10), LocalTime.of(18, 0), LocalTime.of(14, 0), null, null)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void create_noTimes_wholeDayEvent_isValid() {
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        SchoolEvent e = service.create(new CreateSchoolEventCommand(1L, "Journée sportive",
                LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 10), null, null, null, null));

        assertThat(e.getStartTime()).isNull();
        assertThat(e.getEndTime()).isNull();
    }

    @Test
    void findTouchedTeachers_slotOverlapsEventTime_isTouched() {
        // Événement lundi 13h-18h ; créneau lundi 12h-14h chevauche (12h-14h touche 13h-18h) -> touché.
        SchoolEvent event = SchoolEvent.builder().id(5L).tenantId(1L).label("Kermesse")
                .startDate(LocalDate.of(2026, 5, 4)).endDate(LocalDate.of(2026, 5, 4))
                .startTime(LocalTime.of(13, 0)).endTime(LocalTime.of(18, 0)).type(SchoolEventType.OTHER).build();
        when(repo.findById(5L)).thenReturn(Optional.of(event));
        when(repo.findCreditedTeacherIds(5L)).thenReturn(List.of());
        EmploiDuTemps touchedSlot = EmploiDuTemps.builder().id(1L).teacherId(20L).day(JourSemaine.LUNDI)
                .startTime(LocalTime.of(12, 0)).endTime(LocalTime.of(14, 0)).build();
        EmploiDuTemps untouchedSlot = EmploiDuTemps.builder().id(2L).teacherId(21L).day(JourSemaine.LUNDI)
                .startTime(LocalTime.of(8, 0)).endTime(LocalTime.of(10, 0)).build();
        when(emploiDuTempsRepo.search(null, null, null, null)).thenReturn(List.of(touchedSlot, untouchedSlot));

        List<TouchedTeacher> result = service.findTouchedTeachers(5L);

        assertThat(result).extracting(TouchedTeacher::teacherId).containsExactly(20L);
        assertThat(result.get(0).credited()).isFalse();
    }

    @Test
    void findTouchedTeachers_alreadyCredited_reportsCreditedTrue() {
        SchoolEvent event = SchoolEvent.builder().id(5L).tenantId(1L).label("Kermesse")
                .startDate(LocalDate.of(2026, 5, 4)).endDate(LocalDate.of(2026, 5, 4)).type(SchoolEventType.OTHER).build();
        when(repo.findById(5L)).thenReturn(Optional.of(event));
        when(repo.findCreditedTeacherIds(5L)).thenReturn(List.of(20L));
        EmploiDuTemps slot = EmploiDuTemps.builder().id(1L).teacherId(20L).day(JourSemaine.LUNDI)
                .startTime(LocalTime.of(8, 0)).endTime(LocalTime.of(10, 0)).build();
        when(emploiDuTempsRepo.search(null, null, null, null)).thenReturn(List.of(slot));

        List<TouchedTeacher> result = service.findTouchedTeachers(5L);

        assertThat(result).containsExactly(new TouchedTeacher(20L, true));
    }

    @Test
    void setCreditedTeachers_otherEvent_replacesCreditedList() {
        SchoolEvent event = SchoolEvent.builder().id(5L).type(SchoolEventType.OTHER).build();
        when(repo.findById(5L)).thenReturn(Optional.of(event));

        service.setCreditedTeachers(5L, List.of(20L, 21L));

        verify(repo).replaceCreditedTeachers(5L, List.of(20L, 21L));
    }

    @Test
    void setCreditedTeachers_holidayEvent_throws() {
        SchoolEvent event = SchoolEvent.builder().id(5L).type(SchoolEventType.HOLIDAY).build();
        when(repo.findById(5L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.setCreditedTeachers(5L, List.of(20L)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void findTouchedTeachers_unknownEvent_throwsNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findTouchedTeachers(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
