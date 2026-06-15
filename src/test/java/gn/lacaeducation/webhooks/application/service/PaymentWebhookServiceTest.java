package gn.lacaeducation.webhooks.application.service;

import gn.lacaeducation.payments.PaymentCallbackHandler;
import gn.lacaeducation.webhooks.application.port.in.PaymentWebhookUseCase.ReceiveWebhookCommand;
import gn.lacaeducation.webhooks.application.port.out.PaymentWebhookRepository;
import gn.lacaeducation.webhooks.domain.PaymentWebhook;
import gn.lacaeducation.webhooks.domain.WebhookProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookServiceTest {

    private static final String SECRET = "test-secret";

    @Mock PaymentWebhookRepository repo;
    @Mock PaymentCallbackHandler callbackHandler;
    PaymentWebhookService service;

    @BeforeEach
    void setUp() {
        service = new PaymentWebhookService(repo, callbackHandler);
        ReflectionTestUtils.setField(service, "webhookSecret", SECRET);
        lenient().when(repo.save(any(PaymentWebhook.class))).thenAnswer(i -> {
            PaymentWebhook w = i.getArgument(0);
            if (w.getId() == null) w.setId(1L);
            return w;
        });
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    void verifiedSuccessCallback_drivesPaymentAndMarksProcessed() {
        String payload = "{\"event\":\"payment.success\"}";
        PaymentWebhook wh = service.receive(new ReceiveWebhookCommand(
                55L, WebhookProvider.ORANGE_MONEY, "payment.success", payload, sign(payload), "evt-1"));

        assertThat(wh.getVerified()).isTrue();
        assertThat(wh.getProcessed()).isTrue();
        verify(callbackHandler).applyCallback(eq(55L), eq(true), eq("payment.success"), eq(payload));
    }

    @Test
    void verifiedFailureCallback_drivesPaymentAsFailure() {
        String payload = "{\"event\":\"payment.failed\"}";
        service.receive(new ReceiveWebhookCommand(
                55L, WebhookProvider.WAVE, "payment.failed", payload, sign(payload), "evt-2"));

        verify(callbackHandler).applyCallback(eq(55L), eq(false), any(), any());
    }

    @Test
    void invalidSignature_isNotProcessed() {
        PaymentWebhook wh = service.receive(new ReceiveWebhookCommand(
                55L, WebhookProvider.MTN_MONEY, "payment.success", "{}", "bad-signature", "evt-3"));

        assertThat(wh.getVerified()).isFalse();
        assertThat(wh.getProcessed()).isFalse();
        verify(callbackHandler, never()).applyCallback(any(), anyBoolean(), any(), any());
    }

    @Test
    void idempotence_keysOnProcessedEvents() {
        when(repo.existsByExternalEventIdAndProcessedTrue("evt-done")).thenReturn(true);
        assertThat(service.isAlreadyProcessed("evt-done")).isTrue();
        assertThat(service.isAlreadyProcessed(null)).isFalse();
    }
}
