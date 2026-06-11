package gn.lacaeducation.webhooks.adapters.out.persistence;
import gn.lacaeducation.webhooks.domain.WebhookDeliveryStatus;
import gn.lacaeducation.webhooks.domain.WebhookEventType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Entity @Table(name = "webhook_events") @Getter @Setter
public class WebhookEventJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "endpoint_id", nullable = false) private Long endpointId;
    @Enumerated(EnumType.STRING) @Column(name = "event_type", nullable = false) private WebhookEventType eventType;
    @Lob @Column private String payload;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private WebhookDeliveryStatus status;
    @Column(name = "attempt_count", nullable = false) private Integer attemptCount;
    @Column(name = "response_code") private Integer responseCode;
    @Column(name = "next_attempt_at") private LocalDateTime nextAttemptAt;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
}
