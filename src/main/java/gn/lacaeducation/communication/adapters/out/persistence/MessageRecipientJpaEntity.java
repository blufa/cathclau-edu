package gn.lacaeducation.communication.adapters.out.persistence;
import gn.lacaeducation.communication.domain.DeliveryStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Entity @Table(name = "message_recipients") @Getter @Setter
public class MessageRecipientJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "communication_id", nullable = false) private Long communicationId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Enumerated(EnumType.STRING) @Column(name = "delivery_status", nullable = false) private DeliveryStatus deliveryStatus;
    @Column(nullable = false) private Boolean read;
    @Column(name = "delivered_at") private LocalDateTime deliveredAt;
}
