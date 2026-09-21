package gn.lacaeducation.communication.application.service;

import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.communication.application.port.in.SmsRetryQueueUseCase;
import gn.lacaeducation.communication.application.port.out.SmsRetryQueueRepository;
import gn.lacaeducation.communication.domain.SmsRetryEntry;
import gn.lacaeducation.communication.domain.SmsRetryStatus;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmsRetryQueueServiceTest {

    @Mock SmsRetryQueueRepository repo;
    @InjectMocks SmsRetryQueueService service;

    @Test
    void enqueue_savesPendingEntryWithZeroAttempts() {
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SmsRetryEntry saved = service.enqueue(new SmsRetryQueueUseCase.EnqueueCommand(
                1L, 20L, "623273737", "sujet", "contenu", "erreur fournisseur"));

        ArgumentCaptor<SmsRetryEntry> captor = ArgumentCaptor.forClass(SmsRetryEntry.class);
        verify(repo).save(captor.capture());
        SmsRetryEntry entry = captor.getValue();
        assertThat(entry.getTenantId()).isEqualTo(1L);
        assertThat(entry.getRecipientId()).isEqualTo(20L);
        assertThat(entry.getDestination()).isEqualTo("623273737");
        assertThat(entry.getStatus()).isEqualTo(SmsRetryStatus.PENDING);
        assertThat(entry.getAttemptCount()).isZero();
        assertThat(entry.getNextAttemptAt()).isNotNull();
        assertThat(entry.getLastError()).isEqualTo("erreur fournisseur");
        assertThat(saved).isSameAs(entry);
    }

    @Test
    void findByTenant_delegatesToRepository() {
        SmsRetryEntry entry = SmsRetryEntry.builder().id(1L).tenantId(1L).build();
        when(repo.findByTenantId(1L)).thenReturn(List.of(entry));

        assertThat(service.findByTenant(1L)).containsExactly(entry);
    }

    @Test
    void replay_resetsAttemptCountAndStatusAndNextAttemptAt() {
        SmsRetryEntry entry = SmsRetryEntry.builder().id(5L).tenantId(1L)
                .status(SmsRetryStatus.FAILED).attemptCount(5)
                .nextAttemptAt(LocalDateTime.now().minusDays(1)).build();
        when(repo.findById(5L)).thenReturn(Optional.of(entry));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SmsRetryEntry replayed = service.replay(5L);

        assertThat(replayed.getStatus()).isEqualTo(SmsRetryStatus.PENDING);
        assertThat(replayed.getAttemptCount()).isZero();
        assertThat(replayed.getNextAttemptAt()).isAfter(LocalDateTime.now().minusMinutes(1));
    }

    @Test
    void replay_unknownId_throwsNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.replay(99L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
