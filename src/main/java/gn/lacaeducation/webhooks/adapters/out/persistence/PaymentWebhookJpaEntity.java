package gn.lacaeducation.webhooks.adapters.out.persistence;
import gn.lacaeducation.webhooks.domain.WebhookProvider;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Entity @Table(name = "payment_webhooks") @Getter @Setter
public class PaymentWebhookJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "transaction_id") private Long transactionId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private WebhookProvider provider;
    @Column private String event;
    @Lob @Column(name = "raw_payload") private String rawPayload;
    @Column private String signature;
    @Column(nullable = false) private Boolean verified;
    @Column(nullable = false) private Boolean processed;
    @Column(name = "received_at", nullable = false) private LocalDateTime receivedAt;
}
