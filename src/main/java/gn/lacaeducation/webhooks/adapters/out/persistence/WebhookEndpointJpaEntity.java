package gn.lacaeducation.webhooks.adapters.out.persistence;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Entity @Table(name = "webhook_endpoints") @Getter @Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class WebhookEndpointJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(nullable = false) private String url;
    @Column(nullable = false) private String secret;
    @Column private String events;
    @Column(nullable = false) private Boolean active;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
}
