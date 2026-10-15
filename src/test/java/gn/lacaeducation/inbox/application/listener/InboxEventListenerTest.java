package gn.lacaeducation.inbox.application.listener;

import gn.lacaeducation.communication.application.event.UpcomingPaymentDeadlinesDigestEvent;
import gn.lacaeducation.inbox.application.port.in.NotificationUseCase;
import gn.lacaeducation.inbox.application.port.in.NotificationUseCase.CreateNotificationCommand;
import gn.lacaeducation.inbox.domain.NotificationType;
import gn.lacaeducation.students.ParentAccessProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InboxEventListenerTest {

    @Mock NotificationUseCase notificationUseCase;
    @Mock ParentAccessProvider parentAccessProvider;

    InboxEventListener listener() {
        return new InboxEventListener(notificationUseCase, parentAccessProvider);
    }

    /** §retour comptable : une seule notification groupée par destinataire, pas une par élève — le
     * contenu doit porter le nombre total d'élèves concernés. */
    @Test
    void onUpcomingPaymentDeadlinesDigest_notifiesEachResolvedUserOnce() {
        when(notificationUseCase.create(org.mockito.ArgumentMatchers.any())).thenReturn(null);

        listener().onUpcomingPaymentDeadlinesDigest(new UpcomingPaymentDeadlinesDigestEvent(10L, List.of(100L, 101L), 3));

        ArgumentCaptor<CreateNotificationCommand> captor = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(notificationUseCase, org.mockito.Mockito.times(2)).create(captor.capture());
        List<CreateNotificationCommand> commands = captor.getAllValues();
        assertThat(commands).extracting(CreateNotificationCommand::userId).containsExactlyInAnyOrder(100L, 101L);
        assertThat(commands).allMatch(c -> c.tenantId().equals(10L));
        assertThat(commands).allMatch(c -> c.type() == NotificationType.PAYMENT);
        assertThat(commands).allMatch(c -> c.content().contains("3 élèves ont"));
        assertThat(commands).allMatch(c -> "/communications/payment-reminders".equals(c.link()));
    }

    @Test
    void onUpcomingPaymentDeadlinesDigest_singularWording_forOneStudent() {
        when(notificationUseCase.create(org.mockito.ArgumentMatchers.any())).thenReturn(null);

        listener().onUpcomingPaymentDeadlinesDigest(new UpcomingPaymentDeadlinesDigestEvent(10L, List.of(100L), 1));

        ArgumentCaptor<CreateNotificationCommand> captor = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(notificationUseCase).create(captor.capture());
        assertThat(captor.getValue().content()).contains("1 élève a");
    }
}
