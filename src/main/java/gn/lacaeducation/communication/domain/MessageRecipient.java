package gn.lacaeducation.communication.domain;
import lombok.*;
import java.time.LocalDateTime;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MessageRecipient {
    private Long id;
    private Long communicationId;
    private Long userId;
    private DeliveryStatus deliveryStatus;
    private Boolean read;
    private LocalDateTime deliveredAt;
}
