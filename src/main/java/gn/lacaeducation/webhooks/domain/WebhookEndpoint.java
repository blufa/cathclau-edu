package gn.lacaeducation.webhooks.domain;
import lombok.*;
import java.time.LocalDateTime;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class WebhookEndpoint {
    private Long id;
    private Long tenantId;
    private String url;
    private String secret;
    private String events;
    private Boolean active;
    private LocalDateTime createdAt;
}
