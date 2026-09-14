package gn.lacaeducation.calendar.application.service;

import gn.lacaeducation.calendar.application.event.AcademicEventCreatedEvent;
import gn.lacaeducation.calendar.application.port.out.AcademicEventRepository;
import gn.lacaeducation.calendar.application.service.AcademicEventService.EventCommand;
import gn.lacaeducation.calendar.domain.AcademicEvent;
import gn.lacaeducation.calendar.domain.AcademicEventType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcademicEventServiceTest {

    @Mock AcademicEventRepository repo;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks AcademicEventService service;

    private EventCommand cmd() {
        return new EventCommand(9L, 1L, "Réunion parents", AcademicEventType.MEETING,
                LocalDate.of(2026, 11, 5), null, "Salle polyvalente");
    }

    @Test
    void create_publishesAcademicEventCreatedEvent() {
        when(repo.save(any())).thenAnswer(i -> {
            AcademicEvent e = i.getArgument(0);
            e.setId(42L);
            return e;
        });

        service.create(cmd());

        ArgumentCaptor<AcademicEventCreatedEvent> captor = ArgumentCaptor.forClass(AcademicEventCreatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        AcademicEventCreatedEvent event = captor.getValue();
        assertThat(event.eventId()).isEqualTo(42L);
        assertThat(event.tenantId()).isEqualTo(9L);
        assertThat(event.title()).isEqualTo("Réunion parents");
        assertThat(event.eventType()).isEqualTo("MEETING");
        assertThat(event.startDate()).isEqualTo(LocalDate.of(2026, 11, 5));
    }

    @Test
    void update_doesNotPublishEvent() {
        AcademicEvent existing = AcademicEvent.builder().id(42L).tenantId(9L).build();
        when(repo.findById(42L)).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.update(42L, cmd());

        verify(eventPublisher, never()).publishEvent(any());
    }
}
