package gn.lacaeducation.webhooks.domain;
import lombok.*;
import java.time.LocalDateTime;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentWebhook {
    private Long id;
    private Long transactionId;
    private WebhookProvider provider;
    private String event;
    private String rawPayload;
    private String signature;
    private Boolean verified;
    private Boolean processed;
    private String externalEventId;
    private LocalDateTime receivedAt;
}
