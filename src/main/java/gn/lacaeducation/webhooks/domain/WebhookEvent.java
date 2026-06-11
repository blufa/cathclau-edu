package gn.lacaeducation.webhooks.domain;
import lombok.*;
import java.time.LocalDateTime;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class WebhookEvent {
    private Long id;
    private Long endpointId;
    private WebhookEventType eventType;
    private String payload;
    private WebhookDeliveryStatus status;
    private Integer attemptCount;
    private Integer responseCode;
    private LocalDateTime nextAttemptAt;
    private LocalDateTime createdAt;
}
