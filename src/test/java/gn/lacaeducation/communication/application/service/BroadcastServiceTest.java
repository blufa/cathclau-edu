package gn.lacaeducation.communication.application.service;

import gn.lacaeducation.communication.application.port.in.BroadcastUseCase.BroadcastCommand;
import gn.lacaeducation.communication.application.port.in.CommunicationUseCase;
import gn.lacaeducation.communication.application.port.in.CommunicationUseCase.RecipientTarget;
import gn.lacaeducation.communication.application.port.in.CommunicationUseCase.SendCommunicationCommand;
import gn.lacaeducation.communication.domain.BroadcastAudience;
import gn.lacaeducation.communication.domain.Communication;
import gn.lacaeducation.communication.domain.CommunicationChannel;
import gn.lacaeducation.communication.domain.RecipientType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BroadcastServiceTest {

    @Mock RecipientResolverService recipientResolverService;
    @Mock CommunicationUseCase communicationUseCase;
    @InjectMocks BroadcastService service;

    @Test
    void noRecipients_returnsNullWithoutSending() {
        when(recipientResolverService.resolve(1L, BroadcastAudience.ALL_PARENTS, null, null, CommunicationChannel.SMS))
                .thenReturn(List.of());

        Communication result = service.broadcast(new BroadcastCommand(
                1L, 2L, CommunicationChannel.SMS, BroadcastAudience.ALL_PARENTS, null, null,
                "Sujet", "Contenu", null));

        assertThat(result).isNull();
        verifyNoInteractions(communicationUseCase);
    }

    @Test
    void allTeachers_usesTeacherRecipientType() {
        RecipientTarget target = new RecipientTarget(null, "623000099");
        when(recipientResolverService.resolve(1L, BroadcastAudience.ALL_TEACHERS, null, null, CommunicationChannel.SMS))
                .thenReturn(List.of(target));
        when(communicationUseCase.send(any())).thenReturn(Communication.builder().id(99L).build());

        Communication result = service.broadcast(new BroadcastCommand(
                1L, 2L, CommunicationChannel.SMS, BroadcastAudience.ALL_TEACHERS, null, null,
                "Réunion", "Réunion pédagogique demain", null));

        assertThat(result.getId()).isEqualTo(99L);
        ArgumentCaptor<SendCommunicationCommand> captor = ArgumentCaptor.forClass(SendCommunicationCommand.class);
        verify(communicationUseCase).send(captor.capture());
        assertThat(captor.getValue().recipientType()).isEqualTo(RecipientType.TEACHER);
        assertThat(captor.getValue().recipients()).containsExactly(target);
    }

    @Test
    void allStaff_usesGroupRecipientType() {
        RecipientTarget target = new RecipientTarget(null, "623000099");
        when(recipientResolverService.resolve(1L, BroadcastAudience.ALL_STAFF, null, null, CommunicationChannel.SMS))
                .thenReturn(List.of(target));
        when(communicationUseCase.send(any())).thenReturn(Communication.builder().id(99L).build());

        service.broadcast(new BroadcastCommand(
                1L, 2L, CommunicationChannel.SMS, BroadcastAudience.ALL_STAFF, null, null,
                "Paie", "Les salaires du mois sont disponibles", null));

        ArgumentCaptor<SendCommunicationCommand> captor = ArgumentCaptor.forClass(SendCommunicationCommand.class);
        verify(communicationUseCase).send(captor.capture());
        assertThat(captor.getValue().recipientType()).isEqualTo(RecipientType.GROUP);
    }

    @Test
    void all_usesGroupRecipientType() {
        RecipientTarget target = new RecipientTarget(null, "623000099");
        when(recipientResolverService.resolve(1L, BroadcastAudience.ALL, null, null, CommunicationChannel.SMS))
                .thenReturn(List.of(target));
        when(communicationUseCase.send(any())).thenReturn(Communication.builder().id(99L).build());

        service.broadcast(new BroadcastCommand(
                1L, 2L, CommunicationChannel.SMS, BroadcastAudience.ALL, null, null,
                "Kermesse", "La kermesse commence dans 1h, merci de venir récupérer vos enfants à 15h.", null));

        ArgumentCaptor<SendCommunicationCommand> captor = ArgumentCaptor.forClass(SendCommunicationCommand.class);
        verify(communicationUseCase).send(captor.capture());
        assertThat(captor.getValue().recipientType()).isEqualTo(RecipientType.GROUP);
    }

    @Test
    void classAudienceWithoutClassId_throws() {
        assertThatThrownBy(() -> service.broadcast(new BroadcastCommand(
                1L, 2L, CommunicationChannel.SMS, BroadcastAudience.CLASS_PARENTS, null, null,
                "Sujet", "Contenu", null)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(communicationUseCase);
    }

    @Test
    void studentParentsWithoutStudentId_throws() {
        assertThatThrownBy(() -> service.broadcast(new BroadcastCommand(
                1L, 2L, CommunicationChannel.SMS, BroadcastAudience.STUDENT_PARENTS, null, null,
                "Sujet", "Contenu", null)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(communicationUseCase);
    }

    @Test
    void allParents_usesParentRecipientType() {
        RecipientTarget target = new RecipientTarget(null, "623000001");
        when(recipientResolverService.resolve(1L, BroadcastAudience.ALL_PARENTS, null, null, CommunicationChannel.SMS))
                .thenReturn(List.of(target));
        when(communicationUseCase.send(any())).thenReturn(Communication.builder().id(1L).build());

        service.broadcast(new BroadcastCommand(
                1L, 2L, CommunicationChannel.SMS, BroadcastAudience.ALL_PARENTS, null, null,
                "Sujet", "Contenu", null));

        ArgumentCaptor<SendCommunicationCommand> captor = ArgumentCaptor.forClass(SendCommunicationCommand.class);
        verify(communicationUseCase).send(captor.capture());
        assertThat(captor.getValue().recipientType()).isEqualTo(RecipientType.PARENT);
    }
}
