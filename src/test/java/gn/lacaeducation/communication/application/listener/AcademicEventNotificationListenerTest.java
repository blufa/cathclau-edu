package gn.lacaeducation.communication.application.listener;

import gn.lacaeducation.calendar.application.event.AcademicEventCreatedEvent;
import gn.lacaeducation.communication.application.port.in.BroadcastUseCase;
import gn.lacaeducation.communication.application.port.in.BroadcastUseCase.BroadcastCommand;
import gn.lacaeducation.communication.domain.BroadcastAudience;
import gn.lacaeducation.communication.domain.CommunicationChannel;
import gn.lacaeducation.communication.domain.CommunicationReason;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AcademicEventNotificationListenerTest {

    @Mock BroadcastUseCase broadcastUseCase;

    private AcademicEventNotificationListener listener() {
        return new AcademicEventNotificationListener(broadcastUseCase);
    }

    @Test
    void onAcademicEventCreated_broadcastsToAllParents() {
        AcademicEventCreatedEvent event = new AcademicEventCreatedEvent(9L, 5L, "Réunion parents/enseignants",
                "MEETING", LocalDate.of(2026, 11, 5), "Salle polyvalente, 15h");

        listener().onAcademicEventCreated(event);

        ArgumentCaptor<BroadcastCommand> captor = ArgumentCaptor.forClass(BroadcastCommand.class);
        verify(broadcastUseCase).broadcast(captor.capture());
        BroadcastCommand cmd = captor.getValue();
        assertThat(cmd.tenantId()).isEqualTo(9L);
        assertThat(cmd.audience()).isEqualTo(BroadcastAudience.ALL_PARENTS);
        assertThat(cmd.channel()).isEqualTo(CommunicationChannel.SMS);
        assertThat(cmd.reason()).isEqualTo(CommunicationReason.MEETING);
        assertThat(cmd.subject()).isEqualTo("Réunion parents/enseignants");
        assertThat(cmd.content()).contains("Réunion parents/enseignants").contains("05/11/2026").contains("Salle polyvalente");
    }

    @Test
    void onAcademicEventCreated_unknownType_mapsToGeneral() {
        AcademicEventCreatedEvent event = new AcademicEventCreatedEvent(9L, 5L, "Sortie pédagogique",
                "OTHER", LocalDate.of(2026, 5, 27), null);

        listener().onAcademicEventCreated(event);

        ArgumentCaptor<BroadcastCommand> captor = ArgumentCaptor.forClass(BroadcastCommand.class);
        verify(broadcastUseCase).broadcast(captor.capture());
        assertThat(captor.getValue().reason()).isEqualTo(CommunicationReason.GENERAL);
        // Pas de description : pas de tiret final orphelin dans le message.
        assertThat(captor.getValue().content()).isEqualTo("Nouvel événement : Sortie pédagogique le 27/05/2026");
    }
}
