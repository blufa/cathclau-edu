package gn.lacaeducation.communication.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.communication.application.port.in.BroadcastUseCase;
import gn.lacaeducation.communication.application.port.in.ScheduledBroadcastUseCase.ScheduleCommand;
import gn.lacaeducation.communication.application.port.out.ScheduledBroadcastRepository;
import gn.lacaeducation.communication.domain.BroadcastAudience;
import gn.lacaeducation.communication.domain.Communication;
import gn.lacaeducation.communication.domain.CommunicationChannel;
import gn.lacaeducation.communication.domain.ScheduledBroadcast;
import gn.lacaeducation.communication.domain.ScheduledBroadcastStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduledBroadcastServiceTest {

    @Mock ScheduledBroadcastRepository repository;
    @Mock BroadcastUseCase broadcastUseCase;
    @InjectMocks ScheduledBroadcastService service;

    @Test
    void schedule_pastSendAt_throws() {
        ScheduleCommand cmd = new ScheduleCommand(1L, 2L, CommunicationChannel.SMS, BroadcastAudience.ALL,
                null, null, "Sujet", "Contenu", null, LocalDateTime.now().minusMinutes(1), null);

        assertThatThrownBy(() -> service.schedule(cmd)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void schedule_nullSendAt_throws() {
        ScheduleCommand cmd = new ScheduleCommand(1L, 2L, CommunicationChannel.SMS, BroadcastAudience.ALL,
                null, null, "Sujet", "Contenu", null, null, null);

        assertThatThrownBy(() -> service.schedule(cmd)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void schedule_classAudienceWithoutClassId_throws() {
        ScheduleCommand cmd = new ScheduleCommand(1L, 2L, CommunicationChannel.SMS, BroadcastAudience.CLASS_PARENTS,
                null, null, "Sujet", "Contenu", null, LocalDateTime.now().plusHours(1), null);

        assertThatThrownBy(() -> service.schedule(cmd)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void schedule_valid_savesAsPending() {
        LocalDateTime sendAt = LocalDateTime.now().plusDays(1);
        ScheduleCommand cmd = new ScheduleCommand(1L, 2L, CommunicationChannel.SMS, BroadcastAudience.ALL,
                null, null, "Kermesse", "La kermesse commence à 15h", null, sendAt, 42L);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        ScheduledBroadcast result = service.schedule(cmd);

        assertThat(result.getStatus()).isEqualTo(ScheduledBroadcastStatus.PENDING);
        assertThat(result.getSendAt()).isEqualTo(sendAt);
        assertThat(result.getSchoolEventId()).isEqualTo(42L);
    }

    @Test
    void cancel_pendingBroadcast_setsCancelled() {
        ScheduledBroadcast pending = ScheduledBroadcast.builder().id(1L).status(ScheduledBroadcastStatus.PENDING).build();
        when(repository.findById(1L)).thenReturn(Optional.of(pending));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.cancel(1L);

        ArgumentCaptor<ScheduledBroadcast> captor = ArgumentCaptor.forClass(ScheduledBroadcast.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ScheduledBroadcastStatus.CANCELLED);
    }

    @Test
    void cancel_alreadySent_throws() {
        ScheduledBroadcast sent = ScheduledBroadcast.builder().id(1L).status(ScheduledBroadcastStatus.SENT).build();
        when(repository.findById(1L)).thenReturn(Optional.of(sent));

        assertThatThrownBy(() -> service.cancel(1L)).isInstanceOf(BusinessRuleException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void processDueForTenant_success_marksSent() {
        LocalDateTime now = LocalDateTime.now();
        ScheduledBroadcast due = ScheduledBroadcast.builder().id(5L).tenantId(1L).channel(CommunicationChannel.SMS)
                .audience(BroadcastAudience.ALL).content("Contenu").status(ScheduledBroadcastStatus.PENDING).build();
        when(repository.findDuePending(1L, now)).thenReturn(List.of(due));
        when(broadcastUseCase.broadcast(any())).thenReturn(Communication.builder().id(9L).build());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.processDueForTenant(1L, now);

        ArgumentCaptor<ScheduledBroadcast> captor = ArgumentCaptor.forClass(ScheduledBroadcast.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ScheduledBroadcastStatus.SENT);
        assertThat(captor.getValue().getSentAt()).isEqualTo(now);
    }

    @Test
    void processDueForTenant_noRecipients_marksFailed() {
        LocalDateTime now = LocalDateTime.now();
        ScheduledBroadcast due = ScheduledBroadcast.builder().id(5L).tenantId(1L).channel(CommunicationChannel.SMS)
                .audience(BroadcastAudience.ALL).content("Contenu").status(ScheduledBroadcastStatus.PENDING).build();
        when(repository.findDuePending(1L, now)).thenReturn(List.of(due));
        when(broadcastUseCase.broadcast(any())).thenReturn(null);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.processDueForTenant(1L, now);

        ArgumentCaptor<ScheduledBroadcast> captor = ArgumentCaptor.forClass(ScheduledBroadcast.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ScheduledBroadcastStatus.FAILED);
    }

    @Test
    void processDueForTenant_exception_marksFailedAndContinuesOthers() {
        LocalDateTime now = LocalDateTime.now();
        ScheduledBroadcast a = ScheduledBroadcast.builder().id(5L).tenantId(1L).channel(CommunicationChannel.SMS)
                .audience(BroadcastAudience.ALL).content("A").status(ScheduledBroadcastStatus.PENDING).build();
        ScheduledBroadcast b = ScheduledBroadcast.builder().id(6L).tenantId(1L).channel(CommunicationChannel.SMS)
                .audience(BroadcastAudience.ALL).content("B").status(ScheduledBroadcastStatus.PENDING).build();
        when(repository.findDuePending(1L, now)).thenReturn(List.of(a, b));
        when(broadcastUseCase.broadcast(any()))
                .thenThrow(new RuntimeException("panne réseau"))
                .thenReturn(Communication.builder().id(9L).build());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.processDueForTenant(1L, now);

        ArgumentCaptor<ScheduledBroadcast> captor = ArgumentCaptor.forClass(ScheduledBroadcast.class);
        verify(repository, org.mockito.Mockito.times(2)).save(captor.capture());
        List<ScheduledBroadcast> saved = captor.getAllValues();
        assertThat(saved.get(0).getStatus()).isEqualTo(ScheduledBroadcastStatus.FAILED);
        assertThat(saved.get(0).getErrorMessage()).isEqualTo("panne réseau");
        assertThat(saved.get(1).getStatus()).isEqualTo(ScheduledBroadcastStatus.SENT);
    }
}
