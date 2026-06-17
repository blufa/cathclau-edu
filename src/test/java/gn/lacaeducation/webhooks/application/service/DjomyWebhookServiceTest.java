package gn.lacaeducation.webhooks.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import gn.lacaeducation.payments.DjomyCallbackHandler;
import gn.lacaeducation.webhooks.application.port.out.PaymentWebhookRepository;
import gn.lacaeducation.webhooks.domain.PaymentWebhook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DjomyWebhookServiceTest {

    @Mock DjomyCallbackHandler djomy;
    @Mock PaymentWebhookRepository repo;
    DjomyWebhookService service;

    private static final String SUCCESS_BODY =
            "{\"id\":\"evt1\",\"event\":\"payment.success\",\"data\":{\"transactionId\":\"TX-1\"}}";
    private static final String FAILED_BODY =
            "{\"id\":\"evt2\",\"event\":\"payment.failed\",\"data\":{\"transactionId\":\"TX-1\"}}";

    @BeforeEach
    void setUp() {
        service = new DjomyWebhookService(djomy, repo, null);
    }

    @Test
    void rejects_invalid_signature_with_400_and_no_processing() throws Exception {
        when(djomy.verifyWebhookSignature(any(), any())).thenReturn(false);

        assertThat(service.handle("{}", "bad")).isFalse();
        verify(repo, never()).save(any());
        verify(djomy, never()).confirmSuccess(any());
        verify(djomy, never()).markFailed(any());
    }

    @Test
    void payment_success_confirmed_marks_processed() throws Exception {
        when(djomy.verifyWebhookSignature(any(), any())).thenReturn(true);
        when(repo.existsByExternalEventIdAndProcessedTrue("evt1")).thenReturn(false);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(djomy.resolveTransactionId("TX-1")).thenReturn(Optional.of(5L));
        when(djomy.confirmSuccess("TX-1")).thenReturn(true);

        assertThat(service.handle(SUCCESS_BODY, "v1:abc")).isTrue();
        verify(djomy).confirmSuccess("TX-1");
        // saved once on receipt, once more after marking processed
        verify(repo, times(2)).save(any(PaymentWebhook.class));
    }

    @Test
    void payment_success_not_confirmed_does_not_mark_processed() throws Exception {
        when(djomy.verifyWebhookSignature(any(), any())).thenReturn(true);
        when(repo.existsByExternalEventIdAndProcessedTrue("evt1")).thenReturn(false);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(djomy.resolveTransactionId("TX-1")).thenReturn(Optional.of(5L));
        when(djomy.confirmSuccess("TX-1")).thenReturn(false); // verify_payment contradicts the webhook

        assertThat(service.handle(SUCCESS_BODY, "v1:abc")).isTrue();
        verify(djomy).confirmSuccess("TX-1");
        verify(repo, times(1)).save(any(PaymentWebhook.class));
    }

    @Test
    void payment_failed_marks_failed() throws Exception {
        when(djomy.verifyWebhookSignature(any(), any())).thenReturn(true);
        when(repo.existsByExternalEventIdAndProcessedTrue("evt2")).thenReturn(false);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(djomy.resolveTransactionId("TX-1")).thenReturn(Optional.of(5L));

        assertThat(service.handle(FAILED_BODY, "v1:abc")).isTrue();
        verify(djomy).markFailed("TX-1");
    }

    @Test
    void idempotent_already_processed_event_is_acknowledged_without_acting() throws Exception {
        when(djomy.verifyWebhookSignature(any(), any())).thenReturn(true);
        when(repo.existsByExternalEventIdAndProcessedTrue("evt1")).thenReturn(true);

        assertThat(service.handle(SUCCESS_BODY, "v1:abc")).isTrue();
        verify(repo, never()).save(any());
        verify(djomy, never()).confirmSuccess(any());
    }
}
