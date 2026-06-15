package gn.lacaeducation.webhooks.application.service;

import gn.lacaeducation.webhooks.application.port.out.WebhookEventRepository;
import gn.lacaeducation.webhooks.domain.WebhookDeliveryStatus;
import gn.lacaeducation.webhooks.domain.WebhookEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookEventServiceTest {

    @Mock WebhookEventRepository repo;
    @InjectMocks WebhookEventService service;

    @Test
    void replay_resetsToPendingImmediatelyAndClearsAttempts() {
        WebhookEvent failed = WebhookEvent.builder().id(1L).status(WebhookDeliveryStatus.FAILED)
                .attemptCount(5).responseCode(500).nextAttemptAt(LocalDateTime.now().plusDays(1)).build();
        when(repo.findById(1L)).thenReturn(Optional.of(failed));
        when(repo.save(any(WebhookEvent.class))).thenAnswer(i -> i.getArgument(0));

        WebhookEvent replayed = service.replay(1L);

        assertThat(replayed.getStatus()).isEqualTo(WebhookDeliveryStatus.PENDING);
        assertThat(replayed.getAttemptCount()).isZero();
        assertThat(replayed.getResponseCode()).isNull();
        assertThat(replayed.getNextAttemptAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void scheduleRetry_incrementsAttemptsAndBacksOffExponentially() {
        WebhookEvent event = WebhookEvent.builder().id(1L).status(WebhookDeliveryStatus.PENDING)
                .attemptCount(1).build();
        when(repo.findById(1L)).thenReturn(Optional.of(event));
        when(repo.save(any(WebhookEvent.class))).thenAnswer(i -> i.getArgument(0));

        WebhookEvent retried = service.scheduleRetry(1L);

        assertThat(retried.getAttemptCount()).isEqualTo(2);
        assertThat(retried.getStatus()).isEqualTo(WebhookDeliveryStatus.PENDING);
        // 2^2 * 30 = 120s in the future
        assertThat(retried.getNextAttemptAt()).isAfter(LocalDateTime.now().plusSeconds(60));
    }
}
