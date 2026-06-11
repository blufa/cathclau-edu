package gn.lacaeducation.communication.domain;
import lombok.*;
import java.time.LocalDateTime;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Communication {
    private Long id;
    private Long tenantId;
    private Long senderId;
    private CommunicationChannel channel;
    private RecipientType recipientType;
    private String subject;
    private String content;
    private CommunicationReason reason;
    private LocalDateTime sentAt;
}
