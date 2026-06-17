package gn.lacaeducation.webhooks.adapters.out.persistence;
import gn.lacaeducation.webhooks.domain.WebhookProvider;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Entity @Table(name = "payment_webhooks") @Getter @Setter
public class PaymentWebhookJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "transaction_id") private Long transactionId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private WebhookProvider provider;
    @Column private String event;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR) @Column(name = "raw_payload") private String rawPayload;
    @Column private String signature;
    @Column(nullable = false) private Boolean verified;
    @Column(nullable = false) private Boolean processed;
    @Column(name = "external_event_id") private String externalEventId;
    @Column(name = "received_at", nullable = false) private LocalDateTime receivedAt;
}
